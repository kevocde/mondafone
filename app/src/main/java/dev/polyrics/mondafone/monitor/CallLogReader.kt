package dev.polyrics.mondafone.monitor

import android.content.ContentResolver
import android.provider.CallLog
import dev.polyrics.mondafone.spam.CallInfo
import dev.polyrics.mondafone.spam.CallType

data class LoggedCall(val id: Long, val info: CallInfo)

class CallLogReader(private val resolver: ContentResolver) {

    fun latestCall(): LoggedCall? {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
        )
        val sortOrder = "${CallLog.Calls.DATE} DESC"
        return resolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, sortOrder)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                LoggedCall(
                    id = cursor.getLong(0),
                    info = CallInfo(
                        number = cursor.getString(1) ?: return null,
                        type = toCallType(cursor.getInt(2)),
                        durationSeconds = cursor.getLong(3),
                    ),
                )
            }
    }

    private fun toCallType(type: Int): CallType = when (type) {
        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
        else -> CallType.REJECTED
    }
}
