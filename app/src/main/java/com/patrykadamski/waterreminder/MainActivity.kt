package com.patrykadamski.waterreminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Main entry point of the application.
 * Responsible for initializing the UI, creating notification channels,
 * and handling runtime permissions for Android 13+.
 */
class MainActivity : ComponentActivity() {

    /**
     * Launcher for requesting POST_NOTIFICATIONS permission on Android 13 (API 33+).
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handling (optional logging or UI feedback can be added here)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize the notification channel (Required for Android 8.0+)
        createNotificationChannel()

        // 2. Check and request notification permissions if needed
        checkNotificationPermission()

        setContent {
            MaterialTheme {
                // Initialize the shared ViewModel
                val viewModel: WaterViewModel = viewModel()

                // Render the main screen
                WaterReminderScreen(viewModel)
            }
        }
    }

    /**
     * Creates the notification channel for water reminders.
     * This is required for notifications to be delivered on Android Oreo (API 26) and higher.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "water_reminder_channel"
            val name = "Przypomnienie o wodzie"
            val descriptionText = "Powiadomienia przypominające o piciu wody"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Requests notification permissions for devices running Android 13 (Tiramisu) or higher.
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}