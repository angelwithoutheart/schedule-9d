package ru.school9d.schedule.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import ru.school9d.schedule.BuildConfig
import java.time.LocalDate

private val Context.scheduleDataStore by preferencesDataStore(name = "schedule_9d")

private object Keys {
    val API_KEY = stringPreferencesKey("openrouter_api_key")
    val MODEL = stringPreferencesKey("openrouter_model")
    val CONSULTATIONS = stringPreferencesKey("oge_consultations")
    fun schedule(date: String) = stringPreferencesKey("schedule_$date")
}

class ScheduleRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val dataStore = appContext.scheduleDataStore
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        @Volatile
        private var instance: ScheduleRepository? = null

        fun get(context: Context): ScheduleRepository =
            instance ?: synchronized(this) {
                instance ?: ScheduleRepository(context).also { instance = it }
            }
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            apiKey = prefs[Keys.API_KEY] ?: BuildConfig.DEFAULT_OPENROUTER_KEY,
            model = prefs[Keys.MODEL]?.takeIf { it.isNotBlank() } ?: AppDefaults.DEFAULT_MODEL
        )
    }

    val consultationsFlow: Flow<List<Consultation>> = dataStore.data.map { prefs ->
        readConsultations(prefs)
    }

    fun scheduleFlow(date: LocalDate): Flow<SavedSchedule?> = dataStore.data.map { prefs ->
        readSchedule(prefs, date)
    }

    suspend fun ensureDefaults() {
        dataStore.edit { prefs ->
            if (!prefs.contains(Keys.API_KEY)) {
                prefs[Keys.API_KEY] = BuildConfig.DEFAULT_OPENROUTER_KEY
            }
            if (!prefs.contains(Keys.MODEL)) {
                prefs[Keys.MODEL] = AppDefaults.DEFAULT_MODEL
            }
            if (!prefs.contains(Keys.CONSULTATIONS)) {
                prefs[Keys.CONSULTATIONS] = json.encodeToString(
                    ConsultationList.serializer(),
                    ConsultationList(ConsultationsDefaults.copyDefaults())
                )
            }
        }
    }

    suspend fun apiKey(): String {
        val prefs = dataStore.data.first()
        return prefs[Keys.API_KEY] ?: BuildConfig.DEFAULT_OPENROUTER_KEY
    }

    suspend fun model(): String {
        val prefs = dataStore.data.first()
        return prefs[Keys.MODEL]?.takeIf { it.isNotBlank() } ?: AppDefaults.DEFAULT_MODEL
    }

    suspend fun consultations(): List<Consultation> =
        readConsultations(dataStore.data.first())

    suspend fun schedule(date: LocalDate): SavedSchedule? =
        readSchedule(dataStore.data.first(), date)

    suspend fun saveSchedule(schedule: SavedSchedule) {
        val normalized = schedule.copy(
            recognizedAt = if (schedule.recognizedAt > 0) schedule.recognizedAt else System.currentTimeMillis()
        )
        dataStore.edit { prefs ->
            prefs[Keys.schedule(normalized.date)] = json.encodeToString(
                SavedSchedule.serializer(),
                normalized
            )
        }
    }

    suspend fun saveApiKey(key: String) {
        dataStore.edit { prefs ->
            prefs[Keys.API_KEY] = key.trim()
        }
    }

    suspend fun saveModel(model: String) {
        dataStore.edit { prefs ->
            prefs[Keys.MODEL] = model.trim().ifBlank { AppDefaults.DEFAULT_MODEL }
        }
    }

    suspend fun saveConsultation(updated: Consultation) {
        dataStore.edit { prefs ->
            val current = readConsultations(prefs)
            val exists = current.any { it.id == updated.id }
            val next = if (exists) {
                current.map { if (it.id == updated.id) updated else it }
            } else {
                current + updated
            }
            prefs[Keys.CONSULTATIONS] = json.encodeToString(
                ConsultationList.serializer(),
                ConsultationList(next.sortedWith(compareBy({ it.dayOfWeek }, { it.start })))
            )
        }
    }

    private fun readSchedule(prefs: Preferences, date: LocalDate): SavedSchedule? {
        val raw = prefs[Keys.schedule(date.toString())] ?: return null
        return runCatching {
            json.decodeFromString(SavedSchedule.serializer(), raw)
        }.getOrNull()
    }

    private fun readConsultations(prefs: Preferences): List<Consultation> {
        val raw = prefs[Keys.CONSULTATIONS] ?: return ConsultationsDefaults.copyDefaults()
        return runCatching {
            json.decodeFromString(ConsultationList.serializer(), raw).items
        }.getOrElse { ConsultationsDefaults.copyDefaults() }
    }
}
