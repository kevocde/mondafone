package dev.polyrics.mondafone.network

data class ReportRequest(
    val createdBy: String,
    val fromSeq: Long,
    val toSeq: Long,
)
