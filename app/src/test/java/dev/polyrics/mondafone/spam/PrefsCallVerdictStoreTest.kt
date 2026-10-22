package dev.polyrics.mondafone.spam

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsCallVerdictStoreTest {

    private val store = PrefsCallVerdictStore(FakeSharedPreferences())

    @Test
    fun addToBlacklistMakesNumberBlacklisted() {
        store.addToBlacklist("600111222")

        assertTrue(store.isBlacklisted("600111222"))
        assertFalse(store.isWhitelisted("600111222"))
    }

    @Test
    fun addToWhitelistMakesNumberWhitelisted() {
        store.addToWhitelist("600111222")

        assertTrue(store.isWhitelisted("600111222"))
        assertFalse(store.isBlacklisted("600111222"))
    }

    @Test
    fun whitelistOverwritesBlacklist() {
        store.addToBlacklist("600111222")
        store.addToWhitelist("600111222")

        assertTrue(store.isWhitelisted("600111222"))
        assertFalse(store.isBlacklisted("600111222"))
    }

    @Test
    fun blacklistOverwritesWhitelist() {
        store.addToWhitelist("600111222")
        store.addToBlacklist("600111222")

        assertTrue(store.isBlacklisted("600111222"))
        assertFalse(store.isWhitelisted("600111222"))
    }

    @Test
    fun unclassifiedNumberIsNeitherWhitelistNorBlacklist() {
        assertFalse(store.isWhitelisted("600999888"))
        assertFalse(store.isBlacklisted("600999888"))
        assertFalse(store.isClassified("600999888"))
    }

    @Test
    fun isClassifiedReturnsTrueForWhitelistedNumber() {
        store.addToWhitelist("600111222")
        assertTrue(store.isClassified("600111222"))
    }

    @Test
    fun isClassifiedReturnsTrueForBlacklistedNumber() {
        store.addToBlacklist("600111222")
        assertTrue(store.isClassified("600111222"))
    }

    @Test
    fun statsCountsWhitelistAndBlacklist() {
        store.addToBlacklist("600111222")
        store.addToBlacklist("600333444")
        store.addToWhitelist("600555666")

        val stats = store.stats()
        assertEquals(2, stats.blacklist)
        assertEquals(1, stats.whitelist)
    }

    @Test
    fun clearRemovesAllNumbers() {
        store.addToBlacklist("600111222")
        store.addToWhitelist("600555666")

        store.clear()

        assertEquals(0, store.stats().blacklist)
        assertEquals(0, store.stats().whitelist)
        assertFalse(store.isClassified("600111222"))
        assertFalse(store.isClassified("600555666"))
    }

    @Test
    fun removeDeletesNumber() {
        store.addToBlacklist("600111222")

        store.remove("600111222")

        assertFalse(store.isClassified("600111222"))
    }

    @Test
    fun addPendingStoresNumberAsPending() {
        store.addPending("600111222")

        assertEquals(listOf("600111222"), store.pendingNumbers())
    }

    @Test
    fun pendingNumberIsNotClassified() {
        store.addPending("600111222")

        assertFalse(store.isClassified("600111222"))
    }

    @Test
    fun addPendingDoesNotOverwriteExistingVerdict() {
        store.addToBlacklist("600111222")

        store.addPending("600111222")

        assertTrue(store.isBlacklisted("600111222"))
    }

    @Test
    fun addToBlacklistRemovesFromPending() {
        store.addPending("600111222")

        store.addToBlacklist("600111222")

        assertEquals(emptyList<String>(), store.pendingNumbers())
        assertTrue(store.isClassified("600111222"))
    }

    @Test
    fun addToWhitelistRemovesFromPending() {
        store.addPending("600111222")

        store.addToWhitelist("600111222")

        assertEquals(emptyList<String>(), store.pendingNumbers())
        assertTrue(store.isClassified("600111222"))
    }

    @Test
    fun clearRemovesAllPending() {
        store.addPending("600111222")
        store.addPending("600333444")

        store.clear()

        assertEquals(emptyList<String>(), store.pendingNumbers())
    }

    @Test
    fun whitelistNumbersReturnsOnlyWhitelisted() {
        store.addToWhitelist("600111222")
        store.addToWhitelist("600333444")
        store.addToBlacklist("600555666")

        val whitelisted = store.whitelistNumbers()

        assertEquals(2, whitelisted.size)
        assertTrue(whitelisted.contains("600111222"))
        assertTrue(whitelisted.contains("600333444"))
        assertFalse(whitelisted.contains("600555666"))
    }

    @Test
    fun blacklistNumbersReturnsOnlyBlacklisted() {
        store.addToBlacklist("600111222")
        store.addToBlacklist("600333444")
        store.addToWhitelist("600555666")

        val blacklisted = store.blacklistNumbers()

        assertEquals(2, blacklisted.size)
        assertTrue(blacklisted.contains("600111222"))
        assertTrue(blacklisted.contains("600333444"))
        assertFalse(blacklisted.contains("600555666"))
    }
}

private class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = key in data
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class Editor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { pending[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String, value: Int): SharedPreferences.Editor = this
        override fun putLong(key: String, value: Long): SharedPreferences.Editor = this
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = this
        override fun remove(key: String): SharedPreferences.Editor = apply { pending[key] = null }
        override fun clear(): SharedPreferences.Editor = apply { clearRequested = true }
        override fun commit(): Boolean = true.also { apply() }
        override fun apply() {
            if (clearRequested) data.clear()
            pending.forEach { (k, v) -> if (v == null) data.remove(k) else data[k] = v }
        }
    }
}
