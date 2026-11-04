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
import java.util.UUID

class GattSyncServer(
    private val context: Context,
    private val inventoryProvider: () -> List<SequenceInventoryEntry>,
    private val eventLog: BeaconEventLog,
    private val codec: SequenceInventoryCodec = SequenceInventoryCodec(),
) {

    private var server: BluetoothGattServer? = null
    private var stateCharacteristic: BluetoothGattCharacteristic? = null

    fun open(manager: BluetoothManager): Boolean {
        val newServer = manager.openGattServer(context, callback) ?: return false
        val service = buildService()
        if (!newServer.addService(service)) {
            runCatching { newServer.close() }
            return false
        }
        stateCharacteristic = service.getCharacteristic(GattSyncProtocol.STATE_UUID)
        server = newServer
        eventLog.log("gatt server open")
        return true
    }

    fun close() {
        val current = server ?: return
        runCatching { current.clearServices() }
        runCatching { current.close() }
        server = null
        stateCharacteristic = null
        eventLog.log("gatt server closed")
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
                BluetoothProfile.STATE_CONNECTED -> eventLog.log("peripheral link up ${device.address}")
                BluetoothProfile.STATE_DISCONNECTED -> eventLog.log("peripheral link down ${device.address}")
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            eventLog.log("peripheral mtu $mtu")
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
            val entries = inventoryProvider()
            val payload = codec.encode(entries)
            val chunk = payload.copyOfRange(offset.coerceAtMost(payload.size), payload.size)
            respond(device, requestId, offset, chunk)
            eventLog.log("state read: ${entries.size} entries")
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
            eventLog.log("data write: ${value.size} bytes")
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
            if (!isNotificationEnabled(value)) return
            eventLog.log("state notify on ${device.address}")
            notifyState(device)
        }
    }

    @Suppress("DEPRECATION")
    private fun notifyState(device: BluetoothDevice) {
        val characteristic = stateCharacteristic ?: return
        val current = server ?: return
        val payload = codec.encode(inventoryProvider())
        val sent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            current.notifyCharacteristicChanged(device, characteristic, false, payload) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.value = payload
            current.notifyCharacteristicChanged(device, characteristic, false)
        }
        eventLog.log(if (sent) "state notified" else "state notify failed")
    }

    private fun respond(device: BluetoothDevice, requestId: Int, offset: Int, value: ByteArray?) {
        runCatching { server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value) }
    }

    private fun isNotificationEnabled(value: ByteArray): Boolean =
        value.isNotEmpty() && (value[0].toInt() and NOTIFY_FLAG) != 0

    private companion object {
        const val NOTIFY_FLAG = 0x01
    }
}
