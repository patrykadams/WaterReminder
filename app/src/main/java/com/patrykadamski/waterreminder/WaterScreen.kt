package com.patrykadamski.waterreminder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Main dashboard screen updated to support Material You Dynamic Colors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(viewModel: WaterViewModel) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        GoalSettingsDialog(viewModel = viewModel, onDismiss = { showSettingsDialog = false })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Water Reminder", color = MaterialTheme.colorScheme.onBackground) },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Ustawienia celu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Progress Visualization
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant
                val progress = (viewModel.waterIntake.toFloat() / viewModel.dailyGoal.toFloat()).coerceIn(0f, 1f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw droplet outline using Theme outline color
                    drawCircle(color = outlineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))

                    // Fill droplet using Dynamic primary color
                    clipRect(top = size.height * (1f - progress)) {
                        drawCircle(color = primaryColor)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${viewModel.waterIntake} ml",
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Goal: ${viewModel.dailyGoal} ml",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Actions using Material 3 button themes
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { viewModel.addWater(viewModel.quickAddAmount) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("+ ${viewModel.quickAddAmount} ml")
                }

                OutlinedButton(
                    onClick = { viewModel.resetWater() },
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // History Header
            Text(
                "Last 7 Days",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )

            // History List using Theme colors
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(viewModel.records) { record ->
                    ListItem(
                        headlineContent = { Text(record.date, color = MaterialTheme.colorScheme.onSurface) },
                        trailingContent = { Text("${record.amount} ml", color = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }
}