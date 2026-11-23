package ru.school9d.schedule.data

import java.time.LocalTime

object AppDefaults {
    const val DEFAULT_MODEL = "google/gemma-4-31b-it:free"
    const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
    val FALLBACK_MODELS = listOf(
        DEFAULT_MODEL,
        "google/gemma-4-26b-a4b-it:free",
        "openrouter/free"
    )
}

data class BellTime(
    val number: Int,
    val start: LocalTime,
    val end: LocalTime
)

object BellSchedule {
    val WEEKDAYS: List<BellTime> = listOf(
        BellTime(1, LocalTime.of(8, 0), LocalTime.of(8, 40)),
        BellTime(2, LocalTime.of(8, 50), LocalTime.of(9, 30)),
        BellTime(3, LocalTime.of(9, 40), LocalTime.of(10, 20)),
        BellTime(4, LocalTime.of(10, 35), LocalTime.of(11, 15)),
        BellTime(5, LocalTime.of(11, 30), LocalTime.of(12, 10)),
        BellTime(6, LocalTime.of(12, 20), LocalTime.of(13, 0)),
        BellTime(7, LocalTime.of(13, 10), LocalTime.of(13, 50))
    )

    val SATURDAY: List<BellTime> = listOf(
        BellTime(1, LocalTime.of(8, 0), LocalTime.of(8, 40)),
        BellTime(2, LocalTime.of(8, 45), LocalTime.of(9, 25)),
        BellTime(3, LocalTime.of(9, 30), LocalTime.of(10, 10)),
        BellTime(4, LocalTime.of(10, 25), LocalTime.of(11, 5)),
        BellTime(5, LocalTime.of(11, 10), LocalTime.of(11, 50)),
        BellTime(6, LocalTime.of(11, 55), LocalTime.of(12, 35)),
        BellTime(7, LocalTime.of(12, 40), LocalTime.of(13, 20))
    )

    fun forDay(dayOfWeek: Int): List<BellTime> = when (dayOfWeek) {
        6 -> SATURDAY
        in 1..5 -> WEEKDAYS
        else -> emptyList()
    }

    fun forLesson(dayOfWeek: Int, number: Int): BellTime? =
        forDay(dayOfWeek).firstOrNull { it.number == number }
}

object ConsultationsDefaults {
    val all: List<Consultation> = listOf(
        Consultation(
            id = "oge-russian",
            dayOfWeek = 2,
            title = "Русский язык",
            start = "14:00",
            end = "14:40"
        ),
        Consultation(
            id = "oge-geography",
            dayOfWeek = 4,
            title = "География",
            start = "14:50",
            end = "15:30"
        ),
        Consultation(
            id = "oge-math",
            dayOfWeek = 5,
            title = "Математика",
            start = "14:00",
            end = "14:40"
        ),
        Consultation(
            id = "oge-informatics-usov",
            dayOfWeek = 5,
            title = "Информатика (Усов)",
            start = "15:45",
            end = "16:25"
        )
    )

    fun copyDefaults(): List<Consultation> = all.map { it.copy() }
}
