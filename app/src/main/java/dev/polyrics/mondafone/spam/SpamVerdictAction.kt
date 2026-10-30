package dev.polyrics.mondafone.spam

enum class SpamVerdictAction(val intentAction: String) {
    MARK_SPAM("dev.polyrics.mondafone.ACTION_MARK_SPAM") {
        override fun applyTo(dependencies: SpamDependencies, number: String) {
            val canonicalNumber = dependencies.phoneNumberNormalizer.normalize(number) ?: number
            dependencies.spamReportStore.reportOwn(canonicalNumber, number)
            dependencies.verdictStore.remove(canonicalNumber)
        }
    },
    MARK_NOT_SPAM("dev.polyrics.mondafone.ACTION_MARK_NOT_SPAM") {
        override fun applyTo(dependencies: SpamDependencies, number: String) {
            val canonicalNumber = dependencies.phoneNumberNormalizer.normalize(number) ?: number
            dependencies.verdictStore.addToWhitelist(canonicalNumber)
        }
    };

    abstract fun applyTo(dependencies: SpamDependencies, number: String)

    companion object {
        fun fromIntentAction(action: String?): SpamVerdictAction? =
            entries.firstOrNull { it.intentAction == action }
    }
}
