package ru.school9d.schedule.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import ru.school9d.schedule.data.Lesson
import ru.school9d.schedule.data.SavedSchedule
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
private data class AiScheduleDto(
    val date: String? = null,
    val dayOfWeek: Int = 0,
    val lessons: List<AiLessonDto> = emptyList()
)

@Serializable
private data class AiLessonDto(
    val number: Int = 0,
    val subject: String? = null,
    val room: JsonElement? = null
)

object ScheduleParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val dateFormatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("d.M.yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yy"),
        DateTimeFormatter.ofPattern("d.M.yy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yy")
    )

    fun parse(raw: String): SavedSchedule {
        val firstBrace = raw.indexOf('{')
        val lastBrace = raw.lastIndexOf('}')
        if (firstBrace < 0 || lastBrace <= firstBrace) {
            throw IllegalArgumentException("В ответе модели не найден JSON расписания")
        }
        val candidate = raw.substring(firstBrace, lastBrace + 1)

        val dto = try {
            json.decodeFromString(AiScheduleDto.serializer(), candidate)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("Не удалось разобрать расписание из ответа модели", error)
        }

        val date = parseDate(dto.date)
            ?: throw IllegalArgumentException("Модель не указала дату расписания")

        val seenNumbers = mutableSetOf<Int>()
        val lessons = dto.lessons.mapNotNull { source ->
            val subject = source.subject?.trim().orEmpty()
            if (subject.isBlank()) return@mapNotNull null
            if (source.number < 1) return@mapNotNull null
            if (!seenNumbers.add(source.number)) return@mapNotNull null

            Lesson(
                number = source.number,
                subject = subject,
                room = roomText(source.room)
            )
        }.sortedBy { it.number }

        return SavedSchedule(
            date = date.toString(),
            dayOfWeek = date.dayOfWeek.value,
            lessons = lessons,
            recognizedAt = System.currentTimeMillis()
        )
    }

    private fun parseDate(raw: String?): LocalDate? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        for (formatter in dateFormatters) {
            runCatching { LocalDate.parse(value, formatter) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun roomText(element: JsonElement?): String = when (element) {
        null, is JsonNull -> ""
        is JsonPrimitive -> element.contentOrNull.orEmpty().trim()
        else -> element.toString().trim()
    }
}
