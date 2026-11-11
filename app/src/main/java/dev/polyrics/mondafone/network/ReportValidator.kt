package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.spam.ReportSignature
import dev.polyrics.mondafone.spam.SpamReport

class ReportValidator(
    private val maxAgeMillis: Long,
    private val futureToleranceMillis: Long = FUTURE_TOLERANCE_MILLIS,
) {

    fun validate(report: SpamReport, lastKnownSeq: Long, now: Long): ReportVerdict {
        if (!hasValidSignature(report)) return ReportVerdict.INVALID_SIGNATURE
        if (report.seqNum <= lastKnownSeq) return ReportVerdict.SEQUENCE_ROLLBACK
        if (report.createdAt > now + futureToleranceMillis) return ReportVerdict.FROM_FUTURE
        if (now - report.createdAt > maxAgeMillis) return ReportVerdict.EXPIRED
        return ReportVerdict.VALID
    }

    private fun hasValidSignature(report: SpamReport): Boolean =
        ReportSignature.verify(
            report.phoneHash,
            report.createdAt,
            report.seqNum,
            report.signature,
            report.createdBy,
        )

    companion object {
        const val FUTURE_TOLERANCE_MILLIS = 5L * 60 * 1000
    }
}
