package ru.school9d.schedule.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.school9d.schedule.data.AppDefaults
import java.io.IOException
import java.util.concurrent.TimeUnit

object OpenRouterClient {

    const val PROMPT = """На фото школьное расписание с таблицами по классам. 1) Прочитай заголовок листа и определи дату (день, месяц, год) и день недели (1=Пн .. 7=Вс). Если год из 2 цифр (например 24.11.26) — это 2026 год. 2) Найди блок 9-х классов и строго колонку класса '9Д' (не перепутай с 9Г слева и 9Е справа! У каждого класса две подколонки: предмет слева и его кабинет справа). 3) Обрати внимание на пустые строки: если 1-й, 2-й или 3-й урок пустые, НЕ сдвигай нумерацию! Строго сохраняй номер урока из первой колонки 'ур'/'урок'. 4) Красиво расшифруй сокращения (Англ.яз -> Английский язык, Литер. -> Литература, Алгеб. -> Алгебра, Геом. -> Геометрия, Биол. -> Биология, Рус.яз. -> Русский язык, Истор. -> История, Чит.гр -> Читательская грамотность, ОБЗР/ОБиЗР -> ОБЗР, Пр.м -> Практикум по математике, Физ-ра -> Физкультура, эор -> ЭОР). Верни ТОЛЬКО чистый валидный JSON без пояснений и без ```json обертки строго такого формата: {"date": "YYYY-MM-DD", "dayOfWeek": 2, "lessons": [{"number": 2, "subject": "Английский язык", "room": "2.1"}]}"""

    private const val MODEL_FIELD = "model"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    suspend fun recognize(
        apiKey: String,
        model: String,
        jpegDataUrl: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IOException("API-ключ OpenRouter не задан. Откройте настройки приложения.")
        }

        val primaryModel = model.trim().ifBlank { AppDefaults.DEFAULT_MODEL }
        val models = buildList {
            add(primaryModel)
            addAll(AppDefaults.FALLBACK_MODELS)
        }.distinct()

        val requestBody = buildJsonObject {
            put(MODEL_FIELD, primaryModel)
            putJsonArray("models") {
                models.forEach { add(it) }
            }
            put("temperature", JsonPrimitive(0))
            put("max_tokens", JsonPrimitive(2048))
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", PROMPT)
                        }
                        addJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", jpegDataUrl)
                            }
                        }
                    }
                }
            }
        }

        val request = Request.Builder()
            .url(AppDefaults.OPENROUTER_URL)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("HTTP-Referer", "https://school9d.app")
            .header("X-Title", "Schedule 9D")
            .post(requestBody.toString().toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val apiMessage = runCatching {
                    json.parseToJsonElement(raw)
                        .jsonObject["error"]
                        ?.jsonObject
                        ?.get("message")
                        ?.jsonPrimitive
                        ?.contentOrNull
                }.getOrNull()
                throw IOException(
                    "OpenRouter ${response.code}: ${apiMessage ?: raw.take(400).ifBlank { "пустой ответ" }}"
                )
            }
            extractContent(raw)
        }
    }

    private fun extractContent(raw: String): String {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrElse {
            throw IOException("OpenRouter вернул некорректный JSON")
        }

        val message = ((root["choices"] as? JsonArray)
            ?.firstOrNull() as? JsonObject)
            ?.get("message") as? JsonObject
            ?: throw IOException("В ответе OpenRouter нет choices/message")

        val content = message["content"]
        val text = when (content) {
            is JsonPrimitive -> content.contentOrNull.orEmpty()
            is JsonArray -> content.joinToString(separator = "") { part ->
                when (part) {
                    is JsonObject -> (part["text"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                    is JsonPrimitive -> part.contentOrNull.orEmpty()
                    else -> ""
                }
            }
            else -> ""
        }.trim()

        if (text.isBlank()) {
            throw IOException("Модель не вернула текст расписания. Попробуйте другое фото.")
        }
        return text
    }
}
