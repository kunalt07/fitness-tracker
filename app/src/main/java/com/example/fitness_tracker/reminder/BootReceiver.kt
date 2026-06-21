package com.example.fitness_tracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fitness_tracker.data.FitnessRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        scope.launch {
            try {
                val repo = FitnessRepository.get(context)
                val s = repo.getReminderSettings()
                if (s != null && s.enabled) {
                    ReminderScheduler.scheduleNext(context, s.hour, s.minute)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
