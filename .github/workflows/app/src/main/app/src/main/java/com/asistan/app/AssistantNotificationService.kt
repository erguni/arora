package com.asistan.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class AssistantNotificationService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.notification?.extras?.let { extras ->
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val pkg = sbn.packageName ?: ""

            // Mail veya mesaj geldiyse yerel hafızaya kaydet
            if (pkg.contains("android.gm") || pkg.contains("outlook") || pkg.contains("whatsapp")) {
                val prefs = getSharedPreferences("asistan_veriler", MODE_PRIVATE)
                val current = prefs.getString("bildirimler", "") ?: ""
                prefs.edit().putString("bildirimler", "$current\n[$title]: $text").apply()
            }
        }
    }
}
