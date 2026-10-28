package dev.polyrics.mondafone.network

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import dev.polyrics.mondafone.MondafoneApp
import dev.polyrics.mondafone.R
import java.time.Duration
import java.time.LocalDateTime

class BleBeaconService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val config by lazy { BeaconConfig.fromBuild() }
    private val schedule by lazy { CronExpression.parse(config.cronExpression) }
    private val codec = BeaconPayloadCodec()
    private val serviceUuid by lazy { ParcelUuid(BeaconProtocol.SERVICE_UUID) }
    private var counter = BeaconCycleCounter()
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var activeId = ""
    private var started = false

    private val eventLog: BeaconEventLog
        get() = (application as MondafoneApp).beaconEventLog

    private val identity by lazy { (application as MondafoneApp).deviceIdentity }

    private val cycle = object : Runnable {
        override fun run() {
            startCycle()
            handler.postDelayed({ stopWindow() }, config.windowMs)
            scheduleNext()
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            eventLog.log("advertise failed: $errorCode")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val id = codec.decode(result.scanRecord?.getServiceData(serviceUuid)) ?: return
            if (id == activeId) return
            counter.record()
            eventLog.log("received id $id")
        }

        override fun onScanFailed(errorCode: Int) {
            eventLog.log("scan failed: $errorCode")
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(FOREGROUND_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasBluetoothPermissions()) return START_NOT_STICKY
        if (!started) {
            started = true
            scheduleNext()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        started = false
        handler.removeCallbacksAndMessages(null)
        stopWindow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startCycle() {
        if (!hasBluetoothPermissions()) {
            eventLog.log("ble permission missing")
            return
        }
        val deviceId = identity.publicKeyFingerprint()
        if (deviceId == null) {
            eventLog.log("identity unavailable")
            return
        }
        if (!codec.fits(deviceId)) {
            eventLog.log("device id too long")
            return
        }
        val adapter = bluetoothAdapter()
        if (adapter == null || !adapter.isEnabled) {
            eventLog.log("bluetooth unavailable")
            return
        }
        val newAdvertiser = adapter.bluetoothLeAdvertiser
        val newScanner = adapter.bluetoothLeScanner
        if (newAdvertiser == null || newScanner == null) {
            eventLog.log("ble unavailable")
            return
        }
        advertiser = newAdvertiser
        scanner = newScanner
        activeId = deviceId
        counter = BeaconCycleCounter()
        eventLog.log("Beacon active")
        startAdvertising(newAdvertiser, deviceId)
        startScanning(newScanner)
    }

    private fun startAdvertising(target: BluetoothLeAdvertiser, deviceId: String) {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceData(serviceUuid, codec.encode(deviceId))
            .build()
        runCatching { target.startAdvertising(settings, data, advertiseCallback) }
            .onFailure { eventLog.log("advertise unavailable") }
    }

    private fun startScanning(target: BluetoothLeScanner) {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        runCatching { target.startScan(emptyList<ScanFilter>(), settings, scanCallback) }
            .onFailure { eventLog.log("scan unavailable") }
    }

    private fun scheduleNext() {
        val cron = schedule
        if (cron == null) {
            eventLog.log("invalid schedule")
            return
        }
        val now = LocalDateTime.now()
        val next = cron.nextAfter(now)
        if (next == null) {
            eventLog.log("no next schedule")
            return
        }
        val delay = Duration.between(now, next).toMillis().coerceAtLeast(0)
        handler.postDelayed(cycle, delay)
    }

    private fun stopWindow() {
        val activeAdvertiser = advertiser
        val activeScanner = scanner
        if (activeAdvertiser == null && activeScanner == null) return
        activeAdvertiser?.let { runCatching { it.stopAdvertising(advertiseCallback) } }
        activeScanner?.let { runCatching { it.stopScan(scanCallback) } }
        advertiser = null
        scanner = null
        eventLog.log("count: ${counter.count()}")
    }

    private fun hasBluetoothPermissions(): Boolean =
        BLUETOOTH_PERMISSIONS.all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }

    private fun bluetoothAdapter(): BluetoothAdapter? =
        getSystemService(BluetoothManager::class.java).adapter

    private fun buildNotification(): Notification {
        ensureChannel()
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(getString(R.string.beacon_channel_name))
            .setContentText(getString(R.string.beacon_notification_text))
            .setOngoing(true)
            .build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.beacon_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "beacon"
        const val FOREGROUND_ID = 1
        val BLUETOOTH_PERMISSIONS = listOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    }
}
