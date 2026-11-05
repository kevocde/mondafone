package dev.polyrics.mondafone.spam

interface SpamReportStore {
    fun reportOwn(canonicalNumber: String, displayNumber: String): SpamReport?
    fun acceptRemoteReport(report: SpamReport): Boolean
    fun isReportedByMe(canonicalNumber: String): Boolean
    fun foreignReportCount(canonicalNumber: String): Int
    fun ownReports(): List<OwnSpamReport>
    fun sequenceInventory(): List<SequenceInventoryEntry>
    fun reportsByReporter(createdBy: String, fromSeq: Long, toSeq: Long): List<SpamReport>
    fun reportCount(): Int
    fun spamNumberCount(notOlderThan: Long): Int
    fun networkDeviceCount(notOlderThan: Long, excludePubkey: String): Int
    fun removeOwnReport(phoneHash: String)
    fun registerOnChangeListener(listener: () -> Unit)
    fun unregisterOnChangeListener()
}

data class OwnSpamReport(
    val phoneHash: String,
    val displayNumber: String,
    val seqNum: Long,
    val createdAt: Long,
)
