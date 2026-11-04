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

class GattSyncClient(
    private val context: Context,
    private val device: BluetoothDevice,
    private val eventLog: BeaconEventLog,
    private val codec: SequenceInventoryCodec = SequenceInventoryCodec(),
) {

    private var gatt: BluetoothGatt? = null
    private var probeSent = false

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
            handleState("state read", characteristic, characteristic.value)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            handleState("state read", characteristic, value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            handleState("state notified", characteristic, characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleState("state notified", characteristic, value)
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

    private fun handleState(label: String, characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
        if (characteristic.uuid != GattSyncProtocol.STATE_UUID) return
        val entries = codec.decode(value)
        if (entries == null) {
            eventLog.log("$label decode failed")
            return
        }
        eventLog.log("$label: ${entries.size} entries")
        if (label == READ_LABEL) sendProbeOnce()
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

    @Suppress("DEPRECATION")
    private fun sendProbeOnce() {
        if (probeSent) return
        val current = gatt ?: return
        val data = current
            .getService(GattSyncProtocol.SERVICE_UUID)
            ?.getCharacteristic(GattSyncProtocol.DATA_UUID)
        if (data == null) {
            eventLog.log("data characteristic missing")
            return
        }
        probeSent = true
        val sent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            current.writeCharacteristic(data, DATA_PROBE, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            data.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            data.value = DATA_PROBE
            current.writeCharacteristic(data)
        }
        eventLog.log(if (sent) "data probe sent" else "data probe failed")
    }

    private companion object {
        const val READ_LABEL = "state read"
        val DATA_PROBE = byteArrayOf(0x01)
    }
}
