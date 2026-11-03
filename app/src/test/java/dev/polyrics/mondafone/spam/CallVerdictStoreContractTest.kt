package dev.polyrics.mondafone.spam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

abstract class CallVerdictStoreContractTest {

    private lateinit var store: CallVerdictStore

    protected abstract fun createStore(): CallVerdictStore

    @Before
    fun setUp() {
        store = createStore()
    }

    @Test
    fun addToWhitelistMakesNumberWhitelisted() {
        store.addToWhitelist("600111222")

        assertTrue(store.isWhitelisted("600111222"))
    }

    @Test
    fun unclassifiedNumberIsNotWhitelisted() {
        assertFalse(store.isWhitelisted("600999888"))
        assertFalse(store.isClassified("600999888"))
    }

    @Test
    fun isClassifiedReturnsTrueForWhitelistedNumber() {
        store.addToWhitelist("600111222")
        assertTrue(store.isClassified("600111222"))
    }

    @Test
    fun clearRemovesAllNumbers() {
        store.addToWhitelist("600555666")

        store.clear()

        assertEquals(emptyList<String>(), store.whitelistNumbers())
        assertFalse(store.isClassified("600555666"))
    }

    @Test
    fun removeDeletesNumber() {
        store.addToWhitelist("600111222")

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
        store.addToWhitelist("600111222")

        store.addPending("600111222")

        assertTrue(store.isWhitelisted("600111222"))
        assertEquals(emptyList<String>(), store.pendingNumbers())
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
        store.addPending("600555666")

        val whitelisted = store.whitelistNumbers()

        assertEquals(2, whitelisted.size)
        assertTrue(whitelisted.contains("600111222"))
        assertTrue(whitelisted.contains("600333444"))
        assertFalse(whitelisted.contains("600555666"))
    }
}
