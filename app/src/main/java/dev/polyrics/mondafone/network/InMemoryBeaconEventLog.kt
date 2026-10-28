package dev.polyrics.mondafone.network

import java.time.LocalTime
import java.time.format.DateTimeFormatter

class InMemoryBeaconEventLog(
    private val maxLines: Int = DEFAULT_MAX_LINES,
    private val timestamp: () -> String = { LocalTime.now().format(TIME_FORMAT) },
) : BeaconEventLog {

    private val entries = ArrayDeque<String>()

    @Volatile
    private var listener: ((List<String>) -> Unit)? = null

    override fun log(event: String) {
        val snapshot = synchronized(this) {
            entries.addLast("[${timestamp()}] $event")
            while (entries.size > maxLines) entries.removeFirst()
            entries.toList()
        }
        listener?.invoke(snapshot)
    }

    override fun lines(): List<String> = synchronized(this) { entries.toList() }

    override fun setListener(listener: ((List<String>) -> Unit)?) {
        this.listener = listener
        listener?.invoke(lines())
    }

    private companion object {
        const val DEFAULT_MAX_LINES = 20
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}
