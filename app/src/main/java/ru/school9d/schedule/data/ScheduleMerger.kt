package ru.school9d.schedule.data

import ru.school9d.schedule.util.parseTimeOrNull
import java.time.LocalDate
import java.time.LocalTime

object ScheduleMerger {

    fun merge(
        date: LocalDate,
        saved: SavedSchedule?,
        consultations: List<Consultation>
    ): DaySchedule {
        val dayOfWeek = date.dayOfWeek.value
        val bells = BellSchedule.forDay(dayOfWeek)

        val lessonItems = saved?.lessons
            .orEmpty()
            .sortedBy { it.number }
            .map { lesson ->
                val bell = bells.firstOrNull { it.number == lesson.number }
                DayItem(
                    kind = ItemKind.LESSON,
                    number = lesson.number.takeIf { it > 0 },
                    title = lesson.subject.trim().ifBlank { "Урок ${lesson.number}" },
                    room = lesson.room.trim().takeIf { it.isNotBlank() },
                    start = bell?.start,
                    end = bell?.end,
                    typeLabel = if (lesson.number > 0) "${lesson.number}-Й УРОК" else "УРОК"
                )
            }

        val consultationItems = consultations
            .filter { it.dayOfWeek == dayOfWeek && it.enabled }
            .sortedBy { parseTimeOrNull(it.start) ?: LocalTime.MAX }
            .map { consultation ->
                DayItem(
                    kind = ItemKind.CONSULTATION,
                    number = null,
                    title = consultation.title.trim(),
                    room = consultation.room.trim().takeIf { it.isNotBlank() },
                    start = parseTimeOrNull(consultation.start),
                    end = parseTimeOrNull(consultation.end),
                    typeLabel = consultation.type.ifBlank { "КОНСУЛЬТАЦИЯ" }
                )
            }

        return DaySchedule(
            date = date,
            dayOfWeek = dayOfWeek,
            items = lessonItems + consultationItems,
            isLoaded = saved != null,
            recognizedAt = saved?.recognizedAt
        )
    }

    fun chronological(items: List<DayItem>): List<DayItem> =
        items.sortedWith(
            compareBy<DayItem> { it.start ?: LocalTime.MAX }
                .thenBy { it.end ?: LocalTime.MAX }
        )
}
