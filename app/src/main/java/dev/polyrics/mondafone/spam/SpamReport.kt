package dev.polyrics.mondafone.spam

data class SpamReport(
    val phoneHash: String,
    val createdBy: String,
    val seqNum: Long,
    val createdAt: Long,
    val signature: String,
)
