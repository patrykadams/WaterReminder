package com.patrykadamski.waterreminder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val FREQUENCY_ORDER = listOf(
    ReminderPacing.FREQUENCY_LESS,
    ReminderPacing.FREQUENCY_NORMAL,
    ReminderPacing.FREQUENCY_MORE
)
private val FREQUENCY_LABELS = mapOf(
    ReminderPacing.FREQUENCY_LESS to "Rzadziej",
    ReminderPacing.FREQUENCY_NORMAL to "Normalnie",
    ReminderPacing.FREQUENCY_MORE to "Częściej"
)

@Composable
fun GoalSettingsDialog(viewModel: WaterViewModel, onDismiss: () -> Unit) {
    var weightText by remember { mutableStateOf(viewModel.userWeight.toString()) }
    var activityLevel by remember { mutableStateOf(WaterGoalCalculator.normalizeActivityLevel(viewModel.userActivity)) }
    var goalText by remember { mutableStateOf(viewModel.dailyGoal.toString()) }
    var frequencyIndex by remember {
        mutableStateOf(FREQUENCY_ORDER.indexOf(viewModel.reminderFrequency).coerceAtLeast(0))
    }
    var vesselDrafts by remember { mutableStateOf(viewModel.vesselSizes.toDrafts()) }
    var wakeUpHourText by remember { mutableStateOf(viewModel.wakeUpHour.toString()) }
    var sleepHourText by remember { mutableStateOf(viewModel.sleepHour.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cel picia wody") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GoalEditorFields(
                    weightText = weightText,
                    onWeightTextChange = { weightText = it },
                    activityLevel = activityLevel,
                    onActivityLevelChange = { activityLevel = it },
                    goalText = goalText,
                    onGoalTextChange = { goalText = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Godziny aktywności", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    OutlinedTextField(
                        value = wakeUpHourText,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) wakeUpHourText = it },
                        label = { Text("Pobudka (godz.)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = sleepHourText,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) sleepHourText = it },
                        label = { Text("Sen (godz.)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "Format 24-godzinny (0-23). Przypomnienia w ciągu dnia i wieczorne podsumowanie (godzina snu − 60 min) są liczone w tym przedziale.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Częstotliwość przypomnień: ${FREQUENCY_LABELS.getValue(FREQUENCY_ORDER[frequencyIndex])}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = frequencyIndex.toFloat(),
                    onValueChange = { frequencyIndex = it.roundToInt().coerceIn(0, FREQUENCY_ORDER.lastIndex) },
                    valueRange = 0f..(FREQUENCY_ORDER.lastIndex).toFloat(),
                    steps = FREQUENCY_ORDER.size - 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                VesselSizeEditor(
                    sizes = vesselDrafts,
                    onSizesChange = { vesselDrafts = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val weightKg = weightText.toIntOrNull()?.coerceAtLeast(1) ?: viewModel.userWeight
                val goal = goalText.toIntOrNull()?.coerceAtLeast(1) ?: viewModel.dailyGoal
                val wakeUpHour = wakeUpHourText.toIntOrNull()?.coerceIn(0, 23) ?: viewModel.wakeUpHour
                val sleepHour = sleepHourText.toIntOrNull()?.coerceIn(0, 23) ?: viewModel.sleepHour
                viewModel.saveSettings(
                    newGoal = goal,
                    newWeight = weightKg,
                    newWakeUp = wakeUpHour,
                    newSleep = sleepHour,
                    newGender = viewModel.userGender,
                    newActivity = activityLevel,
                    newFrequency = FREQUENCY_ORDER[frequencyIndex],
                    newVesselSizes = vesselDrafts.toVesselSizes()
                )
                onDismiss()
            }) { Text("Zapisz") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}
