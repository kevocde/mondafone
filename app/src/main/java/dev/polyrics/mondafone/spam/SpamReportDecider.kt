package dev.polyrics.mondafone.spam

class SpamReportDecider(
    private val store: SpamStore,
) {
    fun shouldAsk(call: CallInfo): Boolean =
        call.type == CallType.INCOMING && !store.isRegistered(call.number)
}
