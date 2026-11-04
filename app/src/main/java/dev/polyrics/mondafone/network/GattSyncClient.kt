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

class GattSyncClient(
    private val context: Context,
    private val device: BluetoothDevice,
    private val eventLog: BeaconEventLog,
    private val localInventoryProvider: () -> List<SequenceInventoryEntry>,
    private val ownPubkeyProvider: () -> String?,
    private val inventoryCodec: SequenceInventoryCodec = SequenceInventoryCodec(),
    private val requestCodec: ReportRequestCodec = ReportRequestCodec(),
    private val planner: SyncRequestPlanner = SyncRequestPlanner(),
) {

    private var gatt: BluetoothGatt? = null
    private var syncRequested = false

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
            val state = stateCharacteristic(gatt) ?: return
            gatt.setCharacteristicNotification(state, true)
            subscribe(gatt, state)
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
            handleNotification(characteristic, characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleNotification(characteristic, value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (descriptor.characteristic.uuid != GattSyncProtocol.STATE_UUID) return
            eventLog.log("state subscribed")
            val state = stateCharacteristic(gatt) ?: return
            if (!gatt.readCharacteristic(state)) eventLog.log("state read failed")
        }
    }

    private fun handleRead(characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
        val entries = decodeState(characteristic, value) ?: return
        eventLog.log("state read: ${entries.size} entries")
        requestReports(entries)
    }

    private fun handleNotification(characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
        val entries = decodeState(characteristic, value) ?: return
        eventLog.log("state notified: ${entries.size} entries")
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
        val data = current
            .getService(GattSyncProtocol.SERVICE_UUID)
            ?.getCharacteristic(GattSyncProtocol.DATA_UUID)
        if (data == null) {
            eventLog.log("data characteristic missing")
            return false
        }
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

    private fun stateCharacteristic(gatt: BluetoothGatt): BluetoothGattCharacteristic? {
        val characteristic = gatt
            .getService(GattSyncProtocol.SERVICE_UUID)
            ?.getCharacteristic(GattSyncProtocol.STATE_UUID)
        if (characteristic == null) eventLog.log("state characteristic missing")
        return characteristic
    }

    @Suppress("DEPRECATION")
    private fun subscribe(gatt: BluetoothGatt, state: BluetoothGattCharacteristic) {
        val descriptor = state.getDescriptor(GattSyncProtocol.CCC_DESCRIPTOR_UUID)
        if (descriptor == null) {
            eventLog.log("state descriptor missing")
            return
        }
        val sent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
        eventLog.log(if (sent) "state subscribe sent" else "state subscribe failed")
    }

    private companion object {
        const val LOG_KEY_CHARS = 8
    }
}
