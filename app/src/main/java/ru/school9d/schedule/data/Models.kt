package ru.school9d.schedule.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Immutable
@Serializable
data class Lesson(
    val number: Int = 0,
    val subject: String = "",
    val room: String = ""
)

@Immutable
@Serializable
data class SavedSchedule(
    val date: String,
    val dayOfWeek: Int,
    val lessons: List<Lesson> = emptyList(),
    val recognizedAt: Long = 0L
)

@Immutable
@Serializable
data class Consultation(
    val id: String,
    val dayOfWeek: Int,
    val title: String,
    val start: String,
    val end: String,
    val room: String = "",
    val enabled: Boolean = true,
    val type: String = "КОНСУЛЬТАЦИЯ"
)

@Serializable
data class ConsultationList(
    val items: List<Consultation> = emptyList()
)

@Immutable
data class AppSettings(
    val apiKey: String = "",
    val model: String = AppDefaults.DEFAULT_MODEL
)

enum class ItemKind {
    LESSON,
    CONSULTATION
}

@Immutable
data class DayItem(
    val kind: ItemKind,
    val number: Int?,
    val title: String,
    val room: String?,
    val start: LocalTime?,
    val end: LocalTime?,
    val typeLabel: String
)

@Immutable
data class DaySchedule(
    val date: LocalDate,
    val dayOfWeek: Int,
    val items: List<DayItem>,
    val isLoaded: Boolean,
    val recognizedAt: Long? = null
) {
    val lessonCount: Int get() = items.count { it.kind == ItemKind.LESSON }
    val consultationCount: Int get() = items.count { it.kind == ItemKind.CONSULTATION }
}
