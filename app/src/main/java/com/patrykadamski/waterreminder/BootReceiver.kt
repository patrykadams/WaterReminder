package com.patrykadamski.waterreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
        // Only reschedule if the user had reminders running before the reboot.
        if (!prefs.contains("next_alarm_time")) return

        AlarmScheduler.scheduleNextAlarm(context)
    }
}
