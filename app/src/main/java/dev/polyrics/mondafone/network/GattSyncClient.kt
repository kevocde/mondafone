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
    private val clock: () -> Long = System::currentTimeMillis,
    private val inventoryCodec: SequenceInventoryCodec = SequenceInventoryCodec(),
    private val requestCodec: ReportRequestCodec = ReportRequestCodec(),
    private val planner: SyncRequestPlanner = SyncRequestPlanner(),
    private val reportDecoder: ReportStreamDecoder = ReportStreamDecoder(),
) {

    private var gatt: BluetoothGatt? = null
    private var syncRequested = false
    private val lastSeqByPeer = mutableMapOf<String, Long>()

    fun connect() {
        eventLog.log("central connecting ${device.address}")
        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
    }

    fun close() {
        val current = gatt ?: return
        runCatching { current.disconnect() }
        runCatching { current.close() }
        gatt = null
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    eventLog.log("central connected ${gatt.device.address}")
                    gatt.requestMtu(GattSyncProtocol.TARGET_MTU)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    eventLog.log("central disconnected ${gatt.device.address}")
                    runCatching { gatt.close() }
                    this@GattSyncClient.gatt = null
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            eventLog.log("central mtu $mtu")
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                eventLog.log("services discovery failed: $status")
                return
            }
            eventLog.log("services discovered")
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
                GattSyncProtocol.STATE_UUID -> {
                    eventLog.log("state subscribed")
                    subscribe(gatt, GattSyncProtocol.DATA_UUID, DATA_LABEL)
                }
                GattSyncProtocol.DATA_UUID -> {
                    eventLog.log("data subscribed")
                    readState(gatt)
                }
            }
        }
    }

    private fun handleRead(characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
        val entries = decodeState(characteristic, value) ?: return
        eventLog.log("state read: ${entries.size} entries")
        requestReports(entries)
    }

    private fun handleChanged(characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
        when (characteristic.uuid) {
            GattSyncProtocol.STATE_UUID -> {
                val entries = decodeState(characteristic, value) ?: return
                eventLog.log("state notified: ${entries.size} entries")
            }
            GattSyncProtocol.DATA_UUID -> handleReports(value)
        }
    }

    private fun handleReports(value: ByteArray?) {
        if (value == null) return
        reportDecoder.feed(value).forEach(::handleReport)
    }

    private fun handleReport(report: SpamReport) {
        val peer = report.createdBy.take(LOG_KEY_CHARS)
        val lastKnown = lastSeqByPeer.getOrPut(report.createdBy) { peerMaxSeqProvider(report.createdBy) }
        val verdict = reportValidator.validate(report, lastKnown, clock())
        if (verdict != ReportVerdict.VALID) {
            eventLog.log("report $peer#${report.seqNum} dropped: ${verdictLabel(verdict)}")
            return
        }
        if (!reportPersister(report)) {
            eventLog.log("report $peer#${report.seqNum} dropped: duplicate")
            return
        }
        lastSeqByPeer[report.createdBy] = report.seqNum
        eventLog.log("report $peer#${report.seqNum} ok")
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
        if (requests.isEmpty()) {
            eventLog.log("sync up to date")
            return
        }
        requests.forEach { request ->
            eventLog.log("REQ: ${request.createdBy.take(LOG_KEY_CHARS)} [${request.fromSeq}..${request.toSeq}]")
        }
        val sent = writeRequests(requests)
        eventLog.log(if (sent) "req sent: ${requests.size}" else "req send failed")
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
        eventLog.log(if (sent) "$label subscribe sent" else "$label subscribe failed")
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

    private fun verdictLabel(verdict: ReportVerdict): String = when (verdict) {
        ReportVerdict.INVALID_SIGNATURE -> "signature"
        ReportVerdict.SEQUENCE_ROLLBACK -> "rollback"
        ReportVerdict.FROM_FUTURE -> "future"
        ReportVerdict.EXPIRED -> "expired"
        ReportVerdict.VALID -> "ok"
    }

    private companion object {
        const val LOG_KEY_CHARS = 8
        const val STATE_LABEL = "state"
        const val DATA_LABEL = "data"
    }
}
