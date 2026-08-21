package com.patrykadamski.waterreminder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Main dashboard screen updated to support Material You Dynamic Colors.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WaterScreen(viewModel: WaterViewModel) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    var customAmountText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    if (showSettingsDialog) {
        GoalSettingsDialog(viewModel = viewModel, onDismiss = { showSettingsDialog = false })
    }

    // Surfaces ViewModel-side messages (cooldown warning, undo confirmation, ...)
    // that previously had nowhere to render.
    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
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

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "🔥 Streak: ${viewModel.streakDays} ${StreakCalculator.dayWord(viewModel.streakDays)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (viewModel.nextAlarmTime.isNotEmpty()) {
                    Text(
                        text = "⏰ Następne przypomnienie: ${viewModel.nextAlarmTime}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quick-add buttons, one per user-defined vessel size, plus Reset.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    viewModel.vesselSizes.forEach { size ->
                        Button(
                            onClick = { viewModel.addWater(size.amountMl) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("${size.name} +${size.amountMl}ml")
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetWater() },
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.primary)
                    }

                    if (viewModel.lastAddedAmount > 0) {
                        OutlinedButton(
                            onClick = { viewModel.undoLastAdd() },
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text("Cofnij ${viewModel.lastAddedAmount}ml", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom amount, typed directly from the keyboard.
                val customAmount = customAmountText.toIntOrNull()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { if (it.length <= 5 && it.all(Char::isDigit)) customAmountText = it },
                        label = { Text("Własna ilość (ml)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (customAmount != null && customAmount > 0) {
                                viewModel.addWater(customAmount)
                                customAmountText = ""
                            }
                        }),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (customAmount != null && customAmount > 0) {
                                viewModel.addWater(customAmount)
                                customAmountText = ""
                            }
                        },
                        enabled = customAmount != null && customAmount > 0
                    ) {
                        Text("Dodaj")
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

        if (viewModel.showConfetti) {
            ConfettiOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}
