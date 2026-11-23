package ru.school9d.schedule.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.school9d.schedule.alarm.AlarmScheduler
import ru.school9d.schedule.data.AppDefaults
import ru.school9d.schedule.data.Consultation
import ru.school9d.schedule.data.DayItem
import ru.school9d.schedule.data.DaySchedule
import ru.school9d.schedule.data.ItemKind
import ru.school9d.schedule.ui.theme.AppPalette
import ru.school9d.schedule.util.formatTime
import ru.school9d.schedule.util.normalizeTimeOrNull
import ru.school9d.schedule.util.pluralRu
import ru.school9d.schedule.util.toShortRu
import ru.school9d.schedule.util.weekdayNameRu
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPickImage: () -> Unit,
    onRequestExactAlarms: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel, context) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    var exactAlarmsAllowed by remember {
        mutableStateOf(AlarmScheduler.canScheduleExactAlarms(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmsAllowed = AlarmScheduler.canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = AppPalette.Background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeader(today = state.todayDate)
            }
            item {
                GalleryButton(
                    enabled = !state.isProcessing,
                    onClick = onPickImage
                )
            }
            item {
                DayCard(
                    label = "СЕГОДНЯ",
                    date = state.todayDate,
                    day = state.today
                )
            }
            item {
                DayCard(
                    label = "ЗАВТРА",
                    date = state.tomorrowDate,
                    day = state.tomorrow
                )
            }
            if (!exactAlarmsAllowed) {
                item {
                    SectionCard(
                        title = "Точные обновления",
                        subtitle = "Разрешите точные будильники, чтобы виджет менялся ровно в минуту начала и конца урока."
                    ) {
                        Button(
                            onClick = onRequestExactAlarms,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppPalette.SurfaceElevated,
                                contentColor = AppPalette.Accent
                            )
                        ) {
                            Text("Разрешить точные будильники", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                ConsultationsSection(
                    consultations = state.consultations,
                    onToggle = viewModel::setConsultationEnabled,
                    onSave = viewModel::saveConsultation
                )
            }
            item {
                ApiSettingsSection(
                    initialApiKey = state.apiKey,
                    initialModel = state.model,
                    onSave = viewModel::saveApiSettings
                )
            }
            item {
                Text(
                    text = "Виджет работает офлайн: расписание хранится в DataStore, обновление — по AlarmManager.",
                    color = AppPalette.TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }

    if (state.isProcessing) {
        ProcessingDialog()
    }
}

@Composable
private fun ScreenHeader(today: LocalDate) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Расписание",
                color = AppPalette.TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "9Д класс • ${today.toShortRu()} • ${weekdayNameRu(today.dayOfWeek.value)}",
                color = AppPalette.TextMuted,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(AppPalette.SurfaceElevated, CircleShape)
                .border(1.dp, AppPalette.Border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "9Д",
                color = AppPalette.Accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GalleryButton(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(AppPalette.Accent, AppPalette.AccentSecondary)
                )
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AppPalette.DarkText.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = AppPalette.DarkText
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Загрузить фото расписания",
                    color = AppPalette.DarkText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Фото из галереи • AI-распознавание колонки 9Д",
                    color = AppPalette.DarkText.copy(alpha = 0.72f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun DayCard(label: String, date: LocalDate, day: DaySchedule) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppPalette.Border, shape),
        shape = shape,
        color = AppPalette.Surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$label • ${date.toShortRu()}".uppercase(),
                        color = AppPalette.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = weekdayNameRu(date.dayOfWeek.value),
                        color = AppPalette.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CountPill(pluralRu(day.lessonCount, "урок", "урока", "уроков"))
                    if (day.consultationCount > 0) {
                        CountPill(pluralRu(day.consultationCount, "конс.", "конс.", "конс."))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (day.items.isEmpty()) {
                Text(
                    text = if (day.isLoaded) {
                        "На этот день уроков нет."
                    } else {
                        "Фото расписания ещё не распознано. Нажмите «Загрузить фото расписания» или поделитесь фото из галереи."
                    },
                    color = AppPalette.TextMuted,
                    fontSize = 13.sp
                )
            } else {
                if (!day.isLoaded) {
                    Text(
                        text = "Фото ещё не загружено — видны постоянные консультации.",
                        color = AppPalette.TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(10.dp))
                }
                day.items.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = AppPalette.Border.copy(alpha = 0.65f)
                        )
                    }
                    ScheduleRow(item)
                }
            }
        }
    }
}

@Composable
private fun CountPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = AppPalette.SurfaceElevated
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = AppPalette.Accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ScheduleRow(item: DayItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(58.dp)) {
            Text(
                text = formatTime(item.start),
                color = AppPalette.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatTime(item.end),
                color = AppPalette.TextMuted,
                fontSize = 11.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = AppPalette.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.kind == ItemKind.CONSULTATION) {
                    Text(
                        text = item.typeLabel,
                        color = AppPalette.Accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (item.number != null) {
                    Text(
                        text = "${item.number}-й урок",
                        color = AppPalette.TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (!item.room.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(50),
                color = AppPalette.SurfaceElevated
            ) {
                Text(
                    text = "каб. ${item.room}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = AppPalette.Accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConsultationsSection(
    consultations: List<Consultation>,
    onToggle: (Consultation, Boolean) -> Unit,
    onSave: (Consultation) -> Unit
) {
    SectionCard(
        title = "Консультации ОГЭ",
        subtitle = "Добавляются в конец расписания нужного дня. Время и кабинет можно изменить."
    ) {
        val sorted = consultations.sortedWith(
            compareBy<Consultation>({ it.dayOfWeek }, { it.start })
        )
        if (sorted.isEmpty()) {
            Text(
                text = "Консультации не настроены.",
                color = AppPalette.TextMuted,
                fontSize = 13.sp
            )
        } else {
            sorted.forEachIndexed { index, consultation ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = AppPalette.Border.copy(alpha = 0.65f)
                    )
                }
                ConsultationEditor(
                    consultation = consultation,
                    onToggle = onToggle,
                    onSave = onSave
                )
            }
        }
    }
}

@Composable
private fun ConsultationEditor(
    consultation: Consultation,
    onToggle: (Consultation, Boolean) -> Unit,
    onSave: (Consultation) -> Unit
) {
    var start by remember(consultation.id, consultation.start) {
        mutableStateOf(consultation.start)
    }
    var end by remember(consultation.id, consultation.end) {
        mutableStateOf(consultation.end)
    }
    var room by remember(consultation.id, consultation.room) {
        mutableStateOf(consultation.room)
    }

    val normalizedStart = normalizeTimeOrNull(start)
    val normalizedEnd = normalizeTimeOrNull(end)
    val timesValid = normalizedStart != null &&
        normalizedEnd != null &&
        LocalTime.parse(normalizedEnd).isAfter(LocalTime.parse(normalizedStart))

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = consultation.title,
                    color = if (consultation.enabled) {
                        AppPalette.TextPrimary
                    } else {
                        AppPalette.TextMuted
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${weekdayNameRu(consultation.dayOfWeek)} • ${consultation.start}–${consultation.end}",
                    color = AppPalette.TextMuted,
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = consultation.enabled,
                onCheckedChange = { checked -> onToggle(consultation, checked) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppPalette.Background,
                    checkedTrackColor = AppPalette.Accent,
                    uncheckedThumbColor = AppPalette.TextMuted,
                    uncheckedTrackColor = AppPalette.SurfaceElevated,
                    uncheckedBorderColor = AppPalette.Border
                )
            )
        }

        if (consultation.enabled) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField(
                    value = start,
                    label = "Начало",
                    modifier = Modifier.weight(1f),
                    onValueChange = { start = it }
                )
                TimeField(
                    value = end,
                    label = "Конец",
                    modifier = Modifier.weight(1f),
                    onValueChange = { end = it }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Кабинет", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = scheduleFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = {
                        if (normalizedStart != null && normalizedEnd != null) {
                            onSave(
                                consultation.copy(
                                    start = normalizedStart,
                                    end = normalizedEnd,
                                    room = room.trim()
                                )
                            )
                        }
                    },
                    enabled = timesValid,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AppPalette.Accent.copy(alpha = 0.16f),
                        contentColor = AppPalette.Accent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Сохранить", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            if (!timesValid) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Время в формате ЧЧ:ММ, конец должен быть позже начала.",
                    color = AppPalette.Danger,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TimeField(
    value: String,
    label: String,
    modifier: Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.length <= 5) onValueChange(input)
        },
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        shape = RoundedCornerShape(12.dp),
        colors = scheduleFieldColors(),
        modifier = modifier
    )
}

@Composable
private fun ApiSettingsSection(
    initialApiKey: String,
    initialModel: String,
    onSave: (String, String) -> Unit
) {
    var apiKey by remember(initialApiKey) { mutableStateOf(initialApiKey) }
    var model by remember(initialModel) { mutableStateOf(initialModel) }
    var keyVisible by remember { mutableStateOf(false) }

    SectionCard(
        title = "OpenRouter API",
        subtitle = "Ключ и Vision-модель для распознавания фото. Пустая модель — стандартная."
    ) {
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API-ключ", fontSize = 11.sp) },
            singleLine = true,
            visualTransformation = if (keyVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { keyVisible = !keyVisible }) {
                    Icon(
                        imageVector = if (keyVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = if (keyVisible) "Скрыть ключ" else "Показать ключ",
                        tint = AppPalette.TextMuted
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = scheduleFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Модель", fontSize = 11.sp) },
            placeholder = {
                Text(
                    text = AppDefaults.DEFAULT_MODEL,
                    color = AppPalette.TextMuted.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = scheduleFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Ключ хранится локально в DataStore. Стандартная модель: ${AppDefaults.DEFAULT_MODEL}",
            color = AppPalette.TextMuted,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { onSave(apiKey, model) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppPalette.Accent,
                contentColor = AppPalette.DarkText
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Сохранить настройки", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppPalette.Border, shape),
        shape = shape,
        color = AppPalette.Surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                color = AppPalette.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = AppPalette.TextMuted,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ProcessingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            val shape = RoundedCornerShape(24.dp)
            Surface(
                shape = shape,
                color = AppPalette.Surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AppPalette.Border, shape)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = AppPalette.Accent,
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Распознаю расписание 9Д...",
                        color = AppPalette.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Vision-модель OpenRouter читает колонку 9Д. Обычно это занимает 10–30 секунд.",
                        color = AppPalette.TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun scheduleFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppPalette.TextPrimary,
    unfocusedTextColor = AppPalette.TextPrimary,
    focusedBorderColor = AppPalette.Accent,
    unfocusedBorderColor = AppPalette.Border,
    focusedLabelColor = AppPalette.Accent,
    unfocusedLabelColor = AppPalette.TextMuted,
    cursorColor = AppPalette.Accent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)
