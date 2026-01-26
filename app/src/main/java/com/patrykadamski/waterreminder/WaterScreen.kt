package com.patrykadamski.waterreminder

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.random.Random

// Kolory
val LightBlueBg = Color(0xFFE3F2FD)
val WaterColor = Color(0xFF2196F3)
val WaterGradientTop = Color(0xFF4FC3F7)
val WaterGradientBottom = Color(0xFF0288D1)
val DropletBorderColor = Color(0xFF01579B)
val EmptyDropletBg = Color(0xFFE1F5FE).copy(alpha = 0.5f)

@Composable
fun WaterReminderScreen(viewModel: WaterViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    val waterIntake = viewModel.waterIntake
    val dailyGoal = viewModel.dailyGoal
    val historyRecords = viewModel.records
    val currentInterval = viewModel.alertInterval
    val currentWeight = viewModel.userWeight
    val currentQuickAdd = viewModel.quickAddAmount
    val currentWakeUp = viewModel.wakeUpHour
    val currentSleep = viewModel.sleepHour
    val currentGender = viewModel.userGender
    // Pobieramy aktywność
    val currentActivity = viewModel.userActivity

    val streakDays = viewModel.streakDays
    val showConfetti = viewModel.showConfetti
    val lastAddedAmount = viewModel.lastAddedAmount

    val nextAlarmTime = viewModel.nextAlarmTime

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showCustomWaterDialog by remember { mutableStateOf(false) }

    val progress = (waterIntake.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing), label = "progress")

    val drankText = if (currentGender == "K") "Wypiłaś" else "Wypiłeś"

    var successMessage by remember { mutableStateOf("") }
    LaunchedEffect(waterIntake, dailyGoal, currentGender) {
        if (waterIntake >= dailyGoal) {
            if (currentGender == "K") {
                successMessage = "Jesteś Super! ❤️"
                delay(3000)
                successMessage = "Kocham Cię! ❤️"
            } else {
                successMessage = "Jesteś Super! 🔥"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(LightBlueBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔥", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "$streakDays dni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (streakDays > 0) Color(0xFFFF5722) else Color.Gray)
                }
                IconButton(onClick = { showSettingsDialog = true }) { Icon(Icons.Default.Settings, contentDescription = "Ustawienia") }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Cel: $dailyGoal ml", style = MaterialTheme.typography.titleMedium)

            if (waterIntake >= dailyGoal) {
                Text(
                    text = successMessage,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFE91E63),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(38.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                WaterDropletProgressBar(progress = animatedProgress, modifier = Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$waterIntake", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "ml ($drankText)", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (nextAlarmTime.isNotEmpty() && waterIntake < dailyGoal) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏰", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Następne: $nextAlarmTime",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { viewModel.addWater(currentQuickAdd) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = WaterColor)) { Text("+$currentQuickAdd ml") }
                OutlinedButton(onClick = { showCustomWaterDialog = true }, modifier = Modifier.weight(1f)) { Text("Inna...") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showResetConfirmDialog = true }) { Text("Reset", color = Color.Red.copy(alpha = 0.7f)) }
                if (lastAddedAmount > 0) {
                    TextButton(onClick = { viewModel.undoLastAdd() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cofnij")
                    }
                }
                TextButton(onClick = { scheduleNotification(context) }) { Text("Test") }
            }
            Spacer(modifier = Modifier.height(15.dp))
            Divider()
            Spacer(modifier = Modifier.height(10.dp))
            Text("Statystyki (7 dni):", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(15.dp))
            WeekBarChart(data = historyRecords.asReversed())
        }
        if (showConfetti) ConfettiAnimation()
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Zresetować wodę?") }, text = { Text("Czy na pewno chcesz wyzerować licznik?") },
            confirmButton = { Button(onClick = { viewModel.resetWater(); showResetConfirmDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Tak, resetuj") } },
            dismissButton = { TextButton(onClick = { showResetConfirmDialog = false }) { Text("Anuluj") } }
        )
    }

    if (showCustomWaterDialog) CustomWaterDialog(onDismiss = { showCustomWaterDialog = false }, onConfirm = { amount -> viewModel.addWater(amount); showCustomWaterDialog = false })

    if (showSettingsDialog) {
        EditGoalDialog(
            currentGoal = dailyGoal,
            currentWeight = currentWeight,
            currentQuickAdd = currentQuickAdd,
            currentWakeUp = currentWakeUp,
            currentSleep = currentSleep,
            currentGender = currentGender,
            currentActivity = currentActivity, // Przekazujemy aktywność
            onDismiss = { showSettingsDialog = false },
            onConfirm = { g, w, q, wake, sleep, gender, activity ->
                viewModel.saveSettings(g, w, q, wake, sleep, gender, activity)
                showSettingsDialog = false
                scheduleNotification(context)
            }
        )
    }
}

@Composable
fun WaterDropletProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val borderStrokeWidth = 3.dp.toPx()

        val dropletPath = Path().apply {
            moveTo(width / 2, 0f)
            cubicTo(width * 1.1f, height * 0.3f, width, height * 0.8f, width / 2, height)
            cubicTo(0f, height * 0.8f, -width * 0.1f, height * 0.3f, width / 2, 0f)
            close()
        }
        drawPath(path = dropletPath, color = EmptyDropletBg)
        val waterLevelY = height * (1 - progress)
        val waterBrush = Brush.verticalGradient(colors = listOf(WaterGradientTop, WaterGradientBottom), startY = waterLevelY, endY = height)
        clipPath(dropletPath) { drawRect(brush = waterBrush, topLeft = Offset(0f, waterLevelY), size = Size(width, height - waterLevelY)) }
        drawPath(path = dropletPath, color = DropletBorderColor, style = Stroke(width = borderStrokeWidth))
    }
}

// --- ZMODYFIKOWANY DIALOG USTAWIEŃ: Obsługa Aktywności ---
@Composable
fun EditGoalDialog(
    currentGoal: Int, currentWeight: Int, currentQuickAdd: Int,
    currentWakeUp: Int, currentSleep: Int, currentGender: String, currentActivity: String,
    onDismiss: () -> Unit, onConfirm: (Int, Int, Int, Int, Int, String, String) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    var weightText by remember { mutableStateOf(currentWeight.toString()) }
    var quickAddText by remember { mutableStateOf(currentQuickAdd.toString()) }
    var wakeUpText by remember { mutableStateOf(currentWakeUp.toString()) }
    var sleepText by remember { mutableStateOf(currentSleep.toString()) }
    var selectedGender by remember { mutableStateOf(currentGender) }
    var selectedActivity by remember { mutableStateOf(currentActivity) }

    // Funkcja obliczająca cel z uwzględnieniem aktywności
    fun calculateGoal(weight: Int, gender: String, activity: String): Int {
        val baseGoal = weight * (if (gender == "M") 35 else 31)
        val multiplier = when(activity) {
            "NONE" -> 1.0
            "LOW" -> 1.2
            "MEDIUM" -> 1.4
            "HIGH" -> 1.6
            else -> 1.0
        }
        return (baseGoal * multiplier).toInt()
    }

    // Pomocnicza funkcja do aktualizacji tekstu celu
    fun refreshGoalText() {
        val w = weightText.toIntOrNull()
        if (w != null && w > 0) {
            goalText = calculateGoal(w, selectedGender, selectedActivity).toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Ustawienia") },
        text = {
            Column(modifier = Modifier
                .padding(vertical = 8.dp)
                .verticalScroll(rememberScrollState()) // Dodano scroll, bo dialog robi się długi
            ) {
                // --- PŁEĆ ---
                Text("Płeć:", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedGender == "M", onClick = { selectedGender = "M"; refreshGoalText() })
                    Text("M")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = selectedGender == "K", onClick = { selectedGender = "K"; refreshGoalText() })
                    Text("K")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- AKTYWNOŚĆ FIZYCZNA ---
                Text("Aktywność fizyczna:", style = MaterialTheme.typography.labelLarge)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedActivity == "NONE", onClick = { selectedActivity = "NONE"; refreshGoalText() })
                        Text("Brak", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedActivity == "LOW", onClick = { selectedActivity = "LOW"; refreshGoalText() })
                        Text("< 3h (Lekka)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedActivity == "MEDIUM", onClick = { selectedActivity = "MEDIUM"; refreshGoalText() })
                        Text("3-6h (Średnia)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedActivity == "HIGH", onClick = { selectedActivity = "HIGH"; refreshGoalText() })
                        Text("+6h (Duża)", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- WAGA I CEL ---
                Row {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = {
                            val f = it.filter { c -> c.isDigit() }
                            weightText = f
                            val w = f.toIntOrNull()
                            if (w != null && w > 0) {
                                goalText = calculateGoal(w, selectedGender, selectedActivity).toString()
                            }
                        },
                        label = { Text("Waga (kg)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = goalText, onValueChange = { goalText = it.filter { c -> c.isDigit() } }, label = { Text("Cel (ml)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- CZAS SNU ---
                Row {
                    OutlinedTextField(value = wakeUpText, onValueChange = { wakeUpText = it.filter { c -> c.isDigit() } }, label = { Text("Start") }, placeholder = { Text("8") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = sleepText, onValueChange = { sleepText = it.filter { c -> c.isDigit() } }, label = { Text("Koniec") }, placeholder = { Text("22") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = quickAddText, onValueChange = { quickAddText = it.filter { c -> c.isDigit() } }, label = { Text("Porcja (Szklanka)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(goalText.toIntOrNull() ?: 2000, weightText.toIntOrNull() ?: 70, quickAddText.toIntOrNull() ?: 250, wakeUpText.toIntOrNull() ?: 8, sleepText.toIntOrNull() ?: 22, selectedGender, selectedActivity) }, colors = ButtonDefaults.buttonColors(containerColor = WaterColor)) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

// ... CustomWaterDialog, WeekBarChart, ConfettiAnimation ... (bez zmian)
@Composable
fun CustomWaterDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Ile wypiłeś?") }, text = { Column { Text("Wpisz ilość w ml:"); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = text, onValueChange = { text = it.filter { c -> c.isDigit() } }, placeholder = { Text("np. 330") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { val amount = text.toIntOrNull(); if (amount != null && amount > 0) onConfirm(amount) })) } }, confirmButton = { Button(onClick = { val amount = text.toIntOrNull(); if (amount != null && amount > 0) onConfirm(amount) }, colors = ButtonDefaults.buttonColors(containerColor = WaterColor)) { Text("Dodaj") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } })
}

@Composable
fun WeekBarChart(data: List<WaterEntity>) {
    val today = LocalDate.now(); val last7Days = (0..6).map { today.minusDays(it.toLong()) }.reversed()
    val chartData = last7Days.map { date -> val entry = data.find { it.date == date.toString() }; date to (entry?.amount ?: 0) }
    val maxInWeek = chartData.maxOfOrNull { it.second } ?: 2000; val maxAmount = if (maxInWeek > 0) maxInWeek else 2000; val chartHeight = 160.dp
    Row(modifier = Modifier.fillMaxWidth().height(chartHeight), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        chartData.forEach { (date, amount) ->
            val percentage = (amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
            val animatedBarHeight by animateDpAsState(targetValue = if (amount > 0) (chartHeight - 30.dp) * percentage else 4.dp, label = "barHeight")
            val isToday = date == today
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                if (amount > 0) Text(text = if (amount > 1000) "${amount/1000}k" else "$amount", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.Black, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.width(18.dp).height(animatedBarHeight).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(if (isToday) Color(0xFF1565C0) else if (amount > 0) WaterColor else Color.LightGray.copy(alpha = 0.3f)))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = getDayLabel(date.toString()), style = MaterialTheme.typography.labelSmall, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, color = if (isToday) Color.Black else Color.Gray, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}
@Composable
fun ConfettiAnimation() {
    val particles = remember { List(50) { ConfettiParticle(x = Random.nextFloat(), y = Random.nextFloat() * -1f, speed = Random.nextFloat() * 10f + 10f, color = Color(Random.nextLong(0xFFFFFFFF))) } }
    val infiniteTransition = rememberInfiniteTransition(label = "confetti"); val time by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "time")
    Canvas(modifier = Modifier.fillMaxSize()) { val width = size.width; val height = size.height; particles.forEach { p -> val cy = (p.y + time * p.speed) * height; val cx = (p.x * width) + (Math.sin(time * 10.0 + p.y) * 50.0).toFloat(); if (cy < height) drawCircle(color = p.color, radius = 15f, center = Offset(cx, cy)) } }
}
data class ConfettiParticle(val x: Float, val y: Float, val speed: Float, val color: Color)
fun getDayLabel(dateString: String): String { return try { val d = LocalDate.parse(dateString); d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase().take(2) } catch (e: Exception) { "?" } }
fun scheduleNotification(context: Context) { val i = Intent(context, ReminderReceiver::class.java); context.sendBroadcast(i) }