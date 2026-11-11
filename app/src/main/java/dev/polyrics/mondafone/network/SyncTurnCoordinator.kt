package dev.polyrics.mondafone.network

class SyncTurnCoordinator {

    private val waitQueue = ArrayDeque<Waiter>()
    private var activeId: String? = null

    @Synchronized
    fun requestTurn(
        id: String,
        onWait: () -> Unit = {},
        onReady: () -> Unit,
    ): Boolean {
        if (activeId == null || activeId == id) {
            activeId = id
            onReady()
            return true
        }
        if (waitQueue.none { it.id == id }) {
            waitQueue.addLast(Waiter(id, onReady))
        }
        onWait()
        return false
    }

    @Synchronized
    fun releaseTurn(id: String) {
        waitQueue.removeAll { it.id == id }
        if (activeId != id) return
        val next = waitQueue.removeFirstOrNull()
        activeId = next?.id
        next?.onReady?.invoke()
    }

    @Synchronized
    fun clear() {
        waitQueue.clear()
        activeId = null
    }

    private data class Waiter(
        val id: String,
        val onReady: () -> Unit,
    )
}
