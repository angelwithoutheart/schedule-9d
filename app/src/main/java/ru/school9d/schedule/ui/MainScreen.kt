package ru.school9d.schedule.ui

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import ru.school9d.schedule.R
import ru.school9d.schedule.data.AppDefaults
import ru.school9d.schedule.data.Consultation
import ru.school9d.schedule.data.DayItem
import ru.school9d.schedule.data.DaySchedule
import ru.school9d.schedule.data.ItemKind
import ru.school9d.schedule.data.ScheduleMerger
import ru.school9d.schedule.ui.theme.AppPalette
import ru.school9d.schedule.util.formatTime
import ru.school9d.schedule.util.normalizeTimeOrNull
import ru.school9d.schedule.util.pluralRu
import ru.school9d.schedule.util.toShortRu
import ru.school9d.schedule.util.toWeekdayRu
import ru.school9d.schedule.util.weekdayNameRu
import java.time.LocalDate
import java.time.LocalTime

private val TabularFigures = TextStyle(fontFeatureSettings = "tnum")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPickImage: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedDay by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(viewModel, context) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(30_000)
        }
    }

    val displayedDate = if (selectedDay == 0) state.todayDate else state.tomorrowDate
    val displayedDay = if (selectedDay == 0) state.today else state.tomorrow
    val displayedItems = remember(displayedDay.items) {
        ScheduleMerger.chronological(displayedDay.items)
    }

    Scaffold(
        containerColor = AppPalette.Background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "header") {
                ScreenHeader(
                    date = displayedDate,
                    isProcessing = state.isProcessing,
                    onPickImage = onPickImage,
                    onOpenSettings = { showSettings = true }
                )
            }
            item(key = "summary") {
                DaySummary(day = displayedDay)
            }
            item(key = "tabs") {
                IosSegmentedControl(
                    selected = selectedDay,
                    onSelect = { selectedDay = it }
                )
            }
            item(key = "schedule") {
                ScheduleCard(
                    day = displayedDay,
                    items = displayedItems,
                    isToday = selectedDay == 0,
                    now = now
                )
            }
            item(key = "footnote") {
                Text(
                    text = "9Д • Виджет обновляется каждые 30 минут",
                    color = AppPalette.TextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    if (state.isProcessing) {
        ProcessingDialog()
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = AppPalette.Surface,
            contentColor = AppPalette.TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(AppPalette.SurfaceElevated)
                    )
                }
            }
        ) {
            SettingsSheet(
                consultations = state.consultations,
                initialApiKey = state.apiKey,
                initialModel = state.model,
                onToggleConsultation = viewModel::setConsultationEnabled,
                onSaveConsultation = viewModel::saveConsultation,
                onSaveApiSettings = viewModel::saveApiSettings,
                onClose = { showSettings = false }
            )
        }
    }
}

@Composable
private fun ScreenHeader(
    date: LocalDate,
    isProcessing: Boolean,
    onPickImage: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${date.toWeekdayRu()}, ${date.toShortRu()}".uppercase(),
            color = AppPalette.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Расписание",
                color = AppPalette.TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f)
            )
            RoundIconButton(
                icon = R.drawable.ic_sf_photo,
                contentDescription = "Загрузить фото расписания",
                enabled = !isProcessing,
                onClick = onPickImage
            )
            Spacer(Modifier.width(10.dp))
            RoundIconButton(
                icon = R.drawable.ic_sf_sliders,
                contentDescription = "Настройки",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun RoundIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(AppPalette.Surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) AppPalette.TextPrimary else AppPalette.TextTertiary,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun DaySummary(day: DaySchedule) {
    val first = day.items.minByOrNull { it.start ?: LocalTime.MAX }
    val last = day.items.maxByOrNull { it.end ?: LocalTime.MIN }
    val timeText = if (first?.start != null && last?.end != null) {
        "${formatTime(first.start)} – ${formatTime(last.end)}"
    } else {
        "Время уточняется"
    }
    val counts = buildString {
        if (day.lessonCount > 0) {
            append(pluralRu(day.lessonCount, "урок", "урока", "уроков"))
        }
        if (day.consultationCount > 0) {
            if (isNotEmpty()) append(" • ")
            append("${day.consultationCount} конс.")
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_sf_clock),
            contentDescription = null,
            tint = AppPalette.TextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (counts.isBlank()) timeText else "$timeText  •  $counts",
            color = AppPalette.TextSecondary,
            fontSize = 13.sp,
            style = TabularFigures,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IosSegmentedControl(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AppPalette.Surface)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf("Сегодня", "Завтра").forEachIndexed { index, label ->
            val active = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) AppPalette.SurfaceElevated else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (active) AppPalette.TextPrimary else AppPalette.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    day: DaySchedule,
    items: List<DayItem>,
    isToday: Boolean,
    now: LocalTime
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppPalette.Surface)
    ) {
        if (items.isEmpty()) {
            EmptyDay(day = day)
        } else {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 0.5.dp,
                        color = AppPalette.Separator
                    )
                }
                ScheduleRow(item = item, isToday = isToday, now = now)
            }
            if (!day.isLoaded) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 0.5.dp,
                    color = AppPalette.Separator
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sf_cap),
                        contentDescription = null,
                        tint = AppPalette.TextTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Фото расписания не загружено — показаны только консультации.",
                        color = AppPalette.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(item: DayItem, isToday: Boolean, now: LocalTime) {
    val start = item.start
    val end = item.end
    val isCurrent = isToday && start != null && end != null &&
        !now.isBefore(start) && now.isBefore(end)
    val isPast = isToday && end != null && !end.isAfter(now)
    val primary = if (isPast) AppPalette.TextTertiary else AppPalette.TextPrimary
    val secondary = if (isPast) AppPalette.TextTertiary else AppPalette.TextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(
                    color = if (isCurrent) AppPalette.Accent else Color.Transparent,
                    shape = RoundedCornerShape(1.5.dp)
                )
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 13.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(52.dp)) {
                Text(
                    text = formatTime(start),
                    color = if (isCurrent) AppPalette.Accent else primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TabularFigures,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatTime(end),
                    color = secondary,
                    fontSize = 11.sp,
                    style = TabularFigures,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = item.title,
                    color = primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.kind == ItemKind.CONSULTATION) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_cap),
                            contentDescription = null,
                            tint = if (isPast) AppPalette.TextTertiary else AppPalette.Accent,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = when {
                            item.kind == ItemKind.CONSULTATION -> "Консультация ОГЭ"
                            item.number != null -> "${item.number}-й урок"
                            else -> "Урок"
                        },
                        color = when {
                            isPast -> AppPalette.TextTertiary
                            item.kind == ItemKind.CONSULTATION -> AppPalette.Accent
                            else -> AppPalette.TextSecondary
                        },
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            if (!item.room.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sf_location_fill),
                        contentDescription = null,
                        tint = if (isPast) AppPalette.TextTertiary else AppPalette.Accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = item.room,
                        color = secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        style = TabularFigures,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDay(day: DaySchedule) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = if (day.isLoaded) "Уроков нет" else "Расписание не загружено",
            color = AppPalette.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (day.isLoaded) {
                "На этот день уроков и консультаций нет."
            } else {
                "Нажмите на иконку галереи, чтобы распознать фото расписания 9Д."
            },
            color = AppPalette.TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SettingsSheet(
    consultations: List<Consultation>,
    initialApiKey: String,
    initialModel: String,
    onToggleConsultation: (Consultation, Boolean) -> Unit,
    onSaveConsultation: (Consultation) -> Unit,
    onSaveApiSettings: (String, String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Настройки",
                color = AppPalette.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClose) {
                Text(
                    text = "Готово",
                    color = AppPalette.Accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Консультации ОГЭ")
        Spacer(Modifier.height(8.dp))
        ConsultationsSettings(
            consultations = consultations,
            onToggle = onToggleConsultation,
            onSave = onSaveConsultation
        )

        Spacer(Modifier.height(22.dp))
        SectionLabel("OpenRouter API")
        Spacer(Modifier.height(8.dp))
        ApiSettings(
            initialApiKey = initialApiKey,
            initialModel = initialModel,
            onSave = onSaveApiSettings
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = AppPalette.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppPalette.SurfaceMuted),
        content = content
    )
}

@Composable
private fun ConsultationsSettings(
    consultations: List<Consultation>,
    onToggle: (Consultation, Boolean) -> Unit,
    onSave: (Consultation) -> Unit
) {
    val sorted = remember(consultations) {
        consultations.sortedWith(compareBy<Consultation>({ it.dayOfWeek }, { it.start }))
    }
    SettingsGroup {
        if (sorted.isEmpty()) {
            Text(
                text = "Консультации не настроены.",
                color = AppPalette.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(14.dp)
            )
        } else {
            sorted.forEachIndexed { index, consultation ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 14.dp),
                        thickness = 0.5.dp,
                        color = AppPalette.Separator
                    )
                }
                ConsultationSettingRow(
                    consultation = consultation,
                    onToggle = onToggle,
                    onSave = onSave
                )
            }
        }
    }
}

@Composable
private fun ConsultationSettingRow(
    consultation: Consultation,
    onToggle: (Consultation, Boolean) -> Unit,
    onSave: (Consultation) -> Unit
) {
    var expanded by rememberSaveable(consultation.id) { mutableStateOf(false) }
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

    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { expanded = !expanded }
            ) {
                Text(
                    text = consultation.title,
                    color = if (consultation.enabled) {
                        AppPalette.TextPrimary
                    } else {
                        AppPalette.TextSecondary
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(weekdayNameRu(consultation.dayOfWeek))
                        append(" • ")
                        append(consultation.start)
                        append("–")
                        append(consultation.end)
                        if (consultation.room.isNotBlank()) {
                            append(" • каб. ")
                            append(consultation.room)
                        }
                    },
                    color = AppPalette.TextSecondary,
                    fontSize = 11.sp,
                    style = TabularFigures,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sf_chevron_right),
                    contentDescription = if (expanded) "Свернуть" else "Изменить",
                    tint = AppPalette.TextTertiary,
                    modifier = Modifier
                        .size(12.dp)
                        .rotate(if (expanded) 90f else 0f)
                )
            }
            Spacer(Modifier.width(2.dp))
            Switch(
                checked = consultation.enabled,
                onCheckedChange = { checked -> onToggle(consultation, checked) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppPalette.Green,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF39393D),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }

        if (expanded) {
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
                    shape = RoundedCornerShape(10.dp),
                    colors = settingsFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (timesValid) {
                            onSave(
                                consultation.copy(
                                    start = normalizedStart!!,
                                    end = normalizedEnd!!,
                                    room = room.trim()
                                )
                            )
                            expanded = false
                        }
                    },
                    enabled = timesValid,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppPalette.Accent,
                        contentColor = Color.White,
                        disabledContainerColor = AppPalette.Accent.copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sf_check),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Сохранить", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
        shape = RoundedCornerShape(10.dp),
        colors = settingsFieldColors(),
        modifier = modifier
    )
}

@Composable
private fun ApiSettings(
    initialApiKey: String,
    initialModel: String,
    onSave: (String, String) -> Unit
) {
    var apiKey by remember(initialApiKey) { mutableStateOf(initialApiKey) }
    var model by remember(initialModel) { mutableStateOf(initialModel) }
    var keyVisible by rememberSaveable { mutableStateOf(false) }

    SettingsGroup {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            painter = painterResource(
                                if (keyVisible) R.drawable.ic_sf_eye_off else R.drawable.ic_sf_eye
                            ),
                            contentDescription = if (keyVisible) "Скрыть ключ" else "Показать ключ",
                            tint = AppPalette.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = settingsFieldColors(),
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
                        color = AppPalette.TextSecondary.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = settingsFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onSave(apiKey, model) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppPalette.Accent,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sf_check),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text("Сохранить настройки", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Ключ хранится локально в DataStore. Стандартная модель: ${AppDefaults.DEFAULT_MODEL}",
                color = AppPalette.TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppPalette.TextPrimary,
    unfocusedTextColor = AppPalette.TextPrimary,
    focusedBorderColor = AppPalette.Accent,
    unfocusedBorderColor = AppPalette.Separator,
    focusedLabelColor = AppPalette.Accent,
    unfocusedLabelColor = AppPalette.TextSecondary,
    cursorColor = AppPalette.Accent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)

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
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppPalette.Surface)
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
                        text = "Распознаю расписание",
                        color = AppPalette.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Vision-модель OpenRouter читает колонку 9Д. Обычно это занимает 10–30 секунд.",
                        color = AppPalette.TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
