package dev.polyrics.mondafono.spam

data class PeerStatus(
    val idPubkey: String,
    val maxSeqNum: Long,
    val lastSyncAt: Long,
)
