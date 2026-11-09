package dev.polyrics.mondafone.network

data class CentralPeer<T>(
    val peerId: String,
    val address: String,
    val device: T,
)

class CentralSyncQueue<T> {

    private val pending = ArrayDeque<CentralPeer<T>>()
    private val seenAddresses = mutableSetOf<String>()
    private var active: CentralPeer<T>? = null

    fun enqueue(peer: CentralPeer<T>): Boolean {
        if (!seenAddresses.add(peer.address)) return false
        pending.addLast(peer)
        return true
    }

    fun pollNext(): CentralPeer<T>? {
        if (active != null) return null
        active = pending.removeFirstOrNull()
        return active
    }

    fun activePeer(): CentralPeer<T>? = active

    fun completeCurrent(): CentralPeer<T>? {
        val completed = active
        active = null
        return completed
    }

    fun hasPending(): Boolean = active != null || pending.isNotEmpty()

    fun clear() {
        pending.clear()
        seenAddresses.clear()
        active = null
    }
}
