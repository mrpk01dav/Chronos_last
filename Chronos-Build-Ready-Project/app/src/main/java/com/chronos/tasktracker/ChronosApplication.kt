package com.chronos.tasktracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

class ChronosApplication : Application() {

    companion object {
        const val REMINDER_CHANNEL_ID = "chronos_reminders_channel"
        const val PERSISTENT_CHANNEL_ID = "chronos_persistent_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // High-importance task reminder channel with sound and vibration
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Task Reminders & Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent task alarms, deadline notifications and snoozed alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 300)
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(defaultSoundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Low-importance channel for optional persistent "Today's Tasks" summary
            val persistentChannel = NotificationChannel(
                PERSISTENT_CHANNEL_ID,
                "Today's Overview",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing summary of daily tasks and pending counts"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(persistentChannel)
        }
    }
}
