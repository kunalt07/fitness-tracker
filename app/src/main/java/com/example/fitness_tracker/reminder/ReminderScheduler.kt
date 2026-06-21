package com.example.fitness_tracker.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {
    private const val REQUEST_CODE = 7771

    fun scheduleNext(context: Context, hour: Int, minute: Int) {
        val triggerAtMs = nextTriggerMs(hour, minute)
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)
        // Exact-and-allow-while-idle is the only path that survives Doze on consumer
        // devices for a once-a-day nudge. On Android 12+ this needs SCHEDULE_EXACT_ALARM
        // *or* USE_EXACT_ALARM (granted by default for calendar/reminder use cases).
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
                // Fall back to inexact — still fires daily, just within a maintenance window.
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } else {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            }
        } catch (_: SecurityException) {
            // Some OEMs revoke exact alarms aggressively — fall back gracefully.
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun pendingIntent(context: Context, flagsExtra: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            flagsExtra or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** The next epoch ms for the given local hour:minute, today if still in future, else tomorrow. */
    private fun nextTriggerMs(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis
    }
}
