package ru.school9d.schedule.share

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.school9d.schedule.recognition.ScheduleRecognitionService
import ru.school9d.schedule.util.pluralRu
import ru.school9d.schedule.util.toNumericRu
import java.time.LocalDate

data class ShareUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    private val service = ScheduleRecognitionService(application)
    private val _state = MutableStateFlow(ShareUiState())
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    private var started = false

    fun start(uri: Uri?) {
        if (started) return
        started = true

        if (uri == null) {
            _state.value = ShareUiState(
                loading = false,
                error = "Не удалось получить изображение из приложения-источника."
            )
            return
        }

        viewModelScope.launch {
            _state.value = ShareUiState(loading = true)
            try {
                val outcome = service.recognize(uri)
                val date = runCatching { LocalDate.parse(outcome.schedule.date) }
                    .getOrNull()
                    ?.toNumericRu()
                    ?: outcome.schedule.date
                val parts = mutableListOf(
                    pluralRu(outcome.schedule.lessons.size, "урок", "урока", "уроков")
                )
                if (outcome.activeConsultations > 0) {
                    parts += pluralRu(
                        outcome.activeConsultations,
                        "консультация",
                        "консультации",
                        "консультаций"
                    )
                }
                _state.value = ShareUiState(
                    loading = false,
                    successMessage = "Расписание на $date сохранено: ${parts.joinToString(" + ")}"
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                _state.value = ShareUiState(
                    loading = false,
                    error = error.message?.takeIf { it.isNotBlank() }
                        ?: "Не удалось распознать расписание. Попробуйте другое фото."
                )
            }
        }
    }
}
