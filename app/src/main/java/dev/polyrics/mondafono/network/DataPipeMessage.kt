package dev.polyrics.mondafono.network

import dev.polyrics.mondafono.spam.SequenceInventoryEntry
import dev.polyrics.mondafono.spam.SpamReport

sealed interface DataPipeMessage {
    data class Requests(val requests: List<ReportRequest>) : DataPipeMessage
    data class OfferState(val entries: List<SequenceInventoryEntry>) : DataPipeMessage
    data class Reports(val reports: List<SpamReport>) : DataPipeMessage
    data object End : DataPipeMessage
    data object Wait : DataPipeMessage
    data object Ready : DataPipeMessage
}
