package com.patrykadamski.waterreminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.patrykadamski.waterreminder.ui.theme.WaterReminderTheme

/**
 * The main entry point of the application.
 * This activity initializes the ViewModel and sets up the Compose UI with Dynamic Theme.
 */
class MainActivity : ComponentActivity() {

    // Initialize the ViewModel using the activityViewModels delegate
    private val waterViewModel: WaterViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        requestExactAlarmPermissionIfNeeded()

        // Ensure the app content can be drawn under the status bar if needed
        // (Optional: WindowCompat.setDecorFitsSystemWindows(window, false))

        setContent {
            // Apply the custom Material You theme we created
            WaterReminderTheme {
                // Surface provides the background color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (waterViewModel.showOnboarding) {
                        null -> Unit // wciąż ustalane, nic jeszcze nie rysujemy
                        true -> OnboardingScreen(viewModel = waterViewModel)
                        false -> WaterScreen(viewModel = waterViewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data (like next alarm time) when user returns to app
        waterViewModel.refreshData()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!alreadyGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (!AlarmScheduler.hasExactAlarmPermission(this)) {
            AlarmScheduler.requestExactAlarmPermission(this)
        }
    }
}