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
    private val inventoryCodec: SequenceInventoryCodec = SequenceInventoryCodec(),
    private val pipeCodec: DataPipeCodec = DataPipeCodec(),
    private val planner: SyncRequestPlanner = SyncRequestPlanner(),
) {

    private var server: BluetoothGattServer? = null
    private var stateCharacteristic: BluetoothGattCharacteristic? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private val mtuByDevice = mutableMapOf<String, Int>()
    private val ingesters = mutableMapOf<String, ReportIngester>()
    private val pendingFrames = mutableMapOf<String, ArrayDeque<OutgoingFrame>>()
    private val notifyInFlight = mutableSetOf<String>()

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
        runCatching { current.clearServices() }
        runCatching { current.close() }
        server = null
        stateCharacteristic = null
        dataCharacteristic = null
        mtuByDevice.clear()
        ingesters.clear()
        pendingFrames.clear()
        notifyInFlight.clear()
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
            properties = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
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
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    eventLog.log("connected ${device.address}")
                    ingesters[device.address] = ingesterFactory()
                }
                BluetoothProfile.STATE_DISCONNECTED -> onDeviceDisconnected(device)
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            mtuByDevice[device.address] = mtu
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            notifyInFlight.remove(device.address)
            pump(device)
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
            if (descriptor.characteristic.uuid != GattSyncProtocol.STATE_UUID) return
            if (isNotificationEnabled(value)) notifyInventory(device)
        }
    }

    private fun handlePipeFrame(device: BluetoothDevice, message: DataPipeMessage?) {
        when (message) {
            is DataPipeMessage.Requests -> serveRequests(device, message.requests)
            is DataPipeMessage.OfferState -> replyWithRequests(device, message.entries)
            is DataPipeMessage.Reports -> ingesterFor(device).ingest(message.reports)
            DataPipeMessage.End, null -> Unit
        }
    }

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
    }

    private fun notifyInventory(device: BluetoothDevice) {
        val state = stateCharacteristic ?: return
        enqueue(device, state, listOf(inventoryCodec.encode(inventoryProvider())))
    }

    private fun enqueue(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        frames: List<ByteArray>,
    ) {
        if (frames.isEmpty()) return
        val queue = pendingFrames.getOrPut(device.address) { ArrayDeque() }
        frames.forEach { queue.addLast(OutgoingFrame(characteristic, it)) }
        pump(device)
    }

    private fun pump(device: BluetoothDevice) {
        val address = device.address
        if (address in notifyInFlight) return
        val queue = pendingFrames[address] ?: return
        val frame = queue.removeFirstOrNull() ?: return
        if (!notify(device, frame.characteristic, frame.payload)) {
            queue.clear()
            eventLog.log("notify failed")
            return
        }
        notifyInFlight.add(address)
    }

    private fun onDeviceDisconnected(device: BluetoothDevice) {
        val address = device.address
        mtuByDevice.remove(address)
        pendingFrames.remove(address)
        notifyInFlight.remove(address)
        ingesters.remove(address)?.summaryLine(reportCountProvider())?.let(eventLog::log)
    }

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

    private fun isNotificationEnabled(value: ByteArray): Boolean =
        value.isNotEmpty() && (value[0].toInt() and NOTIFY_FLAG) != 0

    private data class OutgoingFrame(
        val characteristic: BluetoothGattCharacteristic,
        val payload: ByteArray,
    )

    private companion object {
        const val NOTIFY_FLAG = 0x01
        const val DEFAULT_MTU = 23
        const val ATT_OVERHEAD = 3
    }
}
