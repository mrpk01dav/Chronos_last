package com.chronos.tasktracker.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.chronos.tasktracker.ChronosApplication
import com.chronos.tasktracker.MainActivity
import com.chronos.tasktracker.R
import com.chronos.tasktracker.data.AppDatabase
import com.chronos.tasktracker.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_REMINDER = "com.chronos.ACTION_TASK_REMINDER"
        const val ACTION_TASK_COMPLETE = "com.chronos.ACTION_TASK_COMPLETE"
        const val ACTION_TASK_SNOOZE = "com.chronos.ACTION_TASK_SNOOZE"

        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
        const val EXTRA_TASK_PRIORITY = "EXTRA_TASK_PRIORITY"
        const val EXTRA_TASK_DUE_TIME = "EXTRA_TASK_DUE_TIME"
        const val EXTRA_SNOOZE_MINUTES = "EXTRA_SNOOZE_MINUTES"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return

        when (intent.action) {
            ACTION_TASK_REMINDER -> {
                val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
                val priority = intent.getStringExtra(EXTRA_TASK_PRIORITY) ?: "medium"
                val dueTime = intent.getStringExtra(EXTRA_TASK_DUE_TIME) ?: ""

                showNotification(context, taskId, title, priority, dueTime)
            }
            ACTION_TASK_COMPLETE -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(taskId.hashCode())

                // Mark task complete in Room database
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getInstance(context)
                    db.taskDao().markTaskComplete(taskId)
                }
            }
            ACTION_TASK_SNOOZE -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(taskId.hashCode())

                val snoozeMins = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Snoozed Task"

                // Reschedule for +snoozeMins from now
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_TASK_REMINDER
                    putExtra(EXTRA_TASK_ID, taskId)
                    putExtra(EXTRA_TASK_TITLE, title)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId.hashCode(),
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val triggerAt = System.currentTimeMillis() + snoozeMins * 60 * 1000L
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    private fun showNotification(
        context: Context,
        taskId: String,
        title: String,
        priority: String,
        dueTime: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Content intent: open app and focus task
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("OPEN_TASK_ID", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Complete Task
        val completeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TASK_COMPLETE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + "_complete").hashCode(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 10 Minutes
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TASK_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_SNOOZE_MINUTES, 10)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + "_snooze").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ChronosApplication.REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Priority: ${priority.uppercase()} · Due ${if (dueTime.isNotEmpty()) dueTime else "today"}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Complete", completePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 10m", snoozePendingIntent)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }
}
