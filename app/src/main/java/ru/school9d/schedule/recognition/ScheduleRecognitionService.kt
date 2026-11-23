package ru.school9d.schedule.recognition

import android.content.Context
import android.net.Uri
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.school9d.schedule.ai.OpenRouterClient
import ru.school9d.schedule.ai.ScheduleParser
import ru.school9d.schedule.data.SavedSchedule
import ru.school9d.schedule.data.ScheduleRepository
import ru.school9d.schedule.util.ImageCompressor
import ru.school9d.schedule.widget.ScheduleWidget

data class RecognitionOutcome(
    val schedule: SavedSchedule,
    val activeConsultations: Int
)

class ScheduleRecognitionService(context: Context) {

    private val appContext = context.applicationContext
    private val repository = ScheduleRepository.get(appContext)

    suspend fun recognize(uri: Uri): RecognitionOutcome = withContext(Dispatchers.IO) {
        repository.ensureDefaults()

        val apiKey = repository.apiKey()
        if (apiKey.isBlank()) {
            throw IllegalStateException("API-ключ OpenRouter не задан. Откройте настройки приложения.")
        }

        val dataUrl = ImageCompressor.toDataUrl(appContext.contentResolver, uri)
        val rawResponse = OpenRouterClient.recognize(apiKey, repository.model(), dataUrl)
        val schedule = ScheduleParser.parse(rawResponse)

        repository.saveSchedule(schedule)

        val activeConsultations = repository.consultations()
            .count { it.enabled && it.dayOfWeek == schedule.dayOfWeek }

        runCatching { ScheduleWidget().updateAll(appContext) }

        RecognitionOutcome(schedule, activeConsultations)
    }
}
