package dev.polyrics.mondafone.network

class PeerRoleResolver {

    fun roleFor(myId: String, peerId: String): PeerRole? {
        val comparison = myId.compareTo(peerId)
        return when {
            comparison > 0 -> PeerRole.CENTRAL
            comparison < 0 -> PeerRole.PERIPHERAL
            else -> null
        }
    }
}
