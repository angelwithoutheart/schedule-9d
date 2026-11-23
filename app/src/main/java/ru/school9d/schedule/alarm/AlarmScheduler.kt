package ru.school9d.schedule.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.school9d.schedule.data.ScheduleMerger
import ru.school9d.schedule.data.ScheduleRepository
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {

    const val ACTION_ALARM = "ru.school9d.schedule.ACTION_ALARM"

    private const val PREFS_NAME = "schedule_9d_alarms"
    private const val KEY_TIMES = "scheduled_alarm_times"

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun reschedule(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching { rescheduleSuspend(appContext) }
        }
    }

    suspend fun rescheduleSuspend(context: Context) {
        val appContext = context.applicationContext
        mutex.withLock {
            val repository = ScheduleRepository.get(appContext)
            repository.ensureDefaults()

            val consultations = repository.consultations()
            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.now()
            val today = now.toLocalDate()
            val triggerTimes = mutableSetOf<Long>()

            for (date in listOf(today, today.plusDays(1))) {
                val saved = repository.schedule(date)
                val day = ScheduleMerger.merge(date, saved, consultations)
                day.items.forEach { item ->
                    item.start?.let { time ->
                        triggerTimes += date.atTime(time).atZone(zone).toInstant().toEpochMilli()
                    }
                    item.end?.let { time ->
                        triggerTimes += date.atTime(time).atZone(zone).toInstant().toEpochMilli()
                    }
                }
            }

            triggerTimes += today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val nowMillis = System.currentTimeMillis()
            val future = triggerTimes
                .filter { it > nowMillis + 100L }
                .sorted()

            scheduleAll(appContext, future)
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    private fun scheduleAll(context: Context, triggerTimes: List<Long>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previouslyScheduled = prefs.getStringSet(KEY_TIMES, emptySet()).orEmpty()

        previouslyScheduled
            .mapNotNull { it.toLongOrNull() }
            .forEach { triggerAt -> cancel(context, alarmManager, triggerAt) }

        val scheduled = mutableSetOf<String>()
        triggerTimes.forEach { triggerAt ->
            val operation = pendingIntent(context, triggerAt, PendingIntent.FLAG_UPDATE_CURRENT)
                ?: return@forEach
            try {
                if (canScheduleExactAlarms(context)) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        operation
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        operation
                    )
                }
                scheduled += triggerAt.toString()
            } catch (_: SecurityException) {
                runCatching {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        operation
                    )
                    scheduled += triggerAt.toString()
                }
            }
        }

        prefs.edit().putStringSet(KEY_TIMES, scheduled).apply()
    }

    private fun cancel(context: Context, alarmManager: AlarmManager, triggerAt: Long) {
        val operation = pendingIntent(context, triggerAt, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(operation)
        operation.cancel()
    }

    private fun pendingIntent(context: Context, triggerAt: Long, flags: Int): PendingIntent? {
        val alarmIntent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_ALARM)
            .setData(Uri.parse("school9d://alarm/$triggerAt"))
        val requestCode = (triggerAt / 60_000L).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            alarmIntent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
