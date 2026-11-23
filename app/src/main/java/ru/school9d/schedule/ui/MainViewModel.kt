package ru.school9d.schedule.ui

import android.app.Application
import android.net.Uri
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school9d.schedule.alarm.AlarmScheduler
import ru.school9d.schedule.data.AppDefaults
import ru.school9d.schedule.data.Consultation
import ru.school9d.schedule.data.ConsultationsDefaults
import ru.school9d.schedule.data.DaySchedule
import ru.school9d.schedule.data.SavedSchedule
import ru.school9d.schedule.data.ScheduleMerger
import ru.school9d.schedule.data.ScheduleRepository
import ru.school9d.schedule.recognition.ScheduleRecognitionService
import ru.school9d.schedule.util.pluralRu
import ru.school9d.schedule.util.toNumericRu
import ru.school9d.schedule.widget.ScheduleWidget
import java.time.LocalDate

data class MainUiState(
    val todayDate: LocalDate = LocalDate.now(),
    val tomorrowDate: LocalDate = LocalDate.now().plusDays(1),
    val today: DaySchedule = ScheduleMerger.merge(
        LocalDate.now(),
        null,
        ConsultationsDefaults.all
    ),
    val tomorrow: DaySchedule = ScheduleMerger.merge(
        LocalDate.now().plusDays(1),
        null,
        ConsultationsDefaults.all
    ),
    val consultations: List<Consultation> = ConsultationsDefaults.all,
    val apiKey: String = "",
    val model: String = AppDefaults.DEFAULT_MODEL,
    val isProcessing: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScheduleRepository.get(application)
    private val recognitionService = ScheduleRecognitionService(application)

    private val dateFlow = MutableStateFlow(LocalDate.now())
    private val processingFlow = MutableStateFlow(false)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val schedulesFlow = dateFlow.flatMapLatest { date ->
        combine(
            repository.scheduleFlow(date),
            repository.scheduleFlow(date.plusDays(1))
        ) { savedToday: SavedSchedule?, savedTomorrow: SavedSchedule? ->
            Triple(date, savedToday, savedTomorrow)
        }
    }

    val uiState: StateFlow<MainUiState> = combine(
        repository.settingsFlow,
        repository.consultationsFlow,
        schedulesFlow,
        processingFlow
    ) { settings, consultations, schedules, processing ->
        val (date, savedToday, savedTomorrow) = schedules
        MainUiState(
            todayDate = date,
            tomorrowDate = date.plusDays(1),
            today = ScheduleMerger.merge(date, savedToday, consultations),
            tomorrow = ScheduleMerger.merge(date.plusDays(1), savedTomorrow, consultations),
            consultations = consultations,
            apiKey = settings.apiKey,
            model = settings.model,
            isProcessing = processing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            runCatching { repository.ensureDefaults() }
        }
    }

    fun refreshDate() {
        val today = LocalDate.now()
        if (dateFlow.value != today) {
            dateFlow.value = today
        }
    }

    fun recognizeImage(uri: Uri) {
        if (processingFlow.value) return
        viewModelScope.launch {
            processingFlow.value = true
            try {
                val outcome = recognitionService.recognize(uri)
                val date = runCatching { LocalDate.parse(outcome.schedule.date) }
                    .getOrNull()
                    ?.toNumericRu()
                    ?: outcome.schedule.date
                val lessonsText = pluralRu(
                    outcome.schedule.lessons.size,
                    "урок",
                    "урока",
                    "уроков"
                )
                val consultationsText = if (outcome.activeConsultations > 0) {
                    " + " + pluralRu(
                        outcome.activeConsultations,
                        "консультация",
                        "консультации",
                        "консультаций"
                    )
                } else {
                    ""
                }
                _messages.tryEmit("Расписание на $date сохранено: $lessonsText$consultationsText")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                _messages.tryEmit(
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "Не удалось распознать расписание"
                )
            } finally {
                processingFlow.value = false
            }
        }
    }

    fun setConsultationEnabled(consultation: Consultation, enabled: Boolean) {
        saveConsultation(consultation.copy(enabled = enabled))
    }

    fun saveConsultation(consultation: Consultation) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.saveConsultation(consultation)
                    runCatching { AlarmScheduler.rescheduleSuspend(app) }
                    runCatching { ScheduleWidget().updateAll(app) }
                }
                _messages.tryEmit("Консультация «${consultation.title}» сохранена")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                _messages.tryEmit(error.message ?: "Не удалось сохранить консультацию")
            }
        }
    }

    fun saveApiSettings(apiKey: String, model: String) {
        if (apiKey.isBlank()) {
            _messages.tryEmit("API-ключ не может быть пустым")
            return
        }
        viewModelScope.launch {
            try {
                repository.saveApiKey(apiKey)
                repository.saveModel(model)
                _messages.tryEmit("Настройки OpenRouter сохранены")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                _messages.tryEmit(error.message ?: "Не удалось сохранить настройки")
            }
        }
    }
}
