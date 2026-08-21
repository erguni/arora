package com.asistan.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CallLog
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // İzinleri iste
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_SMS,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AssistantScreen(
                        onCheckAlarm = { checkAlarmStatus() },
                        onCheckCalls = { getUnreturnedCalls() },
                        onGetNotifications = { getStoredNotifications() },
                        onClearData = { clearAllData() },
                        onOpenNotificationSettings = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                }
            }
        }
    }

    private fun checkAlarmStatus(): String {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextAlarm = alarmManager.nextAlarmClock
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
        return if (nextAlarm != null) {
            "✅ Aktif alarmınız kurulu."
        } else if (isWeekday) {
            "⚠️ DİKKAT: Hafta içindesiniz fakat yarın için kurulmuş bir alarm bulunamadı!"
        } else {
            "Hafta sonu, kurulu alarm yok."
        }
    }

    private fun getUnreturnedCalls(): List<String> {
        val unreturned = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return listOf("Arama günlüğü izni verilmedi.")
        }

        try {
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.CACHED_NAME),
                null, null, "${CallLog.Calls.DATE} DESC LIMIT 20"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                    val name = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)) ?: number

                    if (type == CallLog.Calls.MISSED_TYPE) {
                        unreturned.add("Cevapsız Arama: $name ($number)")
                    }
                }
            }
        } catch (e: Exception) {
            unreturned.add("Aramalar taranırken hata: ${e.message}")
        }
        return if (unreturned.isEmpty()) listOf("Dönüş yapılmamış cevapsız arama yok.") else unreturned
    }

    private fun getStoredNotifications(): String {
        val prefs = getSharedPreferences("asistan_veriler", MODE_PRIVATE)
        return prefs.getString("bildirimler", "Henüz yakalanan mail veya mesaj bildirimi yok.") ?: ""
    }

    private fun clearAllData() {
        val prefs = getSharedPreferences("asistan_veriler", MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

@Composable
fun AssistantScreen(
    onCheckAlarm: () -> String,
    onCheckCalls: () -> List<String>,
    onGetNotifications: () -> String,
    onClearData: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    var alarmStatus by remember { mutableStateOf("") }
    var callsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var notificationsText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        alarmStatus = onCheckAlarm()
        callsList = onCheckCalls()
        notificationsText = onGetNotifications()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("🤖 Kişisel Asistanınız", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("⏰ Alarm Durumu", style = MaterialTheme.typography.titleMedium)
                Text(alarmStatus)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("📞 Cevapsız / Dönüş Bekleyenler", style = MaterialTheme.typography.titleMedium)
                callsList.forEach { call ->
                    Text("• $call", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("✉️ Mail & Mesaj Bildirimleri", style = MaterialTheme.typography.titleMedium)
                Text(notificationsText, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onOpenNotificationSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Mail/Mesaj Dinleme İznini Aç")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                onClearData()
                notificationsText = "Tüm veriler başarıyla silindi."
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tüm Verileri Temizle (Sıfırla)")
        }
    }
}
