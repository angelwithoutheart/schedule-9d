package ru.school9d.schedule.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.school9d.schedule.widget.ScheduleWidget

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                runCatching { ScheduleWidget().updateAll(appContext) }
                runCatching { AlarmScheduler.rescheduleSuspend(appContext) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
