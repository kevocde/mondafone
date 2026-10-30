package dev.polyrics.mondafone.spam

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SqlitePeerStatusStore(private val helper: SQLiteOpenHelper) : PeerStatusStore {

    constructor(context: Context) : this(VerdictDbHelper(context))

    override fun upsert(peer: PeerStatus) {
        val values = ContentValues().apply {
            put(VerdictContract.COLUMN_ID_PUBKEY, peer.idPubkey)
            put(VerdictContract.COLUMN_MAX_SEQ_NUM, peer.maxSeqNum)
            put(VerdictContract.COLUMN_LAST_SYNC_AT, peer.lastSyncAt)
        }
        database().insertWithOnConflict(
            VerdictContract.TABLE_PEERS_STATUS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    override fun find(idPubkey: String): PeerStatus? =
        query("${VerdictContract.COLUMN_ID_PUBKEY} = ?", arrayOf(idPubkey)).firstOrNull()

    override fun all(): List<PeerStatus> = query(null, null)

    private fun query(selection: String?, arguments: Array<String>?): List<PeerStatus> =
        database()
            .query(
                VerdictContract.TABLE_PEERS_STATUS,
                null,
                selection,
                arguments,
                null,
                null,
                "${VerdictContract.COLUMN_LAST_SYNC_AT} DESC",
            )
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.toPeerStatus())
                }
            }

    private fun Cursor.toPeerStatus(): PeerStatus = PeerStatus(
        idPubkey = getString(getColumnIndexOrThrow(VerdictContract.COLUMN_ID_PUBKEY)),
        maxSeqNum = getLong(getColumnIndexOrThrow(VerdictContract.COLUMN_MAX_SEQ_NUM)),
        lastSyncAt = getLong(getColumnIndexOrThrow(VerdictContract.COLUMN_LAST_SYNC_AT)),
    )

    private fun database(): SQLiteDatabase = helper.writableDatabase
}
