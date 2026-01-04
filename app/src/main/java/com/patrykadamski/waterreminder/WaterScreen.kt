package com.patrykadamski.waterreminder.com.patrykadamski.waterreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykadamski.waterreminder.ReminderReceiver
import com.patrykadamski.waterreminder.WaterViewModel

@Composable
fun WaterReminderScreen(viewModel: WaterViewModel) {
    val context = LocalContext.current

    // UI pobiera dane od Managera, a nie trzyma ich samo
    val waterIntake = viewModel.waterIntake
    val dailyGoal = 2000

    val progress = (waterIntake.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Cel dzienny: $dailyGoal ml", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(30.dp))

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

        Spacer(modifier = Modifier.height(40.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Przyciski zlecają pracę Managerowi
            Button(onClick = { viewModel.addWater(250) }) { Text("+250 ml") }
            Button(onClick = { viewModel.addWater(500) }) { Text("+500 ml") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { viewModel.resetWater() }) { Text("Resetuj licznik") }

        Spacer(modifier = Modifier.height(30.dp))
        Button(
            onClick = { scheduleNotification(context) },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text("Test powiadomienia (10s)")
        }
    }
}

fun scheduleNotification(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val triggerTime = System.currentTimeMillis() + 10_000
    try {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        Toast.makeText(context, "Przypomnienie ustawione!", Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Toast.makeText(context, "Brak uprawnień!", Toast.LENGTH_SHORT).show()
    }
}