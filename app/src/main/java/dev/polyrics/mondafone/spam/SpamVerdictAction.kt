package dev.polyrics.mondafone.spam

enum class SpamVerdictAction(val intentAction: String) {
    MARK_SPAM("dev.polyrics.mondafone.ACTION_MARK_SPAM") {
        override fun applyTo(store: CallVerdictStore, number: String) =
            store.addToBlacklist(number)
    },
    MARK_NOT_SPAM("dev.polyrics.mondafone.ACTION_MARK_NOT_SPAM") {
        override fun applyTo(store: CallVerdictStore, number: String) =
            store.addToWhitelist(number)
    };

    abstract fun applyTo(store: CallVerdictStore, number: String)

    companion object {
        fun fromIntentAction(action: String?): SpamVerdictAction? =
            entries.firstOrNull { it.intentAction == action }
    }
}
