package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.spam.TestIdentity
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportValidatorTest {

    private val identity = TestIdentity()
    private val validator = ReportValidator(maxAgeMillis = MAX_AGE_MILLIS)

    @Test
    fun acceptsVerifiableFreshReport() {
        assertEquals(ReportVerdict.VALID, validate(seqNum = 4L, createdAt = NOW))
    }

    @Test
    fun rejectsTamperedSignature() {
        val tampered = report(seqNum = 4L, createdAt = NOW).copy(createdAt = NOW + 1)

        assertEquals(ReportVerdict.INVALID_SIGNATURE, validate(tampered, lastKnownSeq = 3L))
    }

    @Test
    fun rejectsReportAtOrBelowKnownSequence() {
        assertEquals(ReportVerdict.SEQUENCE_ROLLBACK, validate(seqNum = 4L, lastKnownSeq = 4L, createdAt = NOW))
    }

    @Test
    fun acceptsForwardSequenceGap() {
        assertEquals(ReportVerdict.VALID, validate(seqNum = 8L, lastKnownSeq = 4L, createdAt = NOW))
    }

    @Test
    fun rejectsReportOlderThanMaxAge() {
        val expired = NOW - MAX_AGE_MILLIS - 1

        assertEquals(ReportVerdict.EXPIRED, validate(seqNum = 4L, createdAt = expired))
    }

    @Test
    fun acceptsReportAtMaxAgeBoundary() {
        val boundary = NOW - MAX_AGE_MILLIS

        assertEquals(ReportVerdict.VALID, validate(seqNum = 4L, createdAt = boundary))
    }

    @Test
    fun rejectsReportBeyondFutureTolerance() {
        val future = NOW + ReportValidator.FUTURE_TOLERANCE_MILLIS + 1

        assertEquals(ReportVerdict.FROM_FUTURE, validate(seqNum = 4L, createdAt = future))
    }

    @Test
    fun acceptsReportWithinFutureTolerance() {
        val skewed = NOW + ReportValidator.FUTURE_TOLERANCE_MILLIS

        assertEquals(ReportVerdict.VALID, validate(seqNum = 4L, createdAt = skewed))
    }

    @Test
    fun checksSignatureBeforeOtherRules() {
        val tampered = report(seqNum = 4L, createdAt = NOW - MAX_AGE_MILLIS - 1).copy(seqNum = 99L)

        assertEquals(ReportVerdict.INVALID_SIGNATURE, validate(tampered, lastKnownSeq = 3L))
    }

    private fun validate(report: dev.polyrics.mondafone.spam.SpamReport, lastKnownSeq: Long): ReportVerdict =
        validator.validate(report, lastKnownSeq, NOW)

    private fun validate(seqNum: Long, lastKnownSeq: Long = 0L, createdAt: Long): ReportVerdict =
        validate(report(seqNum, createdAt), lastKnownSeq)

    private fun report(seqNum: Long, createdAt: Long) =
        identity.signedReport(phoneHash = "phone-hash", seqNum = seqNum, createdAt = createdAt)

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val MAX_AGE_MILLIS = 20L * 24 * 60 * 60 * 1000
    }
}
