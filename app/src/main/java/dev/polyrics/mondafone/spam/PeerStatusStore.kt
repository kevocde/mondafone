package dev.polyrics.mondafone.spam

interface PeerStatusStore {
    fun upsert(peer: PeerStatus)
    fun find(idPubkey: String): PeerStatus?
    fun all(): List<PeerStatus>
}
