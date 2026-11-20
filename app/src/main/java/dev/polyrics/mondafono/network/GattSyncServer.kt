package dev.polyrics.mondafono.network

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import dev.polyrics.mondafono.spam.SequenceInventoryEntry
import dev.polyrics.mondafono.spam.SpamReport
import java.util.UUID

class GattSyncServer(
    private val context: Context,
    private val inventoryProvider: () -> List<SequenceInventoryEntry>,
    private val reportsProvider: (createdBy: String, fromSeq: Long, toSeq: Long) -> List<SpamReport>,
    private val reportCountProvider: () -> Int,
    private val ownPubkeyProvider: () -> String?,
    private val ingesterFactory: () -> ReportIngester,
    private val outbox: GattOutbox = GattOutbox(),
    private val turnCoordinator: SyncTurnCoordinator = SyncTurnCoordinator(),
    private val inventoryCodec: SequenceInventoryCodec = SequenceInventoryCodec(),
    private val pipeCodec: DataPipeCodec = DataPipeCodec(),
    private val planner: SyncRequestPlanner = SyncRequestPlanner(),
) {

    private var server: BluetoothGattServer? = null
    private var stateCharacteristic: BluetoothGattCharacteristic? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private val mtuByDevice = mutableMapOf<String, Int>()
    private val ingesters = mutableMapOf<String, ReportIngester>()
    private val phases = mutableMapOf<String, Phase>()
    private val pendingRequests = mutableMapOf<String, MutableList<ReportRequest>>()
    private val pendingOffers = mutableMapOf<String, MutableList<SequenceInventoryEntry>>()
    private val handler = Handler(Looper.getMainLooper())
    private val waitTickers = mutableMapOf<String, Runnable>()
    private val releasedSessions = mutableSetOf<String>()

    fun open(manager: BluetoothManager): Boolean {
        val newServer = manager.openGattServer(context, callback) ?: return false
        val service = buildService()
        if (!newServer.addService(service)) {
            runCatching { newServer.close() }
            return false
        }
        stateCharacteristic = service.getCharacteristic(GattSyncProtocol.STATE_UUID)
        dataCharacteristic = service.getCharacteristic(GattSyncProtocol.DATA_UUID)
        server = newServer
        return true
    }

    fun close() {
        val current = server ?: return
        phases.keys.forEach { address ->
            val id = turnId(address)
            outbox.cancel(id)
            turnCoordinator.releaseTurn(id)
        }
        waitTickers.keys.toList().forEach(::stopWaitTicker)
        runCatching { current.clearServices() }
        runCatching { current.close() }
        server = null
        stateCharacteristic = null
        dataCharacteristic = null
        mtuByDevice.clear()
        ingesters.clear()
        phases.clear()
        pendingRequests.clear()
        pendingOffers.clear()
        releasedSessions.clear()
    }

    private fun buildService(): BluetoothGattService {
        val state = characteristic(
            uuid = GattSyncProtocol.STATE_UUID,
            properties = BluetoothGattCharacteristic.PROPERTY_READ or
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            permissions = BluetoothGattCharacteristic.PERMISSION_READ,
        )
        val data = characteristic(
            uuid = GattSyncProtocol.DATA_UUID,
            properties = BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            permissions = BluetoothGattCharacteristic.PERMISSION_WRITE,
        )
        return BluetoothGattService(
            GattSyncProtocol.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY,
        ).apply {
            addCharacteristic(state)
            addCharacteristic(data)
        }
    }

    private fun characteristic(uuid: UUID, properties: Int, permissions: Int) =
        BluetoothGattCharacteristic(uuid, properties, permissions).apply {
            addDescriptor(
                BluetoothGattDescriptor(
                    GattSyncProtocol.CCC_DESCRIPTOR_UUID,
                    BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
                ),
            )
        }

    private val callback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                onDeviceDisconnected(device)
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            BeaconLog.log("server ${device.address} mtu $mtu")
            mtuByDevice[device.address] = mtu
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            outbox.onPacketSent()
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (characteristic.uuid != GattSyncProtocol.STATE_UUID) {
                respond(device, requestId, offset, ByteArray(0))
                return
            }
            ensureSession(device)
            val payload = inventoryCodec.encode(inventoryProvider())
            val start = offset.coerceAtMost(payload.size)
            val end = (start + maxPayloadFor(device)).coerceAtMost(payload.size)
            val chunk = payload.copyOfRange(start, end)
            BeaconLog.log("server ${device.address} state read $start-${end}/${payload.size}")
            respond(device, requestId, offset, chunk)
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (responseNeeded) respond(device, requestId, offset, null)
            if (characteristic.uuid == GattSyncProtocol.DATA_UUID) {
                ensureSession(device)
                handlePipeFrame(device, pipeCodec.decode(value))
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (responseNeeded) respond(device, requestId, offset, null)
            if (descriptor.characteristic.uuid == GattSyncProtocol.DATA_UUID) {
                ensureSession(device)
                turnCoordinator.requestTurn(
                    id = turnId(device.address),
                    onWait = {
                        BeaconLog.log("server ${device.address} wait")
                        sendControl(device, DataPipeMessage.Wait)
                        startWaitTicker(device)
                    },
                    onReady = {
                        BeaconLog.log("server ${device.address} ready")
                        stopWaitTicker(device.address)
                        sendControl(device, DataPipeMessage.Ready)
                        sendState(device)
                    },
                )
            }
        }
    }

    private fun ensureSession(device: BluetoothDevice) {
        val address = device.address
        if (address in phases) return
        BeaconLog.log("server $address connected")
        ingesters[address] = ingesterFactory()
        phases[address] = Phase.AWAIT_PULL_REQUESTS
        releasedSessions.add(turnId(address))
    }

    private fun handlePipeFrame(device: BluetoothDevice, message: DataPipeMessage?) {
        val address = device.address
        when (message) {
            is DataPipeMessage.Requests -> {
                BeaconLog.log("server $address got ${message.requests.size} requests")
                requestsFor(device).addAll(message.requests)
            }
            is DataPipeMessage.OfferState -> {
                BeaconLog.log("server $address got ${message.entries.size} offers")
                offersFor(device).addAll(message.entries)
            }
            is DataPipeMessage.Reports -> {
                BeaconLog.log("server $address got ${message.reports.size} reports")
                ingesterFor(device).ingest(message.reports)
            }
            DataPipeMessage.End -> advancePhase(device)
            DataPipeMessage.Wait, DataPipeMessage.Ready, null -> Unit
        }
    }

    private fun advancePhase(device: BluetoothDevice) {
        val address = device.address
        when (phases[address] ?: Phase.AWAIT_PULL_REQUESTS) {
            Phase.AWAIT_PULL_REQUESTS -> {
                BeaconLog.log("server $address pull done")
                serveRequests(device, pendingRequests.remove(address).orEmpty())
                phases[address] = Phase.AWAIT_OFFER
            }
            Phase.AWAIT_OFFER -> {
                BeaconLog.log("server $address offer done")
                replyWithRequests(device, pendingOffers.remove(address).orEmpty())
                phases[address] = Phase.AWAIT_PUSH_REPORTS
            }
            Phase.AWAIT_PUSH_REPORTS -> {
                BeaconLog.log("server $address push done")
                sendControl(device, DataPipeMessage.End)
            }
        }
    }

    private fun startWaitTicker(device: BluetoothDevice) {
        val address = device.address
        if (waitTickers.containsKey(address)) return
        val tick = object : Runnable {
            override fun run() {
                if (phases[address] == null) return
                sendControl(device, DataPipeMessage.Wait)
                handler.postDelayed(this, WAIT_REFRESH_MS)
            }
        }
        waitTickers[address] = tick
        handler.postDelayed(tick, WAIT_REFRESH_MS)
    }

    private fun stopWaitTicker(address: String) {
        waitTickers.remove(address)?.let(handler::removeCallbacks)
    }

    private fun sendState(device: BluetoothDevice) {
        val data = dataCharacteristic ?: return
        val inventory = inventoryProvider()
        BeaconLog.log("server ${device.address} push state: ${inventory.size} entries")
        enqueue(device, data, pipeCodec.encode(DataPipeMessage.OfferState(inventory), maxPayloadFor(device)))
        enqueue(device, data, pipeCodec.encode(DataPipeMessage.End, maxPayloadFor(device)))
    }

    private fun sendControl(device: BluetoothDevice, message: DataPipeMessage) {
        val data = dataCharacteristic ?: return
        enqueue(device, data, pipeCodec.encode(message, maxPayloadFor(device)))
    }

    private fun requestsFor(device: BluetoothDevice): MutableList<ReportRequest> =
        pendingRequests.getOrPut(device.address) { mutableListOf() }

    private fun offersFor(device: BluetoothDevice): MutableList<SequenceInventoryEntry> =
        pendingOffers.getOrPut(device.address) { mutableListOf() }

    private fun serveRequests(device: BluetoothDevice, requests: List<ReportRequest>) {
        val reports = requests.flatMap { request ->
            reportsProvider(request.createdBy, request.fromSeq, request.toSeq)
        }
        val data = dataCharacteristic ?: return
        enqueue(device, data, pipeCodec.encode(DataPipeMessage.Reports(reports), maxPayloadFor(device)))
        enqueue(device, data, pipeCodec.encode(DataPipeMessage.End, maxPayloadFor(device)))
        BeaconLog.log(
            "server ${device.address} serve: ${reports.size} reports for ${requests.size} requests, " +
                "total ${reportCountProvider()}",
        )
    }

    private fun replyWithRequests(device: BluetoothDevice, offered: List<SequenceInventoryEntry>) {
        val data = dataCharacteristic ?: return
        val requests = planner.plan(offered, inventoryProvider(), ownPubkeyProvider())
        BeaconLog.log("server ${device.address} request: ${requests.size}")
        enqueue(device, data, pipeCodec.encode(DataPipeMessage.Requests(requests), maxPayloadFor(device)))
        enqueue(device, data, pipeCodec.encode(DataPipeMessage.End, maxPayloadFor(device)))
    }

    private fun enqueue(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        frames: List<ByteArray>,
    ) {
        val tag = turnId(device.address)
        frames.forEach { payload ->
            outbox.enqueue(
                tag = tag,
                send = { notify(device, characteristic, payload) },
                onFailure = { BeaconLog.log("notify failed") },
            )
        }
    }

    private fun onDeviceDisconnected(device: BluetoothDevice) {
        val address = device.address
        val id = turnId(address)
        stopWaitTicker(address)
        outbox.cancel(id)
        turnCoordinator.releaseTurn(id)
        mtuByDevice.remove(address)
        phases.remove(address)
        pendingRequests.remove(address)
        pendingOffers.remove(address)
        ingesters.remove(address)?.consumeSummaryLine(reportCountProvider())?.let(BeaconLog::log)
        if (id in releasedSessions) {
            releasedSessions.remove(id)
            BeaconLog.log("server $address session end")
        }
    }

    private fun turnId(address: String): String = "server:$address"

    private fun maxPayloadFor(device: BluetoothDevice): Int =
        ((mtuByDevice[device.address] ?: DEFAULT_MTU) - ATT_OVERHEAD).coerceAtLeast(1)

    private fun ingesterFor(device: BluetoothDevice): ReportIngester =
        ingesters.getOrPut(device.address) { ingesterFactory() }

    @Suppress("DEPRECATION")
    private fun notify(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        payload: ByteArray,
    ): Boolean {
        val current = server ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            current.notifyCharacteristicChanged(device, characteristic, false, payload) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.value = payload
            current.notifyCharacteristicChanged(device, characteristic, false)
        }
    }

    private fun respond(device: BluetoothDevice, requestId: Int, offset: Int, value: ByteArray?) {
        runCatching { server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value) }
    }

    private enum class Phase {
        AWAIT_PULL_REQUESTS,
        AWAIT_OFFER,
        AWAIT_PUSH_REPORTS,
    }

    private companion object {
        const val DEFAULT_MTU = 23
        const val ATT_OVERHEAD = 3
        const val WAIT_REFRESH_MS = 700L
    }
}
