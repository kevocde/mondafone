package dev.polyrics.mondafono.spam

interface ReportSigner {
    fun sign(phoneHash: String, createdAt: Long, seqNum: Long): String?
}
