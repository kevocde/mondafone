package dev.polyrics.mondafono.spam

data class SpamReport(
    val phoneHash: String,
    val createdBy: String,
    val seqNum: Long,
    val createdAt: Long,
    val signature: String,
)
