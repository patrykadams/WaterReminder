package com.patrykadamski.waterreminder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(viewModel: WaterViewModel) {
    var weightText by remember { mutableStateOf(viewModel.userWeight.toString()) }
    var activityLevel by remember { mutableStateOf(WaterGoalCalculator.ACTIVITY_LOW) }
    var goalText by remember {
        mutableStateOf(
            WaterGoalCalculator.suggestedGoalMl(viewModel.userWeight, WaterGoalCalculator.ACTIVITY_LOW).toString()
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Witaj!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Podaj swoją wagę i poziom aktywności fizycznej, żebyśmy mogli zaproponować Ci dzienny cel picia wody.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            GoalEditorFields(
                weightText = weightText,
                onWeightTextChange = { weightText = it },
                activityLevel = activityLevel,
                onActivityLevelChange = { activityLevel = it },
                goalText = goalText,
                onGoalTextChange = { goalText = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val weightKg = weightText.toIntOrNull()?.coerceAtLeast(1) ?: viewModel.userWeight
                    val goal = goalText.toIntOrNull()?.coerceAtLeast(1)
                        ?: WaterGoalCalculator.suggestedGoalMl(weightKg, activityLevel)
                    viewModel.completeOnboarding(weightKg, activityLevel, goal)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rozpocznij")
            }
        }
    }
}
