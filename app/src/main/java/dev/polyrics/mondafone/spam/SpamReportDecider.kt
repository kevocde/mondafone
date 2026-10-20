package dev.polyrics.mondafone.spam

import dev.polyrics.mondafone.contacts.ContactsLookup

class SpamReportDecider(
    private val store: SpamStore,
    private val contacts: ContactsLookup,
) {
    fun shouldAsk(call: CallInfo): Boolean =
        call.type == CallType.INCOMING &&
            call.durationSeconds >= MIN_DURATION_SECONDS &&
            !contacts.isContact(call.number) &&
            !store.isRegistered(call.number)

    private companion object {
        const val MIN_DURATION_SECONDS = 3L
    }
}
