package com.patrykadamski.waterreminder

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun WaterReminderScreen(viewModel: WaterViewModel) {
    val context = LocalContext.current

    // 1. Pobieramy dane z Managera
    val waterIntake = viewModel.waterIntake
    val dailyGoal = viewModel.dailyGoal
    val historyRecords = viewModel.records
    // Pobieramy interwał (częstotliwość) z ViewModelu
    val currentInterval = viewModel.alertInterval

    // 2. Stan: Czy pokazać okienko zmiany celu?
    var showDialog by remember { mutableStateOf(false) }

    val progress = (waterIntake.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // --- NAGŁÓWEK: Cel + Ustawienia ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Cel: $dailyGoal ml", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- LICZNIK (Kółko) ---
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF2196F3),
                strokeWidth = 12.dp,
                trackColor = Color.LightGray.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$waterIntake", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text("ml", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- PRZYCISKI DODAWANIA WODY ---
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { viewModel.addWater(250) }) { Text("+250 ml") }
            Button(onClick = { viewModel.addWater(500) }) { Text("+500 ml") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- PRZYCISKI RESET I TEST ---
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = { viewModel.resetWater() }) { Text("Reset") }
            // Przycisk uruchamia pętlę powiadomień
            TextButton(onClick = { scheduleNotification(context) }) { Text("Start Powiadomień") }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Divider()
        Spacer(modifier = Modifier.height(10.dp))

        // --- HISTORIA ---
        Text(
            text = "Ostatnie 7 dni:",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(historyRecords) { record ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = record.date, fontWeight = FontWeight.Bold)
                        Text(text = "${record.amount} ml", color = Color(0xFF1976D2))
                    }
                }
            }
        }
    }

    // --- LOGIKA OKIENKA (DIALOG) ---
    if (showDialog) {
        EditGoalDialog(
            currentGoal = dailyGoal,
            currentInterval = currentInterval,
            onDismiss = { showDialog = false },
            onConfirm = { newGoal, newInterval ->
                // Zapisujemy nowe ustawienia w ViewModel
                viewModel.changeGoal(newGoal)
                viewModel.changeInterval(newInterval)
                showDialog = false

                // Opcjonalnie: Restartujemy harmonogram z nowym czasem
                scheduleNotification(context)
            }
        )
    }
}

// --- FUNKCJA RYSUJĄCA OKIENKO ---
@Composable
fun EditGoalDialog(
    currentGoal: Int,
    currentInterval: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    var intervalText by remember { mutableStateOf(currentInterval.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ustawienia") },
        text = {
            Column {
                Text("Dzienny cel (ml):")
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Przypomnienie co (minuty):")
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { intervalText = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    placeholder = { Text("np. 60") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val newGoal = goalText.toIntOrNull() ?: 2000
                val newInterval = intervalText.toIntOrNull() ?: 60
                onConfirm(newGoal, newInterval)
            }) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

// --- FUNKCJA STARTUJĄCA POWIADOMIENIA ---
fun scheduleNotification(context: Context) {
    val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
    val intervalMinutes = prefs.getInt("alert_interval", 60)

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val intent = android.content.Intent(context, ReminderReceiver::class.java)
    val pendingIntent = android.app.PendingIntent.getBroadcast(
        context, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
    )

    // Ustawiamy start za 5 sekund
    // a potem ReminderReceiver sam ustawi kolejne za 'intervalMinutes'
    val triggerTime = System.currentTimeMillis() + 5_000

    try {
        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        android.widget.Toast.makeText(context, "Start za 5s (Pętla co $intervalMinutes min)", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: SecurityException) {
        android.widget.Toast.makeText(context, "Brak uprawnień!", android.widget.Toast.LENGTH_SHORT).show()
    }
}