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
    private val localInventoryProvider: () -> List<SequenceInventoryEntry>,
    private val ownPubkeyProvider: () -> String?,
    private val reportsProvider: (createdBy: String, fromSeq: Long, toSeq: Long) -> List<SpamReport>,
    private val reportCountProvider: () -> Int,
    private val ingester: ReportIngester,
    private val idleTimeoutMs: Long,
    private val outbox: GattOutbox = GattOutbox(),
    private val onFinished: () -> Unit = {},
    private val pipeCodec: DataPipeCodec = DataPipeCodec(),
    private val planner: SyncRequestPlanner = SyncRequestPlanner(),
) {

    private val outboxTag = "client:${device.address}"
    private var gatt: BluetoothGatt? = null
    private var mtu = DEFAULT_MTU
    private var phase = Phase.AWAIT_STATE
    private var sessionStarted = false
    private var serverInventory: List<SequenceInventoryEntry> = emptyList()
    private var disconnecting = false
    private val pendingRequests = mutableListOf<ReportRequest>()
    private val handler = Handler(Looper.getMainLooper())
    private val label: String get() = "client ${device.address}"
    private val idleTimeout = Runnable {
        BeaconLog.log("$label idle timeout")
        disconnect()
    }
    private val connectTimeout = Runnable {
        BeaconLog.log("$label connect timeout")
        disconnect()
    }

    private var completed = false
    private var cacheRefreshed = false

    fun connect() {
        BeaconLog.log("$label connecting")
        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        handler.postDelayed(connectTimeout, CONNECT_TIMEOUT_MS)
    }

    fun close() {
        cancelIdleTimer()
        handler.removeCallbacks(connectTimeout)
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
            if (status != BluetoothGatt.GATT_SUCCESS) {
                BeaconLog.log("$label state change status $status new $newState")
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        disconnect()
                        return
                    }
                    BeaconLog.log("$label connected")
                    if (!gatt.requestMtu(GattSyncProtocol.TARGET_MTU)) {
                        gatt.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    cancelIdleTimer()
                    handler.removeCallbacks(connectTimeout)
                    outbox.cancel(outboxTag)
                    logSummary()
                    runCatching { gatt.close() }
                    this@GattSyncClient.gatt = null
                    notifyFinished()
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            BeaconLog.log("$label mtu $mtu status $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                this@GattSyncClient.mtu = mtu
            }
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                BeaconLog.log("$label services discovery failed: $status")
                disconnect()
                return
            }
            handler.removeCallbacks(connectTimeout)
            BeaconLog.log("$label services discovered")
            if (gatt.getService(GattSyncProtocol.SERVICE_UUID) == null && !cacheRefreshed) {
                cacheRefreshed = true
                if (refreshDeviceCache(gatt) && gatt.discoverServices()) return
            }
            restartIdleTimer()
            subscribe(gatt, GattSyncProtocol.DATA_UUID, DATA_LABEL)
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
            handleWrite(characteristic, status)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                BeaconLog.log("$label descriptor write failed: $status")
                disconnect()
                return
            }
            if (descriptor.characteristic.uuid == GattSyncProtocol.DATA_UUID) {
                restartIdleTimer()
            }
        }
    }

    private fun startSession(serverInventory: List<SequenceInventoryEntry>) {
        if (sessionStarted) return
        sessionStarted = true
        phase = Phase.AWAIT_PULL_RESPONSE
        val requests = planner.plan(serverInventory, localInventoryProvider(), ownPubkeyProvider())
        BeaconLog.log("$label pull: ${requests.size} requests")
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
            is DataPipeMessage.Reports -> {
                BeaconLog.log("$label got ${message.reports.size} reports")
                ingester.ingest(message.reports)
            }
            is DataPipeMessage.Requests -> {
                BeaconLog.log("$label got ${message.requests.size} requests")
                pendingRequests.addAll(message.requests)
            }
            DataPipeMessage.End -> advancePhase()
            DataPipeMessage.Wait -> {
                BeaconLog.log("$label wait")
                restartIdleTimer()
            }
            DataPipeMessage.Ready -> BeaconLog.log("$label ready")
            is DataPipeMessage.OfferState -> {
                BeaconLog.log("$label got ${message.entries.size} offers")
                serverInventory = serverInventory + message.entries
            }
            null -> Unit
        }
    }

    private fun advancePhase() {
        when (phase) {
            Phase.AWAIT_STATE -> startSession(serverInventory)
            Phase.AWAIT_PULL_RESPONSE -> sendOffer()
            Phase.AWAIT_REQUESTS -> pushReports()
            Phase.DONE -> disconnect()
        }
    }

    private fun sendOffer() {
        phase = Phase.AWAIT_REQUESTS
        val inventory = localInventoryProvider()
        BeaconLog.log("$label offer: ${inventory.size} entries")
        enqueue(pipeCodec.encode(DataPipeMessage.OfferState(inventory), maxPayload()))
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
        BeaconLog.log("$label push: ${reports.size} reports, total ${reportCountProvider()}")
    }

    private fun handleWrite(characteristic: BluetoothGattCharacteristic, status: Int) {
        if (characteristic.uuid != GattSyncProtocol.DATA_UUID) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            BeaconLog.log("$label write status: $status")
            disconnect()
            return
        }
        restartIdleTimer()
        outbox.onPacketSent()
    }

    private fun enqueue(frames: List<ByteArray>) {
        frames.forEach { frame ->
            outbox.enqueue(
                tag = outboxTag,
                send = { sendFrame(frame) },
                onFailure = {
                    BeaconLog.log("$label write failed")
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
        BeaconLog.log("$label disconnect")
        cancelIdleTimer()
        handler.removeCallbacks(connectTimeout)
        outbox.cancel(outboxTag)
        runCatching { gatt?.disconnect() }
    }

    private fun restartIdleTimer() {
        if (disconnecting) return
        handler.removeCallbacks(idleTimeout)
        handler.postDelayed(idleTimeout, idleTimeoutMs)
    }

    private fun cancelIdleTimer() {
        handler.removeCallbacks(idleTimeout)
    }

    private fun logSummary() {
        ingester.consumeSummaryLine(reportCountProvider())?.let(BeaconLog::log)
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
            BeaconLog.log("$label descriptor missing")
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
            BeaconLog.log("$label subscribe failed")
            disconnect()
        }
    }

    private fun characteristic(gatt: BluetoothGatt, uuid: UUID): BluetoothGattCharacteristic? {
        val characteristic = gatt
            .getService(GattSyncProtocol.SERVICE_UUID)
            ?.getCharacteristic(uuid)
        if (characteristic == null) BeaconLog.log("$uuid characteristic missing")
        return characteristic
    }

    private fun refreshDeviceCache(gatt: BluetoothGatt): Boolean =
        runCatching { gatt.javaClass.getMethod("refresh").invoke(gatt) as? Boolean == true }
            .getOrDefault(false)

    private enum class Phase {
        AWAIT_STATE,
        AWAIT_PULL_RESPONSE,
        AWAIT_REQUESTS,
        DONE,
    }

    private companion object {
        const val DATA_LABEL = "data"
        const val DEFAULT_MTU = 23
        const val ATT_OVERHEAD = 3
        const val CONNECT_TIMEOUT_MS = 5_000L
    }
}
