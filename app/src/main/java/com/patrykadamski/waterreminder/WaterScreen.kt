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

    // 1. Pobieramy dane z Managera (teraz cel jest zmienny!)
    val waterIntake = viewModel.waterIntake
    val dailyGoal = viewModel.dailyGoal
    val historyRecords = viewModel.records

    // 2. Stan: Czy pokazać okienko zmiany celu? (Domyślnie false - ukryte)
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

        // --- ZMIANA: Wiersz z tekstem celu i ikonką ustawień ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Cel: $dailyGoal ml", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Zmień cel")
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- LICZNIK (Bez zmian) ---
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

        // --- PRZYCISKI (Bez zmian) ---
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { viewModel.addWater(250) }) { Text("+250 ml") }
            Button(onClick = { viewModel.addWater(500) }) { Text("+500 ml") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = { viewModel.resetWater() }) { Text("Reset") }
            TextButton(onClick = { scheduleNotification(context) }) { Text("Test Powiadomienia") }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Divider()
        Spacer(modifier = Modifier.height(10.dp))

        // --- HISTORIA (Bez zmian) ---
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

    // --- NOWOŚĆ: Logika wyświetlania okienka ---
    if (showDialog) {
        EditGoalDialog(
            currentGoal = dailyGoal,
            onDismiss = { showDialog = false },
            onConfirm = { newGoal ->
                viewModel.changeGoal(newGoal) // Zapisz nowy cel w Managerze
                showDialog = false // Zamknij okno
            }
        )
    }
}

// Pomocnicza funkcja rysująca okienko
@Composable
fun EditGoalDialog(currentGoal: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(currentGoal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ustaw cel dzienny") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { char -> char.isDigit() } }, // Pozwól wpisać tylko cyfry
                label = { Text("Mililitry") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                val number = text.toIntOrNull() ?: 2000
                onConfirm(number)
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

// Funkcja powiadomień zostaje bez zmian, ale musi tu być, żeby kod się kompilował
fun scheduleNotification(context: Context) {
    // Tę funkcję powinieneś już mieć w tym pliku na samym dole - zostaw ją tak jak była,
    // albo wklej z poprzednich kroków, jeśli ją usunąłeś.
    // Dla porządku przypominam, że ona tu jest potrzebna :)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val intent = android.content.Intent(context, ReminderReceiver::class.java)
    val pendingIntent = android.app.PendingIntent.getBroadcast(
        context, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
    )
    val triggerTime = System.currentTimeMillis() + 10_000
    try {
        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        android.widget.Toast.makeText(context, "Przypomnienie ustawione!", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        android.widget.Toast.makeText(context, "Brak uprawnień!", android.widget.Toast.LENGTH_SHORT).show()
    }
}