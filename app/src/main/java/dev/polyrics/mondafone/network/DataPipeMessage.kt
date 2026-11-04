package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.spam.SequenceInventoryEntry
import dev.polyrics.mondafone.spam.SpamReport

sealed interface DataPipeMessage {
    data class Requests(val requests: List<ReportRequest>) : DataPipeMessage
    data class OfferState(val entries: List<SequenceInventoryEntry>) : DataPipeMessage
    data class Reports(val reports: List<SpamReport>) : DataPipeMessage
    data object End : DataPipeMessage
}
