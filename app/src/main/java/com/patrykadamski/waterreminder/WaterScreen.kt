package com.patrykadamski.waterreminder

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WaterReminderScreen(viewModel: WaterViewModel) {
    val context = LocalContext.current

    // Odświeżanie danych po powrocie do aplikacji
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Pobieranie danych z ViewModel
    val waterIntake = viewModel.waterIntake
    val dailyGoal = viewModel.dailyGoal
    val historyRecords = viewModel.records
    val currentInterval = viewModel.alertInterval
    val currentWeight = viewModel.userWeight
    val currentQuickAdd = viewModel.quickAddAmount
    val currentWakeUp = viewModel.wakeUpHour
    val currentSleep = viewModel.sleepHour

    var showDialog by remember { mutableStateOf(false) }

    val progress = (waterIntake.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Nagłówek i przycisk ustawień
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text("Cel: $dailyGoal ml", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Kółko postępu
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            CircularProgressIndicator(
                progress = { animatedProgress }, modifier = Modifier.fillMaxSize(),
                color = Color(0xFF2196F3), strokeWidth = 12.dp,
                trackColor = Color.LightGray.copy(alpha = 0.3f), strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$waterIntake", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text("ml", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        // Przyciski dodawania
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { viewModel.addWater(currentQuickAdd) }) { Text("+$currentQuickAdd ml") }
            Button(onClick = { viewModel.addWater(500) }) { Text("+500 ml") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Przyciski funkcyjne
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = { viewModel.resetWater() }) { Text("Reset") }
            TextButton(onClick = { scheduleNotification(context) }) { Text("Test Powiadomienia") }
        }

        Spacer(modifier = Modifier.height(15.dp))
        Divider()
        Spacer(modifier = Modifier.height(10.dp))

        Text("Ostatnie 7 dni:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(15.dp))

        // WYKRES SŁUPKOWY
        // Odwracamy listę, żeby najstarszy dzień był z lewej
        WeekBarChart(data = historyRecords.asReversed())
    }

    // Okno dialogowe ustawień
    if (showDialog) {
        EditGoalDialog(
            currentGoal = dailyGoal,
            currentInterval = currentInterval,
            currentWeight = currentWeight,
            currentQuickAdd = currentQuickAdd,
            currentWakeUp = currentWakeUp,
            currentSleep = currentSleep,
            onDismiss = { showDialog = false },
            onConfirm = { g, i, w, q, wake, sleep ->
                viewModel.saveSettings(g, i, w, q, wake, sleep)
                showDialog = false
                scheduleNotification(context)
            }
        )
    }
}

// --- FUNKCJA RYSOWANIA WYKRESU ---
@Composable
fun WeekBarChart(data: List<WaterEntity>) {
    if (data.isEmpty()) {
        Text("Brak danych z tego tygodnia.", color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        return
    }

    // Obliczamy max, żeby skalować słupki
    val maxAmount = data.maxOfOrNull { it.amount } ?: 1
    val chartHeight = 150.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { entry ->
            val percentage = (entry.amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
            val animatedBarHeight by animateDpAsState(
                targetValue = chartHeight * percentage,
                label = "barHeight"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Słupek
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(animatedBarHeight)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(Color(0xFF2196F3).copy(alpha = 0.8f))
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Etykieta dnia
                Text(
                    text = getDayLabel(entry.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// --- NAPRAWIONA FUNKCJA POMOCNICZA ---
fun getDayLabel(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        dayName.uppercase() // Proste i bezpieczne zamienianie na duże litery
    } catch (e: Exception) {
        "?"
    }
}

@Composable
fun EditGoalDialog(
    currentGoal: Int,
    currentInterval: Int,
    currentWeight: Int,
    currentQuickAdd: Int,
    currentWakeUp: Int,
    currentSleep: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, Int, Int, Int) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    var intervalText by remember { mutableStateOf(currentInterval.toString()) }
    var weightText by remember { mutableStateOf(currentWeight.toString()) }
    var quickAddText by remember { mutableStateOf(currentQuickAdd.toString()) }
    var wakeUpText by remember { mutableStateOf(currentWakeUp.toString()) }
    var sleepText by remember { mutableStateOf(currentSleep.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ustawienia") },
        text = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = {
                            val f = it.filter { c -> c.isDigit() }
                            weightText = f
                            val w = f.toIntOrNull()
                            if (w != null && w > 0) goalText = (w * 33).toString()
                        },
                        label = { Text("Waga (kg)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it.filter { c -> c.isDigit() } },
                        label = { Text("Cel (ml)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Harmonogram:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { intervalText = it.filter { c -> c.isDigit() } },
                    label = { Text("Co ile minut?") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = wakeUpText,
                        onValueChange = { wakeUpText = it.filter { c -> c.isDigit() } },
                        label = { Text("Start (h)") },
                        placeholder = { Text("8") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = sleepText,
                        onValueChange = { sleepText = it.filter { c -> c.isDigit() } },
                        label = { Text("Koniec (h)") },
                        placeholder = { Text("22") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quickAddText,
                    onValueChange = { quickAddText = it.filter { c -> c.isDigit() } },
                    label = { Text("Przycisk +ml") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    goalText.toIntOrNull() ?: 2000,
                    intervalText.toIntOrNull() ?: 60,
                    weightText.toIntOrNull() ?: 70,
                    quickAddText.toIntOrNull() ?: 250,
                    wakeUpText.toIntOrNull() ?: 8,
                    sleepText.toIntOrNull() ?: 22
                )
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

fun scheduleNotification(context: Context) {
    val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
    val intervalMinutes = prefs.getInt("alert_interval", 60)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val intent = android.content.Intent(context, ReminderReceiver::class.java)
    val pendingIntent = android.app.PendingIntent.getBroadcast(
        context, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
    )
    val triggerTime = System.currentTimeMillis() + 5_000
    try {
        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        android.widget.Toast.makeText(context, "Start za 5s (Interwał: ${intervalMinutes}m)", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {}
}