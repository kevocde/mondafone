package dev.polyrics.mondafone.network

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
import dev.polyrics.mondafone.spam.SequenceInventoryEntry
import dev.polyrics.mondafone.spam.SpamReport
import java.util.UUID

class GattSyncServer(
    private val context: Context,
    private val inventoryProvider: () -> List<SequenceInventoryEntry>,
    private val reportsProvider: (createdBy: String, fromSeq: Long, toSeq: Long) -> List<SpamReport>,
    private val reportCountProvider: () -> Int,
    private val ownPubkeyProvider: () -> String?,
    private val ingesterFactory: () -> ReportIngester,
    private val eventLog: BeaconEventLog,
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
            val chunk = payload.copyOfRange(offset.coerceAtMost(payload.size), payload.size)
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
                    onWait = { sendControl(device, DataPipeMessage.Wait) },
                    onReady = { sendControl(device, DataPipeMessage.Ready) },
                )
            }
        }
    }

    private fun ensureSession(device: BluetoothDevice) {
        val address = device.address
        if (address in phases) return
        eventLog.log("connected $address")
        ingesters[address] = ingesterFactory()
        phases[address] = Phase.AWAIT_PULL_REQUESTS
    }

    private fun handlePipeFrame(device: BluetoothDevice, message: DataPipeMessage?) {
        when (message) {
            is DataPipeMessage.Requests -> requestsFor(device).addAll(message.requests)
            is DataPipeMessage.OfferState -> offersFor(device).addAll(message.entries)
            is DataPipeMessage.Reports -> ingesterFor(device).ingest(message.reports)
            DataPipeMessage.End -> advancePhase(device)
            DataPipeMessage.Wait, DataPipeMessage.Ready, null -> Unit
        }
    }

    private fun advancePhase(device: BluetoothDevice) {
        val address = device.address
        when (phases[address] ?: Phase.AWAIT_PULL_REQUESTS) {
            Phase.AWAIT_PULL_REQUESTS -> {
                serveRequests(device, pendingRequests.remove(address).orEmpty())
                phases[address] = Phase.AWAIT_OFFER
            }
            Phase.AWAIT_OFFER -> {
                replyWithRequests(device, pendingOffers.remove(address).orEmpty())
                phases[address] = Phase.AWAIT_PUSH_REPORTS
            }
            Phase.AWAIT_PUSH_REPORTS -> sendControl(device, DataPipeMessage.End)
        }
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
        if (reports.isNotEmpty()) {
            eventLog.log("sync: sent ${reports.size}, total ${reportCountProvider()}")
        }
    }

    private fun replyWithRequests(device: BluetoothDevice, offered: List<SequenceInventoryEntry>) {
        val data = dataCharacteristic ?: return
        val requests = planner.plan(offered, inventoryProvider(), ownPubkeyProvider())
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
                onFailure = { eventLog.log("notify failed") },
            )
        }
    }

    private fun onDeviceDisconnected(device: BluetoothDevice) {
        val address = device.address
        val id = turnId(address)
        outbox.cancel(id)
        turnCoordinator.releaseTurn(id)
        mtuByDevice.remove(address)
        phases.remove(address)
        pendingRequests.remove(address)
        pendingOffers.remove(address)
        ingesters.remove(address)?.consumeSummaryLine(reportCountProvider())?.let(eventLog::log)
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
    }
}
