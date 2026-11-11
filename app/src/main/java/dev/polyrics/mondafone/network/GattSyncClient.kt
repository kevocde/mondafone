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
    private val outbox: GattOutbox = GattOutbox(),
    private val onFinished: () -> Unit = {},
    private val inventoryCodec: SequenceInventoryCodec = SequenceInventoryCodec(),
    private val pipeCodec: DataPipeCodec = DataPipeCodec(),
    private val planner: SyncRequestPlanner = SyncRequestPlanner(),
) {

    private val outboxTag = "client:${device.address}"
    private var gatt: BluetoothGatt? = null
    private var mtu = DEFAULT_MTU
    private var phase = Phase.AWAIT_PULL_RESPONSE
    private var sessionStarted = false
    private var disconnecting = false
    private val pendingRequests = mutableListOf<ReportRequest>()
    private val handler = Handler(Looper.getMainLooper())
    private val idleTimeout = Runnable { disconnect() }

    private var completed = false
    private var cacheRefreshed = false

    fun connect() {
        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
    }

    fun close() {
        cancelIdleTimer()
        outbox.cancel(outboxTag)
        logSummary()
        val current = gatt
        gatt = null
        if (current != null) {
            runCatching { current.disconnect() }
            runCatching { current.close() }
        }
        notifyFinished()
    }

    private fun notifyFinished() {
        if (!completed) {
            completed = true
            onFinished()
        }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        eventLog.log("connect failed: $status")
                        disconnect()
                        return
                    }
                    eventLog.log("connected ${gatt.device.address}")
                    if (!gatt.requestMtu(GattSyncProtocol.TARGET_MTU)) {
                        gatt.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    outbox.cancel(outboxTag)
                    logSummary()
                    runCatching { gatt.close() }
                    this@GattSyncClient.gatt = null
                    notifyFinished()
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                this@GattSyncClient.mtu = mtu
            }
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                eventLog.log("services discovery failed: $status")
                disconnect()
                return
            }
            if (gatt.getService(GattSyncProtocol.SERVICE_UUID) == null && !cacheRefreshed) {
                cacheRefreshed = true
                if (refreshDeviceCache(gatt) && gatt.discoverServices()) return
            }
            restartIdleTimer()
            subscribe(gatt, GattSyncProtocol.DATA_UUID, DATA_LABEL)
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
            if (status != BluetoothGatt.GATT_SUCCESS) {
                eventLog.log("descriptor write failed: $status")
                disconnect()
                return
            }
            if (descriptor.characteristic.uuid == GattSyncProtocol.DATA_UUID) {
                restartIdleTimer()
            }
        }
    }

    private fun handleRead(characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
        if (characteristic.uuid != GattSyncProtocol.STATE_UUID) return
        val inventory = inventoryCodec.decode(value) ?: emptyList()
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
            DataPipeMessage.Wait -> restartIdleTimer()
            DataPipeMessage.Ready -> gatt?.let(::readState)
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
        outbox.onPacketSent()
    }

    private fun enqueue(frames: List<ByteArray>) {
        frames.forEach { frame ->
            outbox.enqueue(
                tag = outboxTag,
                send = { sendFrame(frame) },
                onFailure = {
                    eventLog.log("write failed")
                    disconnect()
                },
            )
        }
    }

    private fun sendFrame(frame: ByteArray): Boolean {
        val current = gatt ?: return false
        val data = characteristic(current, GattSyncProtocol.DATA_UUID) ?: return false
        return writeData(current, data, frame)
    }

    private fun disconnect() {
        if (disconnecting) return
        disconnecting = true
        cancelIdleTimer()
        outbox.cancel(outboxTag)
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
        gatt.writeCharacteristic(data, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) ==
            BluetoothStatusCodes.SUCCESS
    } else {
        data.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        data.value = payload
        gatt.writeCharacteristic(data)
    }

    private fun readState(gatt: BluetoothGatt) {
        val state = characteristic(gatt, GattSyncProtocol.STATE_UUID) ?: run {
            disconnect()
            return
        }
        if (!gatt.readCharacteristic(state)) {
            eventLog.log("state read failed")
            disconnect()
        }
    }

    private fun subscribe(gatt: BluetoothGatt, uuid: UUID, label: String) {
        val characteristic = characteristic(gatt, uuid) ?: run {
            disconnect()
            return
        }
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
            disconnect()
            return
        }
        val sent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
        if (!sent) {
            eventLog.log("$label subscribe failed")
            disconnect()
        }
    }

    private fun characteristic(gatt: BluetoothGatt, uuid: UUID): BluetoothGattCharacteristic? {
        val characteristic = gatt
            .getService(GattSyncProtocol.SERVICE_UUID)
            ?.getCharacteristic(uuid)
        if (characteristic == null) eventLog.log("$uuid characteristic missing")
        return characteristic
    }

    private fun refreshDeviceCache(gatt: BluetoothGatt): Boolean =
        runCatching { gatt.javaClass.getMethod("refresh").invoke(gatt) as? Boolean == true }
            .getOrDefault(false)

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
