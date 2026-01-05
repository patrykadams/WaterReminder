package com.patrykadamski.waterreminder

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.random.Random

@Composable
fun WaterReminderScreen(viewModel: WaterViewModel) {
    val context = LocalContext.current

    // Usunęliśmy lifecycleOwner, bo teraz ViewModel sam nasłuchuje bazy danych (Flow)!

    val waterIntake = viewModel.waterIntake
    val dailyGoal = viewModel.dailyGoal
    val historyRecords = viewModel.records
    val currentInterval = viewModel.alertInterval
    val currentWeight = viewModel.userWeight
    val currentQuickAdd = viewModel.quickAddAmount
    val currentWakeUp = viewModel.wakeUpHour
    val currentSleep = viewModel.sleepHour

    val streakDays = viewModel.streakDays
    val showConfetti = viewModel.showConfetti

    var showDialog by remember { mutableStateOf(false) }

    // NOWOŚĆ: Stan dla dialogu potwierdzenia resetu
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val progress = (waterIntake.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔥", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$streakDays dni",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (streakDays > 0) Color(0xFFFF5722) else Color.Gray
                    )
                }
                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Cel: $dailyGoal ml", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(20.dp))

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

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { viewModel.addWater(currentQuickAdd) }) { Text("+$currentQuickAdd ml") }
                Button(onClick = { viewModel.addWater(500) }) { Text("+500 ml") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // ZMIANA: Kliknięcie Resetu otwiera dialog
                TextButton(onClick = { showResetConfirmDialog = true }) {
                    Text("Reset", color = Color.Red.copy(alpha = 0.7f))
                }
                TextButton(onClick = { scheduleNotification(context) }) { Text("Test Powiadomienia") }
            }

            Spacer(modifier = Modifier.height(15.dp))
            Divider()
            Spacer(modifier = Modifier.height(10.dp))

            Text("Statystyki (7 dni):", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(15.dp))

            // Rysujemy nowy, czytelniejszy wykres
            WeekBarChart(data = historyRecords.asReversed())
        }

        if (showConfetti) {
            ConfettiAnimation()
        }
    }

    // --- DIALOG POTWIERDZENIA RESETU ---
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Zresetować wodę?") },
            text = { Text("Czy na pewno chcesz wyzerować dzisiejszy licznik? Tego nie można cofnąć.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetWater()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Tak, resetuj")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

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

// --- ULEPSZONY WYKRES SŁUPKOWY ---
@Composable
fun WeekBarChart(data: List<WaterEntity>) {
    val today = LocalDate.now()
    // Generujemy ostatnie 7 dni
    val last7Days = (0..6).map { today.minusDays(it.toLong()) }.reversed()

    val chartData = last7Days.map { date ->
        val dateString = date.toString()
        val entry = data.find { it.date == dateString }
        val amount = entry?.amount ?: 0
        date to amount
    }

    // Skalowanie
    val maxInWeek = chartData.maxOfOrNull { it.second } ?: 2000
    val maxAmount = if (maxInWeek > 0) maxInWeek else 2000
    // Zwiększamy wysokość wykresu, żeby zmieściły się napisy
    val chartHeight = 160.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        chartData.forEach { (date, amount) ->
            val percentage = (amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
            val animatedBarHeight by animateDpAsState(
                targetValue = if (amount > 0) (chartHeight - 30.dp) * percentage else 4.dp,
                label = "barHeight"
            )

            // Sprawdzamy czy to dzisiaj (żeby wyróżnić kolor)
            val isToday = date == today

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // --- NOWOŚĆ: Liczba nad słupkiem ---
                if (amount > 0) {
                    Text(
                        text = if (amount > 1000) "${amount/1000}k" else "$amount", // Skracamy np. 2500 -> 2.5k jeśli ciasno, albo wyświetlamy całość
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = Color.Black,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Słupek
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(animatedBarHeight)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            if (isToday) Color(0xFF1565C0) // Ciemniejszy niebieski dla DZIŚ
                            else if (amount > 0) Color(0xFF2196F3)
                            else Color.LightGray.copy(alpha = 0.3f)
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Dzień tygodnia
                Text(
                    text = getDayLabel(date.toString()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) Color.Black else Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

// --- Reszta (Animacja, Dialog Ustawień, Powiadomienia) bez zmian ---

@Composable
fun ConfettiAnimation() {
    val particles = remember {
        List(50) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -1f,
                speed = Random.nextFloat() * 10f + 10f,
                color = Color(Random.nextLong(0xFFFFFFFF))
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "time"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        particles.forEach { particle ->
            val currentY = (particle.y + time * particle.speed) * height
            val currentX = (particle.x * width) + (Math.sin(time * 10.0 + particle.y) * 50.0).toFloat()
            if (currentY < height) drawCircle(color = particle.color, radius = 15f, center = Offset(currentX, currentY))
        }
    }
}
data class ConfettiParticle(val x: Float, val y: Float, val speed: Float, val color: Color)

fun getDayLabel(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        dayName.uppercase().take(2)
    } catch (e: Exception) { "?" }
}

@Composable
fun EditGoalDialog(
    currentGoal: Int, currentInterval: Int, currentWeight: Int, currentQuickAdd: Int, currentWakeUp: Int, currentSleep: Int,
    onDismiss: () -> Unit, onConfirm: (Int, Int, Int, Int, Int, Int) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    var intervalText by remember { mutableStateOf(currentInterval.toString()) }
    var weightText by remember { mutableStateOf(currentWeight.toString()) }
    var quickAddText by remember { mutableStateOf(currentQuickAdd.toString()) }
    var wakeUpText by remember { mutableStateOf(currentWakeUp.toString()) }
    var sleepText by remember { mutableStateOf(currentSleep.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Ustawienia") },
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
                        label = { Text("Waga (kg)") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it.filter { c -> c.isDigit() } },
                        label = { Text("Cel (ml)") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { intervalText = it.filter { c -> c.isDigit() } },
                    label = { Text("Co ile minut?") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = wakeUpText,
                        onValueChange = { wakeUpText = it.filter { c -> c.isDigit() } },
                        label = { Text("Start") }, placeholder = { Text("8") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = sleepText,
                        onValueChange = { sleepText = it.filter { c -> c.isDigit() } },
                        label = { Text("Koniec") }, placeholder = { Text("22") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quickAddText,
                    onValueChange = { quickAddText = it.filter { c -> c.isDigit() } },
                    label = { Text("Przycisk +ml") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(goalText.toIntOrNull() ?: 2000, intervalText.toIntOrNull() ?: 60, weightText.toIntOrNull() ?: 70, quickAddText.toIntOrNull() ?: 250, wakeUpText.toIntOrNull() ?: 8, sleepText.toIntOrNull() ?: 22) }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

fun scheduleNotification(context: Context) {
    val prefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
    val intervalMinutes = prefs.getInt("alert_interval", 60)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val intent = android.content.Intent(context, ReminderReceiver::class.java)
    val pendingIntent = android.app.PendingIntent.getBroadcast(context, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
    val triggerTime = System.currentTimeMillis() + 5_000
    try {
        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        android.widget.Toast.makeText(context, "Start za 5s (Interwał: ${intervalMinutes}m)", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {}
}