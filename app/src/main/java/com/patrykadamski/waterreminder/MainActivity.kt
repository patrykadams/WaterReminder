package com.patrykadamski.waterreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.patrykadamski.waterreminder.ui.theme.WaterReminderTheme

/**
 * The main entry point of the application.
 * This activity initializes the ViewModel and sets up the Compose UI with Dynamic Theme.
 */
class MainActivity : ComponentActivity() {

    // Initialize the ViewModel using the activityViewModels delegate
    private val waterViewModel: WaterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    // CRITICAL FIX: Calling 'WaterScreen' instead of 'WaterReminderScreen'
                    // to match the definition in WaterScreen.kt
                    WaterScreen(viewModel = waterViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data (like next alarm time) when user returns to app
        waterViewModel.refreshData()
    }
}