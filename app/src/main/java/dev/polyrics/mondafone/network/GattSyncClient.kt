package dev.polyrics.mondafone.network

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import dev.polyrics.mondafone.spam.SequenceInventoryEntry
import dev.polyrics.mondafone.spam.SpamReport
import java.util.UUID

class GattSyncClient(
    private val context: Context,
    private val device: BluetoothDevice,
    private val eventLog: BeaconEventLog,
    private val localInventoryProvider: () -> List<SequenceInventoryEntry>,
    private val ownPubkeyProvider: () -> String?,
    private val reportsProvider: (createdBy: String, fromSeq: Long, toSeq: Long) -> List<SpamReport>,
    private val reportCountProvider: () -> Int,
    private val ingester: ReportIngester,
    private val idleTimeoutMs: Long,
    private val inventoryCodec: SequenceInventoryCodec = SequenceInventoryCodec(),
    private val pipeCodec: DataPipeCodec = DataPipeCodec(),
    private val planner: SyncRequestPlanner = SyncRequestPlanner(),
) {

    private var gatt: BluetoothGatt? = null
    private var mtu = DEFAULT_MTU
    private var phase = Phase.AWAIT_PULL_RESPONSE
    private var sessionStarted = false
    private var disconnecting = false
    private var writeInFlight = false
    private val pendingFrames = ArrayDeque<ByteArray>()
    private val pendingRequests = mutableListOf<ReportRequest>()
    private val handler = Handler(Looper.getMainLooper())
    private val idleTimeout = Runnable { disconnect() }

    fun connect() {
        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
    }

    fun close() {
        cancelIdleTimer()
        logSummary()
        val current = gatt ?: return
        runCatching { current.disconnect() }
        runCatching { current.close() }
        gatt = null
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    eventLog.log("connected ${gatt.device.address}")
                    gatt.requestMtu(GattSyncProtocol.TARGET_MTU)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    logSummary()
                    runCatching { gatt.close() }
                    this@GattSyncClient.gatt = null
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            this@GattSyncClient.mtu = mtu
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                eventLog.log("services discovery failed: $status")
                return
            }
            restartIdleTimer()
            subscribe(gatt, GattSyncProtocol.STATE_UUID, STATE_LABEL)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            handleRead(characteristic, characteristic.value)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            handleRead(characteristic, value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            handleChanged(characteristic, characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleChanged(characteristic, value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            handleWrite(characteristic)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            when (descriptor.characteristic.uuid) {
                GattSyncProtocol.STATE_UUID -> subscribe(gatt, GattSyncProtocol.DATA_UUID, DATA_LABEL)
                GattSyncProtocol.DATA_UUID -> readState(gatt)
            }
        }
    }

    private fun handleRead(characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
        if (characteristic.uuid != GattSyncProtocol.STATE_UUID) return
        val inventory = inventoryCodec.decode(value) ?: return
        startSession(inventory)
    }

    private fun startSession(serverInventory: List<SequenceInventoryEntry>) {
        if (sessionStarted) return
        sessionStarted = true
        val requests = planner.plan(serverInventory, localInventoryProvider(), ownPubkeyProvider())
        enqueue(pipeCodec.encode(DataPipeMessage.Requests(requests), maxPayload()))
        enqueue(pipeCodec.encode(DataPipeMessage.End, maxPayload()))
    }

    private fun handleChanged(characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
        if (characteristic.uuid != GattSyncProtocol.DATA_UUID) return
        restartIdleTimer()
        handleMessage(pipeCodec.decode(value))
    }

    private fun handleMessage(message: DataPipeMessage?) {
        when (message) {
            is DataPipeMessage.Reports -> ingester.ingest(message.reports)
            is DataPipeMessage.Requests -> pendingRequests.addAll(message.requests)
            DataPipeMessage.End -> advancePhase()
            is DataPipeMessage.OfferState, null -> Unit
        }
    }

    private fun advancePhase() {
        when (phase) {
            Phase.AWAIT_PULL_RESPONSE -> sendOffer()
            Phase.AWAIT_REQUESTS -> pushReports()
            Phase.DONE -> disconnect()
        }
    }

    private fun sendOffer() {
        phase = Phase.AWAIT_REQUESTS
        enqueue(pipeCodec.encode(DataPipeMessage.OfferState(localInventoryProvider()), maxPayload()))
        enqueue(pipeCodec.encode(DataPipeMessage.End, maxPayload()))
    }

    private fun pushReports() {
        phase = Phase.DONE
        val reports = pendingRequests.flatMap { request ->
            reportsProvider(request.createdBy, request.fromSeq, request.toSeq)
        }
        pendingRequests.clear()
        enqueue(pipeCodec.encode(DataPipeMessage.Reports(reports), maxPayload()))
        enqueue(pipeCodec.encode(DataPipeMessage.End, maxPayload()))
        if (reports.isNotEmpty()) {
            eventLog.log("sync: sent ${reports.size}, total ${reportCountProvider()}")
        }
    }

    private fun handleWrite(characteristic: BluetoothGattCharacteristic) {
        if (characteristic.uuid != GattSyncProtocol.DATA_UUID) return
        restartIdleTimer()
        writeInFlight = false
        pump()
    }

    private fun enqueue(frames: List<ByteArray>) {
        if (frames.isEmpty()) return
        frames.forEach(pendingFrames::addLast)
        pump()
    }

    private fun pump() {
        if (writeInFlight) return
        val frame = pendingFrames.removeFirstOrNull() ?: return
        val current = gatt ?: return
        val data = characteristic(current, GattSyncProtocol.DATA_UUID) ?: return
        if (!writeData(current, data, frame)) {
            eventLog.log("write failed")
            return
        }
        writeInFlight = true
    }

    private fun disconnect() {
        if (disconnecting) return
        disconnecting = true
        cancelIdleTimer()
        runCatching { gatt?.disconnect() }
    }

    private fun restartIdleTimer() {
        handler.removeCallbacks(idleTimeout)
        handler.postDelayed(idleTimeout, idleTimeoutMs)
    }

    private fun cancelIdleTimer() {
        handler.removeCallbacks(idleTimeout)
    }

    private fun logSummary() {
        ingester.consumeSummaryLine(reportCountProvider())?.let(eventLog::log)
    }

    private fun maxPayload(): Int = (mtu - ATT_OVERHEAD).coerceAtLeast(1)

    @Suppress("DEPRECATION")
    private fun writeData(
        gatt: BluetoothGatt,
        data: BluetoothGattCharacteristic,
        payload: ByteArray,
    ): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        gatt.writeCharacteristic(data, payload, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) ==
            BluetoothStatusCodes.SUCCESS
    } else {
        data.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        data.value = payload
        gatt.writeCharacteristic(data)
    }

    private fun readState(gatt: BluetoothGatt) {
        val state = characteristic(gatt, GattSyncProtocol.STATE_UUID) ?: return
        if (!gatt.readCharacteristic(state)) eventLog.log("state read failed")
    }

    private fun subscribe(gatt: BluetoothGatt, uuid: UUID, label: String) {
        val characteristic = characteristic(gatt, uuid) ?: return
        gatt.setCharacteristicNotification(characteristic, true)
        writeDescriptor(gatt, characteristic, label)
    }

    @Suppress("DEPRECATION")
    private fun writeDescriptor(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        label: String,
    ) {
        val descriptor = characteristic.getDescriptor(GattSyncProtocol.CCC_DESCRIPTOR_UUID)
        if (descriptor == null) {
            eventLog.log("$label descriptor missing")
            return
        }
        val sent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
        if (!sent) eventLog.log("$label subscribe failed")
    }

    private fun characteristic(gatt: BluetoothGatt, uuid: UUID): BluetoothGattCharacteristic? {
        val characteristic = gatt
            .getService(GattSyncProtocol.SERVICE_UUID)
            ?.getCharacteristic(uuid)
        if (characteristic == null) eventLog.log("$uuid characteristic missing")
        return characteristic
    }

    private enum class Phase {
        AWAIT_PULL_RESPONSE,
        AWAIT_REQUESTS,
        DONE,
    }

    private companion object {
        const val STATE_LABEL = "state"
        const val DATA_LABEL = "data"
        const val DEFAULT_MTU = 23
        const val ATT_OVERHEAD = 3
    }
}
