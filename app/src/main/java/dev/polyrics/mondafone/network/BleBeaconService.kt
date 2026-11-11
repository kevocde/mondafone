package dev.polyrics.mondafone.network

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import dev.polyrics.mondafone.BuildConfig
import dev.polyrics.mondafone.MondafoneApp
import dev.polyrics.mondafone.R
import dev.polyrics.mondafone.spam.SpamDependencies
import java.util.concurrent.TimeUnit

class BleBeaconService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val config by lazy { BeaconConfig.fromBuild() }
    private val schedule by lazy { CronExpression.parse(config.cronExpression) }
    private val scheduler by lazy { BeaconAlarmScheduler(this, schedule) }
    private val rotator by lazy { RotatingBeaconId(config.appSecret) }
    private val codec = BeaconPayloadCodec()
    private val serviceUuid by lazy { ParcelUuid(BeaconProtocol.SERVICE_UUID) }
    private var counter = BeaconCycleCounter()
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var activeId = ""
    private val roleResolver = PeerRoleResolver()
    private val discoveredPeers = mutableMapOf<String, BluetoothDevice>()
    private val centralQueue = CentralSyncQueue<BluetoothDevice>()
    private val outbox = GattOutbox()
    private val turnCoordinator = SyncTurnCoordinator()
    private var activeClient: GattSyncClient? = null
    private var gattServer: GattSyncServer? = null

    private val eventLog: BeaconEventLog
        get() = (application as MondafoneApp).beaconEventLog

    private val identity by lazy { (application as MondafoneApp).deviceIdentity }

    private val spamReportStore by lazy { (application as SpamDependencies).spamReportStore }

    private val peerStatusStore by lazy { (application as SpamDependencies).peerStatusStore }

    private val reportValidator by lazy {
        ReportValidator(TimeUnit.DAYS.toMillis(BuildConfig.REPORT_MAX_AGE_DAYS))
    }

    private fun reportIngester(): ReportIngester = ReportIngester(
        validator = reportValidator,
        persister = spamReportStore::acceptRemoteReport,
        peerMaxSeqProvider = { pubkey -> peerStatusStore.find(pubkey)?.maxSeqNum ?: 0L },
    )

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            eventLog.log("advertise failed: $errorCode")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val id = codec.decode(result.scanRecord?.getServiceData(serviceUuid)) ?: return
            if (id == activeId) return
            counter.record(id)
            discoveredPeers[id] = result.device
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
        if (schedule == null) eventLog.log("invalid schedule")
        logLateness(intent)
        scheduler.scheduleNext()
        startDiscoveryPhase()
        handler.postDelayed({ startTransmissionPhase() }, config.windowMs)
        handler.postDelayed({ finishWindow() }, config.windowMs + config.gattLifetimeMs)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopWindow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startDiscoveryPhase() {
        if (!hasBluetoothPermissions()) {
            eventLog.log("ble permission missing")
            return
        }
        val fingerprint = identity.publicKeyFingerprint()
        if (fingerprint == null) {
            eventLog.log("identity unavailable")
            return
        }
        val beaconId = rotator.current(fingerprint)
        if (!codec.fits(beaconId)) {
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
        activeId = beaconId
        counter = BeaconCycleCounter()
        discoveredPeers.clear()
        centralQueue.clear()
        outbox.clear()
        turnCoordinator.clear()
        eventLog.log("Beacon active")
        openGattServer()
        startAdvertising(newAdvertiser, beaconId)
        startScanning(newScanner)
    }

    private fun startTransmissionPhase() {
        stopScanning()
        discoveredPeers.forEach { (peerId, device) ->
            when (roleResolver.roleFor(activeId, peerId)) {
                PeerRole.CENTRAL -> centralQueue.enqueue(CentralPeer(peerId, device.address, device))
                PeerRole.PERIPHERAL -> eventLog.log("peer $peerId -> peripheral")
                null -> Unit
            }
        }
        processNextCentral()
    }

    private fun processNextCentral() {
        val next = centralQueue.pollNext() ?: return
        val turnId = "client:${next.address}"
        turnCoordinator.requestTurn(
            id = turnId,
            onReady = { connectCentral(next, turnId) },
        )
    }

    private fun connectCentral(next: CentralPeer<BluetoothDevice>, turnId: String) {
        eventLog.log("peer ${next.peerId} -> central")
        val client = GattSyncClient(
            context = this,
            device = next.device,
            eventLog = eventLog,
            localInventoryProvider = { spamReportStore.sequenceInventory() },
            ownPubkeyProvider = { identity.publicKeyBase64() },
            reportsProvider = spamReportStore::reportsByReporter,
            reportCountProvider = spamReportStore::reportCount,
            ingester = reportIngester(),
            idleTimeoutMs = BuildConfig.SYNC_IDLE_TIMEOUT_MS,
            outbox = outbox,
            onFinished = {
                centralQueue.completeCurrent()
                activeClient = null
                turnCoordinator.releaseTurn(turnId)
                processNextCentral()
            },
        )
        activeClient = client
        client.connect()
    }

    private fun stopScanning() {
        scanner?.let { runCatching { it.stopScan(scanCallback) } }
        scanner = null
    }

    private fun openGattServer() {
        val server = GattSyncServer(
            context = this,
            inventoryProvider = { spamReportStore.sequenceInventory() },
            reportsProvider = spamReportStore::reportsByReporter,
            reportCountProvider = spamReportStore::reportCount,
            ownPubkeyProvider = { identity.publicKeyBase64() },
            ingesterFactory = ::reportIngester,
            eventLog = eventLog,
            outbox = outbox,
            turnCoordinator = turnCoordinator,
        )
        if (!server.open(bluetoothManager())) {
            eventLog.log("gatt server unavailable")
            return
        }
        gattServer = server
    }

    private fun startAdvertising(target: BluetoothLeAdvertiser, beaconId: String) {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceData(serviceUuid, codec.encode(beaconId))
            .build()
        runCatching { target.startAdvertising(settings, data, advertiseCallback) }
            .onFailure { eventLog.log("advertise unavailable") }
    }

    private fun startScanning(target: BluetoothLeScanner) {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        runCatching { target.startScan(BeaconScanFilter.filters(), settings, scanCallback) }
            .onFailure { eventLog.log("scan unavailable") }
    }

    private fun logLateness(intent: Intent?) {
        val expectedAt = intent?.getLongExtra(BeaconAlarmScheduler.EXTRA_EXPECTED_AT, 0L) ?: 0L
        if (expectedAt <= 0L) return
        val lateMillis = System.currentTimeMillis() - expectedAt
        if (lateMillis > LATE_THRESHOLD_MS) eventLog.log("window late ${lateMillis / 1000}s")
    }

    private fun finishWindow() {
        stopWindow()
        stopSelf()
    }

    private fun stopWindow() {
        val activeAdvertiser = advertiser
        if (activeAdvertiser == null && scanner == null) {
            closeGatt()
            return
        }
        stopScanning()
        activeAdvertiser?.let { runCatching { it.stopAdvertising(advertiseCallback) } }
        advertiser = null
        closeGatt()
        eventLog.log("count: ${counter.count()}")
    }

    private fun closeGatt() {
        centralQueue.clear()
        turnCoordinator.clear()
        outbox.clear()
        activeClient?.close()
        activeClient = null
        gattServer?.close()
        gattServer = null
    }

    private fun hasBluetoothPermissions(): Boolean =
        BLUETOOTH_PERMISSIONS.all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }

    private fun bluetoothManager(): BluetoothManager =
        getSystemService(BluetoothManager::class.java)

    private fun bluetoothAdapter(): BluetoothAdapter? = bluetoothManager().adapter

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
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.beacon_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        )
        notificationManager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "beacon_windows"
        const val LEGACY_CHANNEL_ID = "beacon"
        const val FOREGROUND_ID = 1
        const val LATE_THRESHOLD_MS = 5_000L
        val BLUETOOTH_PERMISSIONS = listOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    }
}
