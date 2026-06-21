package com.example.fitness_tracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class FitnessApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureReminderChannel(this)
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "fitness_reminder"

        fun ensureReminderChannel(context: android.content.Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(REMINDER_CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Daily reminder",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Nudges to open the app and start today's workout."
                },
            )
        }
    }
}
