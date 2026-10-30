package dev.polyrics.mondafone.spam

import android.app.Application
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class SqlitePeerStatusStoreTest {

    private val app: Application = RuntimeEnvironment.getApplication()
    private lateinit var store: PeerStatusStore

    @Before
    fun setUp() {
        store = SqlitePeerStatusStore(app)
    }

    @After
    fun tearDown() {
        app.deleteDatabase(VerdictContract.DATABASE_NAME)
    }

    @Test
    fun upsertStoresNewPeer() {
        store.upsert(PeerStatus("peer-a", maxSeqNum = 4, lastSyncAt = 100))

        assertEquals(PeerStatus("peer-a", 4, 100), store.find("peer-a"))
    }

    @Test
    fun upsertOverwritesExistingPeer() {
        store.upsert(PeerStatus("peer-a", maxSeqNum = 4, lastSyncAt = 100))
        store.upsert(PeerStatus("peer-a", maxSeqNum = 9, lastSyncAt = 200))

        assertEquals(PeerStatus("peer-a", 9, 200), store.find("peer-a"))
        assertEquals(1, store.all().size)
    }

    @Test
    fun findReturnsNullForUnknownPeer() {
        assertNull(store.find("peer-unknown"))
    }

    @Test
    fun allReturnsPeersWithLatestSyncFirst() {
        store.upsert(PeerStatus("peer-a", 1, 100))
        store.upsert(PeerStatus("peer-b", 2, 300))
        store.upsert(PeerStatus("peer-c", 3, 200))

        assertEquals(listOf("peer-b", "peer-c", "peer-a"), store.all().map { it.idPubkey })
    }
}
