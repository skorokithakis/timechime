package com.stavros.timechime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

private const val CHIME_REQUEST_CODE = 1001

fun scheduleNextChimeAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val triggerAtMillis = nextTenMinuteBoundaryMillis()

    val intent = Intent(context, ChimeReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        CHIME_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
}

fun cancelChimeAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, ChimeReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        CHIME_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    alarmManager.cancel(pendingIntent)
}

// Returns the epoch millis of the next clock time where minutes % 10 == 0 and seconds == 0.
private fun nextTenMinuteBoundaryMillis(): Long {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance().apply {
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val currentMinute = calendar.get(Calendar.MINUTE)
    val minutesUntilBoundary = (10 - (currentMinute % 10)) % 10
    calendar.add(Calendar.MINUTE, minutesUntilBoundary)
    // Guard against the case where truncating seconds/milliseconds placed us on or before
    // the current instant (e.g. called exactly at a boundary with sub-millisecond slack).
    if (calendar.timeInMillis <= now) {
        calendar.add(Calendar.MINUTE, 10)
    }
    return calendar.timeInMillis
}
