package dev.polyrics.mondafono.spam

enum class CallType { INCOMING, OUTGOING, MISSED, REJECTED }

data class CallInfo(
    val number: String,
    val type: CallType,
    val durationSeconds: Long = 0L,
)
