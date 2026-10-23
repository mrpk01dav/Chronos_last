package com.chronos.tasktracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chronos.tasktracker.data.AppDatabase
import com.chronos.tasktracker.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val pendingTasks = db.taskDao().getPendingTasks()
                    val scheduler = AlarmScheduler(context)

                    for (task in pendingTasks) {
                        scheduler.scheduleTaskReminder(task)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
