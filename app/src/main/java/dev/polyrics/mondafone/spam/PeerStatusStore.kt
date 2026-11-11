package dev.polyrics.mondafone.spam

interface PeerStatusStore {
    fun upsert(peer: PeerStatus)
    fun recordReport(idPubkey: String, seqNum: Long, syncedAt: Long)
    fun find(idPubkey: String): PeerStatus?
    fun all(): List<PeerStatus>
}
