package ru.school9d.schedule

import android.app.Application
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.school9d.schedule.alarm.AlarmScheduler
import ru.school9d.schedule.data.ScheduleRepository
import ru.school9d.schedule.widget.ScheduleWidget

class ScheduleApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val context = applicationContext
        appScope.launch {
            runCatching { ScheduleRepository.get(context).ensureDefaults() }
            runCatching { ScheduleWidget().updateAll(context) }
            runCatching { AlarmScheduler.rescheduleSuspend(context) }
        }
    }
}
