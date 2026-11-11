package dev.polyrics.mondafone.network

class GattOutbox {

    private val queue = ArrayDeque<Packet>()
    private var activeTag: String? = null

    @Synchronized
    fun enqueue(tag: String, send: () -> Boolean, onFailure: () -> Unit = {}) {
        queue.addLast(Packet(tag, send, onFailure))
        pump()
    }

    @Synchronized
    fun onPacketSent() {
        activeTag = null
        pump()
    }

    @Synchronized
    fun cancel(tag: String) {
        queue.removeAll { it.tag == tag }
        if (activeTag == tag) {
            activeTag = null
            pump()
        }
    }

    @Synchronized
    fun clear() {
        queue.clear()
        activeTag = null
    }

    private fun pump() {
        while (activeTag == null) {
            val next = queue.removeFirstOrNull() ?: return
            if (next.send()) {
                activeTag = next.tag
            } else {
                queue.removeAll { it.tag == next.tag }
                next.onFailure()
            }
        }
    }

    private data class Packet(
        val tag: String,
        val send: () -> Boolean,
        val onFailure: () -> Unit,
    )
}
