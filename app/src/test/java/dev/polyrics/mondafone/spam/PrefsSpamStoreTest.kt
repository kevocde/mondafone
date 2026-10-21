package dev.polyrics.mondafone.spam

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsSpamStoreTest {

    private val store = PrefsSpamStore(FakeSharedPreferences())

    @Test
    fun countsTotalSpamAndNotSpam() {
        store.mark("600111222", isSpam = true)
        store.mark("600333444", isSpam = true)
        store.mark("600555666", isSpam = false)

        val stats = store.stats()
        assertEquals(3, stats.total)
        assertEquals(2, stats.spam)
        assertEquals(1, stats.notSpam)
    }

    @Test
    fun clearsAllRegisteredNumbers() {
        store.mark("600111222", isSpam = true)
        store.mark("600555666", isSpam = false)

        store.clear()

        val stats = store.stats()
        assertEquals(0, stats.total)
        assertFalse(store.isRegistered("600111222"))
        assertFalse(store.isRegistered("600555666"))
    }

    @Test
    fun isSpamReturnsTrueForSpamNumber() {
        store.mark("600111222", isSpam = true)

        assertTrue(store.isSpam("600111222"))
    }

    @Test
    fun isSpamReturnsFalseForNotSpamNumber() {
        store.mark("600111222", isSpam = false)

        assertFalse(store.isSpam("600111222"))
    }

    @Test
    fun isSpamReturnsFalseForUnknownNumber() {
        assertFalse(store.isSpam("600999888"))
    }

    @Test
    fun isSpamReturnsFalseAfterClear() {
        store.mark("600111222", isSpam = true)

        store.clear()

        assertFalse(store.isSpam("600111222"))
    }

    @Test
    fun addPendingStoresNumberAsPending() {
        store.addPending("600111222")

        assertEquals(listOf("600111222"), store.pendingNumbers())
    }

    @Test
    fun pendingNumberIsNotRegistered() {
        store.addPending("600111222")

        assertFalse(store.isRegistered("600111222"))
    }

    @Test
    fun addPendingDoesNotOverwriteExistingVerdict() {
        store.mark("600111222", isSpam = true)

        store.addPending("600111222")

        assertTrue(store.isSpam("600111222"))
    }

    @Test
    fun markRemovesFromPending() {
        store.addPending("600111222")

        store.mark("600111222", isSpam = true)

        assertEquals(emptyList<String>(), store.pendingNumbers())
        assertTrue(store.isRegistered("600111222"))
    }

    @Test
    fun clearRemovesAllPending() {
        store.addPending("600111222")
        store.addPending("600333444")

        store.clear()

        assertEquals(emptyList<String>(), store.pendingNumbers())
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
