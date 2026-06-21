package com.example.fitness_tracker.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.fitness_tracker.FitnessApplication
import com.example.fitness_tracker.MainActivity
import com.example.fitness_tracker.R
import com.example.fitness_tracker.data.FitnessRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        // Receivers must finish onReceive quickly; do the DB read on a background scope.
        // We hold a pending result so the process isn't killed before we post.
        val pending = goAsync()
        scope.launch {
            try {
                val repo = FitnessRepository.get(context)
                val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val focus = repo.focusForDay(day)
                postNotification(context, focus)
                // Reschedule for tomorrow at the same time.
                val s = repo.getReminderSettings()
                if (s != null && s.enabled) {
                    ReminderScheduler.scheduleNext(context, s.hour, s.minute)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun postNotification(context: Context, focus: String?) {
        FitnessApplication.ensureReminderChannel(context)
        val title = "Time to train"
        val text = if (focus.isNullOrBlank()) "Open Fitness Tracker to start logging."
        else "Today: $focus. Tap to open the plan."

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, FitnessApplication.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(REMINDER_NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_FIRE = "com.example.fitness_tracker.action.REMINDER_FIRE"
        const val REMINDER_NOTIFICATION_ID = 1001
    }
}
