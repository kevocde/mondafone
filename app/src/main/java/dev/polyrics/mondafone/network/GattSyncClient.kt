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
import dev.polyrics.mondafone.spam.SequenceInventoryEntry
import dev.polyrics.mondafone.spam.SpamReport

class GattSyncClient(
    private val context: Context,
    private val device: BluetoothDevice,
    private val eventLog: BeaconEventLog,
    private val localInventoryProvider: () -> List<SequenceInventoryEntry>,
    private val ownPubkeyProvider: () -> String?,
    private val reportValidator: ReportValidator,
    private val reportPersister: (SpamReport) -> Boolean,
    private val peerMaxSeqProvider: (String) -> Long,
    private val reportCountProvider: () -> Int,
    private val clock: () -> Long = System::currentTimeMillis,
    private val inventoryCodec: SequenceInventoryCodec = SequenceInventoryCodec(),
    private val requestCodec: ReportRequestCodec = ReportRequestCodec(),
    private val planner: SyncRequestPlanner = SyncRequestPlanner(),
    private val reportDecoder: ReportStreamDecoder = ReportStreamDecoder(),
) {

    private var gatt: BluetoothGatt? = null
    private var syncRequested = false
    private var receivedCount = 0
    private var droppedCount = 0
    private val lastSeqByPeer = mutableMapOf<String, Long>()

    fun connect() {
        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
    }

    fun close() {
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
                    runCatching { gatt.close() }
                    this@GattSyncClient.gatt = null
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                eventLog.log("services discovery failed: $status")
                return
            }
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
        requestReports(decodeState(characteristic, value) ?: return)
    }

    private fun handleChanged(characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
        if (characteristic.uuid == GattSyncProtocol.DATA_UUID) handleReports(value)
    }

    private fun handleReports(value: ByteArray?) {
        if (value == null) return
        reportDecoder.feed(value).forEach(::handleReport)
    }

    private fun handleReport(report: SpamReport) {
        val lastKnown = lastSeqByPeer.getOrPut(report.createdBy) { peerMaxSeqProvider(report.createdBy) }
        val verdict = reportValidator.validate(report, lastKnown, clock())
        if (verdict != ReportVerdict.VALID || !reportPersister(report)) {
            droppedCount++
            return
        }
        lastSeqByPeer[report.createdBy] = report.seqNum
        receivedCount++
    }

    private fun logSummary() {
        if (receivedCount == 0 && droppedCount == 0) return
        val dropped = if (droppedCount > 0) ", dropped $droppedCount" else ""
        eventLog.log("sync: got $receivedCount$dropped, total ${reportCountProvider()}")
        receivedCount = 0
        droppedCount = 0
    }

    private fun decodeState(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray?,
    ): List<SequenceInventoryEntry>? {
        if (characteristic.uuid != GattSyncProtocol.STATE_UUID) return null
        val entries = inventoryCodec.decode(value)
        if (entries == null) eventLog.log("state decode failed")
        return entries
    }

    private fun requestReports(remote: List<SequenceInventoryEntry>) {
        if (syncRequested) return
        syncRequested = true
        val requests = planner.plan(remote, localInventoryProvider(), ownPubkeyProvider())
        if (requests.isEmpty()) return
        if (!writeRequests(requests)) eventLog.log("req send failed")
    }

    private fun writeRequests(requests: List<ReportRequest>): Boolean {
        val current = gatt ?: return false
        val data = characteristic(current, GattSyncProtocol.DATA_UUID) ?: return false
        return writeData(current, data, requestCodec.encode(requests))
    }

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

    private fun subscribe(gatt: BluetoothGatt, uuid: java.util.UUID, label: String) {
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

    private fun characteristic(
        gatt: BluetoothGatt,
        uuid: java.util.UUID,
    ): BluetoothGattCharacteristic? {
        val characteristic = gatt
            .getService(GattSyncProtocol.SERVICE_UUID)
            ?.getCharacteristic(uuid)
        if (characteristic == null) eventLog.log("$uuid characteristic missing")
        return characteristic
    }

    private companion object {
        const val STATE_LABEL = "state"
        const val DATA_LABEL = "data"
    }
}
