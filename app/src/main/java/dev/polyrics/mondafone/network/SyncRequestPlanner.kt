package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.spam.SequenceInventoryEntry

class SyncRequestPlanner {

    fun plan(
        remote: List<SequenceInventoryEntry>,
        local: List<SequenceInventoryEntry>,
        ownPubkey: String?,
    ): List<ReportRequest> {
        val localMax = local.associate { it.createdBy to it.maxSeqNum }
        return remote.mapNotNull { entry ->
            if (entry.createdBy == ownPubkey) return@mapNotNull null
            requestFor(entry, localMax[entry.createdBy])
        }
    }

    private fun requestFor(entry: SequenceInventoryEntry, localMax: Long?): ReportRequest? {
        if (entry.maxSeqNum < FIRST_SEQ_NUM) return null
        return when {
            localMax == null -> ReportRequest(entry.createdBy, FIRST_SEQ_NUM, entry.maxSeqNum)
            entry.maxSeqNum > localMax -> ReportRequest(entry.createdBy, localMax + 1, entry.maxSeqNum)
            else -> null
        }
    }

    private companion object {
        const val FIRST_SEQ_NUM = 1L
    }
}
