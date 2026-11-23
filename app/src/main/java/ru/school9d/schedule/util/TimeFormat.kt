package ru.school9d.schedule.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RU_LOCALE: Locale = Locale.forLanguageTag("ru-RU")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val SHORT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", RU_LOCALE)
private val FULL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", RU_LOCALE)
private val NUMERIC_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private val WEEKDAYS = listOf(
    "",
    "Понедельник",
    "Вторник",
    "Среда",
    "Четверг",
    "Пятница",
    "Суббота",
    "Воскресенье"
)

fun LocalTime.toHhMm(): String = format(TIME_FORMATTER)

fun formatTime(time: LocalTime?): String = time?.toHhMm() ?: "—"

fun formatTimeRange(start: LocalTime?, end: LocalTime?): String =
    if (start != null && end != null) "${start.toHhMm()}–${end.toHhMm()}" else "—"

fun LocalDate.toShortRu(): String = format(SHORT_DATE_FORMATTER)

fun LocalDate.toFullRu(): String = format(FULL_DATE_FORMATTER)

fun LocalDate.toNumericRu(): String = format(NUMERIC_DATE_FORMATTER)

fun LocalDate.toWeekdayRu(): String = WEEKDAYS.getOrElse(dayOfWeek.value) { "" }

fun weekdayNameRu(dayOfWeek: Int): String = WEEKDAYS.getOrElse(dayOfWeek) { "" }

fun parseTimeOrNull(raw: String): LocalTime? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    return runCatching { LocalTime.parse(value, TIME_FORMATTER) }.getOrNull()
        ?: runCatching { LocalTime.parse(value, DateTimeFormatter.ofPattern("H:mm")) }.getOrNull()
}

fun normalizeTimeOrNull(raw: String): String? = parseTimeOrNull(raw)?.toHhMm()
