package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.spam.SpamReport

class ReportIngester(
    private val validator: ReportValidator,
    private val persister: (SpamReport) -> Boolean,
    private val peerMaxSeqProvider: (String) -> Long,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val lastSeqByPeer = mutableMapOf<String, Long>()
    private var accepted = 0
    private var dropped = 0

    @Synchronized
    fun ingest(reports: List<SpamReport>) {
        reports.forEach(::ingest)
    }

    @Synchronized
    fun ingest(report: SpamReport) {
        val lastKnown = lastSeqByPeer.getOrPut(report.createdBy) { peerMaxSeqProvider(report.createdBy) }
        val verdict = validator.validate(report, lastKnown, clock())
        if (verdict != ReportVerdict.VALID || !persister(report)) {
            dropped++
            return
        }
        lastSeqByPeer[report.createdBy] = report.seqNum
        accepted++
    }

    @Synchronized
    fun summary(): IngestionSummary = IngestionSummary(accepted, dropped)

    @Synchronized
    fun summaryLine(total: Int): String? {
        if (accepted == 0 && dropped == 0) return null
        val droppedText = if (dropped > 0) ", dropped $dropped" else ""
        return "sync: got $accepted$droppedText, total $total"
    }
}
