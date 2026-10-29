package dev.polyrics.mondafone.spam

class PrefsCallVerdictStoreTest : CallVerdictStoreContractTest() {

    override fun createStore(): CallVerdictStore =
        PrefsCallVerdictStore(FakeSharedPreferences())
}
