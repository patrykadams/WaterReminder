package com.patrykadamski.waterreminder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val ACTIVITY_OPTIONS = listOf(
    WaterGoalCalculator.ACTIVITY_LOW to "Niski",
    WaterGoalCalculator.ACTIVITY_MEDIUM to "Umiarkowany",
    WaterGoalCalculator.ACTIVITY_HIGH to "Wysoki"
)

/**
 * Shared weight / activity / goal inputs used by both the onboarding screen
 * and the settings dialog, so the suggestion logic and its disclaimer only
 * live in one place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorFields(
    weightText: String,
    onWeightTextChange: (String) -> Unit,
    activityLevel: String,
    onActivityLevelChange: (String) -> Unit,
    goalText: String,
    onGoalTextChange: (String) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = weightText,
            onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) onWeightTextChange(it) },
            label = { Text("Waga (kg)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Poziom aktywności fizycznej", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ACTIVITY_OPTIONS.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = activityLevel == value,
                    onClick = { onActivityLevelChange(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ACTIVITY_OPTIONS.size),
                    icon = {}
                ) {
                    Text(
                        label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val weightKg = weightText.toIntOrNull() ?: 0
        val suggested = WaterGoalCalculator.suggestedGoalMl(weightKg, activityLevel)

        Text(
            "Sugerowany cel: $suggested ml",
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(onClick = { onGoalTextChange(suggested.toString()) }) {
            Text("Użyj sugerowanej wartości")
        }

        OutlinedTextField(
            value = goalText,
            onValueChange = { if (it.length <= 5 && it.all(Char::isDigit)) onGoalTextChange(it) },
            label = { Text("Dzienny cel (ml)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "To orientacyjne wyliczenie (waga × 32 ml + korekta za aktywność), nie zalecenie medyczne. " +
                "Możesz swobodnie dostosować wartość do swoich potrzeb.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
