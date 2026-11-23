package ru.school9d.schedule.widget

import ru.school9d.schedule.data.Consultation
import ru.school9d.schedule.data.DayItem
import ru.school9d.schedule.data.DaySchedule
import ru.school9d.schedule.data.ItemKind
import ru.school9d.schedule.data.ScheduleMerger
import ru.school9d.schedule.util.formatTime
import ru.school9d.schedule.util.formatTimeRange
import ru.school9d.schedule.util.parseTimeOrNull
import ru.school9d.schedule.util.toShortRu
import ru.school9d.schedule.util.weekdayNameRu
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

data class WidgetLesson(
    val title: String,
    val timeStart: String,
    val timeEnd: String,
    val timeRange: String,
    val room: String?,
    val badge: String,
    val isConsultation: Boolean,
    val isCurrent: Boolean,
    val isPast: Boolean
)

data class WidgetFooter(
    val text: String,
    val compactText: String,
    val isConsultation: Boolean
)

data class WidgetUiData(
    val status: String,
    val dayTitle: String,
    val highlighted: WidgetLesson?,
    val upcoming: List<WidgetLesson>,
    val allItems: List<WidgetLesson>,
    val footer: WidgetFooter,
    val emptyMessage: String
)

object WidgetDataFactory {

    private val WEEKDAYS_SHORT = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    fun build(
        today: DaySchedule,
        tomorrow: DaySchedule,
        consultations: List<Consultation>,
        now: LocalDateTime
    ): WidgetUiData {
        val nowTime = now.toLocalTime()
        val todayItems = ScheduleMerger.chronological(today.items)
        val current = todayItems.firstOrNull { it.isActive(nowTime) }
        val upcomingToday = todayItems.filter { it.isUpcoming(nowTime) }
        val hasRemainingToday = current != null || upcomingToday.isNotEmpty()
        val showTomorrow = !hasRemainingToday &&
            tomorrow.isLoaded &&
            tomorrow.items.isNotEmpty()

        val displayed = if (showTomorrow) tomorrow else today
        val displayedItems = ScheduleMerger.chronological(displayed.items)
        val isToday = !showTomorrow && displayed.date == today.date

        val items = displayedItems.map { item ->
            item.toWidgetLesson(nowTime, isToday)
        }
        val highlightedIndex = when {
            isToday && current != null -> displayedItems.indexOf(current).coerceAtLeast(0)
            else -> items.indexOfFirst { !it.isPast }.takeIf { it >= 0 } ?: items.lastIndex
        }
        val highlighted = items.getOrNull(highlightedIndex)
        val upcoming = if (highlightedIndex >= 0 && highlightedIndex < items.lastIndex) {
            items.drop(highlightedIndex + 1).take(3)
        } else {
            emptyList()
        }

        val status = when {
            current != null -> if (current.kind == ItemKind.CONSULTATION) {
                "ИДЕТ КОНСУЛЬТАЦИЯ"
            } else {
                "СЕЙЧАС ИДЕТ"
            }
            showTomorrow -> "ЗАВТРА"
            isToday && today.items.isEmpty() && !today.isLoaded -> "НЕТ РАСПИСАНИЯ"
            isToday && upcomingToday.isNotEmpty() -> {
                if (todayItems.any { it.isPast(nowTime) }) "ПЕРЕМЕНА" else "СЛЕДУЮЩИЙ УРОК"
            }
            isToday && displayed.items.isNotEmpty() -> "УРОКИ ЗАКОНЧИЛИСЬ"
            displayed.items.isNotEmpty() -> "РАСПИСАНИЕ"
            else -> "СЕГОДНЯ БЕЗ УРОКОВ"
        }

        val emptyMessage = when {
            displayed.items.isNotEmpty() -> ""
            !displayed.isLoaded -> "Расписание не загружено"
            else -> "Уроков нет"
        }

        val weekday = weekdayNameRu(displayed.date.dayOfWeek.value)
            .uppercase(Locale.ROOT)
            .take(2)
        val dayTitle = "$weekday, ${displayed.date.toShortRu().uppercase(Locale.ROOT)}"

        return WidgetUiData(
            status = status,
            dayTitle = dayTitle,
            highlighted = highlighted,
            upcoming = upcoming,
            allItems = items,
            footer = buildFooter(
                consultations = consultations,
                displayDate = displayed.date,
                isDisplayedToday = isToday,
                now = nowTime
            ),
            emptyMessage = emptyMessage
        )
    }

    private fun buildFooter(
        consultations: List<Consultation>,
        displayDate: LocalDate,
        isDisplayedToday: Boolean,
        now: LocalTime
    ): WidgetFooter {
        val enabled = consultations.filter { it.enabled }
        if (enabled.isEmpty()) {
            return WidgetFooter("Консультаций нет", "Консультаций нет", false)
        }

        val dayOfWeek = displayDate.dayOfWeek.value
        val dayConsultations = enabled
            .filter {
                it.dayOfWeek == dayOfWeek &&
                    parseTimeOrNull(it.start) != null &&
                    parseTimeOrNull(it.end) != null
            }
            .sortedBy { parseTimeOrNull(it.start) }

        if (isDisplayedToday) {
            val ongoing = dayConsultations.firstOrNull {
                val start = parseTimeOrNull(it.start) ?: return@firstOrNull false
                val end = parseTimeOrNull(it.end) ?: return@firstOrNull false
                !now.isBefore(start) && now.isBefore(end)
            }
            if (ongoing != null) {
                return WidgetFooter(
                    text = "Идет консультация до ${ongoing.end}",
                    compactText = "Конс. до ${ongoing.end}",
                    isConsultation = true
                )
            }
        }

        val upcoming = dayConsultations.filter {
            if (!isDisplayedToday) {
                true
            } else {
                val end = parseTimeOrNull(it.end) ?: LocalTime.MIN
                end.isAfter(now)
            }
        }

        if (upcoming.isNotEmpty()) {
            val shown = upcoming.take(2)
            val suffix = if (upcoming.size > 2) " +${upcoming.size - 2}" else ""
            val joined = shown.joinToString(" • ") {
                "${shortTitle(it.title)} ${it.start}"
            }
            val full = if (shown.size == 1) {
                "Консультация: ${shortTitle(shown[0].title)} • ${shown[0].start}$suffix"
            } else {
                "Конс: $joined$suffix"
            }
            val compact = if (shown.size == 1) {
                "Конс: ${shortTitle(shown[0].title)} ${shown[0].start}$suffix"
            } else {
                "Конс: $joined$suffix"
            }
            return WidgetFooter(full, compact, true)
        }

        val next = nextConsultation(enabled, displayDate)
            ?: return WidgetFooter("Консультаций нет", "Консультаций нет", false)
        val weekday = WEEKDAYS_SHORT.getOrElse(next.first.dayOfWeek.value - 1) { "" }
        return WidgetFooter(
            text = "След. конс: $weekday • ${shortTitle(next.second.title)} ${next.second.start}",
            compactText = "След. конс: $weekday ${next.second.start}",
            isConsultation = true
        )
    }

    private fun nextConsultation(
        enabled: List<Consultation>,
        from: LocalDate
    ): Pair<LocalDate, Consultation>? {
        for (offset in 1..7) {
            val date = from.plusDays(offset.toLong())
            val candidate = enabled
                .filter { it.dayOfWeek == date.dayOfWeek.value }
                .minByOrNull { parseTimeOrNull(it.start) ?: LocalTime.MAX }
            if (candidate != null) return date to candidate
        }
        return null
    }

    private fun shortTitle(raw: String): String {
        val title = raw.substringBefore('(').trim()
        val lower = title.lowercase(Locale.ROOT)
        return when {
            lower.startsWith("математик") -> "Мат"
            lower.startsWith("информатик") -> "Инф"
            lower.startsWith("русск") -> "Русский"
            lower.startsWith("географ") -> "География"
            lower.startsWith("физик") -> "Физика"
            lower.startsWith("хими") -> "Химия"
            lower.startsWith("биолог") -> "Биология"
            lower.startsWith("истор") -> "История"
            lower.startsWith("обществ") -> "Общество"
            lower.startsWith("англ") -> "Англ"
            lower.startsWith("литератур") -> "Литература"
            else -> title.take(12).ifBlank { "Конс." }
        }
    }

    private fun DayItem.toWidgetLesson(now: LocalTime, isToday: Boolean): WidgetLesson {
        val badge = when {
            kind == ItemKind.CONSULTATION -> "КОНС • ${timeRangeOrFallback()}"
            number != null -> "$number-Й УРОК • ${timeRangeOrFallback()}"
            else -> "УРОК • ${timeRangeOrFallback()}"
        }
        return WidgetLesson(
            title = title,
            timeStart = formatTime(start),
            timeEnd = formatTime(end),
            timeRange = formatTimeRange(start, end),
            room = room?.takeIf { it.isNotBlank() },
            badge = badge,
            isConsultation = kind == ItemKind.CONSULTATION,
            isCurrent = isToday && isActive(now),
            isPast = isToday && isPast(now)
        )
    }

    private fun DayItem.timeRangeOrFallback(): String =
        formatTimeRange(start, end).takeIf { it != "—" } ?: "—"

    private fun DayItem.isActive(now: LocalTime): Boolean {
        val start = start ?: return false
        val end = end ?: return false
        return !now.isBefore(start) && now.isBefore(end)
    }

    private fun DayItem.isUpcoming(now: LocalTime): Boolean =
        start?.isAfter(now) == true

    private fun DayItem.isPast(now: LocalTime): Boolean =
        end?.let { !it.isAfter(now) } == true
}
