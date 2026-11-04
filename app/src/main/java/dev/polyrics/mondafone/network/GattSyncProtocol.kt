package dev.polyrics.mondafone.network

import java.util.UUID

object GattSyncProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("6d6f6e64-6166-6f6e-6500-000000000002")
    val STATE_UUID: UUID = UUID.fromString("6d6f6e64-6166-6f6e-6500-000000000010")
    val DATA_UUID: UUID = UUID.fromString("6d6f6e64-6166-6f6e-6500-000000000011")
    val CCC_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    const val TARGET_MTU = 512
}
