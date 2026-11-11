package dev.polyrics.mondafone.network

enum class ReportVerdict {
    VALID,
    INVALID_SIGNATURE,
    SEQUENCE_ROLLBACK,
    FROM_FUTURE,
    EXPIRED,
}
