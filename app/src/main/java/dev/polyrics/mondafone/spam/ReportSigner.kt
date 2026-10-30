package dev.polyrics.mondafone.spam

interface ReportSigner {
    fun sign(phoneHash: String, createdAt: Long, seqNum: Long): String?
}
