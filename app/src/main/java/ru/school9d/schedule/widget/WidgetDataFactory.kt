package ru.school9d.schedule.widget

import ru.school9d.schedule.data.DayItem
import ru.school9d.schedule.data.DaySchedule
import ru.school9d.schedule.data.ItemKind
import ru.school9d.schedule.data.ScheduleMerger
import ru.school9d.schedule.util.formatTimeRange
import java.time.LocalTime

data class WidgetUiData(
    val status: String,
    val badge: String,
    val subject: String,
    val roomLabel: String?
)

object WidgetDataFactory {

    fun build(today: DaySchedule, tomorrow: DaySchedule, now: LocalTime): WidgetUiData {
        val todayItems = ScheduleMerger.chronological(today.items)

        val current = todayItems.firstOrNull { item ->
            val start = item.start ?: return@firstOrNull false
            val end = item.end ?: return@firstOrNull false
            !now.isBefore(start) && now.isBefore(end)
        }
        if (current != null) {
            return itemData("СЕЙЧАС ИДЕТ", current)
        }

        val upcoming = todayItems.firstOrNull { item ->
            val start = item.start ?: return@firstOrNull false
            start.isAfter(now)
        }
        if (upcoming != null) {
            val hasFinishedSomething = todayItems.any { item ->
                val end = item.end ?: return@any false
                !end.isAfter(now)
            }
            val status = if (hasFinishedSomething) "ПЕРЕМЕНА" else "СЛЕДУЮЩИЙ УРОК"
            return itemData(status, upcoming)
        }

        if (tomorrow.isLoaded) {
            val firstTomorrow = ScheduleMerger.chronological(tomorrow.items).firstOrNull()
            if (firstTomorrow != null) {
                return itemData("ЗАВТРА", firstTomorrow)
            }
        }

        if (today.items.isNotEmpty()) {
            return WidgetUiData(
                status = "СЕГОДНЯ ВСЁ",
                badge = "ЗАВТРА НЕ ЗАГРУЖЕНО",
                subject = "Уроки закончились",
                roomLabel = null
            )
        }

        return WidgetUiData(
            status = "НЕТ РАСПИСАНИЯ",
            badge = "ОТКРОЙТЕ ПРИЛОЖЕНИЕ",
            subject = "Загрузите фото расписания",
            roomLabel = null
        )
    }

    private fun itemData(status: String, item: DayItem): WidgetUiData {
        val timeRange = formatTimeRange(item.start, item.end)
        val badge = when {
            item.kind == ItemKind.CONSULTATION -> "КОНСУЛЬТАЦИЯ • $timeRange"
            item.number != null && item.number > 0 -> "${item.number}-Й УРОК • $timeRange"
            else -> "УРОК • $timeRange"
        }
        val roomLabel = when {
            !item.room.isNullOrBlank() -> "Кабинет ${item.room}"
            item.kind == ItemKind.CONSULTATION -> "Кабинет не указан"
            else -> null
        }
        return WidgetUiData(
            status = status,
            badge = badge,
            subject = item.title,
            roomLabel = roomLabel
        )
    }
}
