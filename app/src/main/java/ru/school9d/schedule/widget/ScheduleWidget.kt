package ru.school9d.schedule.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ru.school9d.schedule.MainActivity
import ru.school9d.schedule.R
import ru.school9d.schedule.data.ScheduleMerger
import ru.school9d.schedule.data.ScheduleRepository
import java.time.LocalDateTime

private val WidgetBackground = ColorProvider(Color(0xFF1C1C1E))
private val WidgetPlate = ColorProvider(Color(0xFF252528))
private val WidgetInnerPlate = ColorProvider(Color(0xFF2C2C2E))
private val WidgetWhite = ColorProvider(Color(0xFFFFFFFF))
private val WidgetSecondary = ColorProvider(Color(0xFF8E8E93))
private val WidgetTertiary = ColorProvider(Color(0xFF636366))
private val WidgetAccent = ColorProvider(Color(0xFF0A84FF))

class ScheduleWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 220.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = ScheduleRepository.get(context)
        repository.ensureDefaults()

        val now = LocalDateTime.now()
        val todayDate = now.toLocalDate()
        val consultations = repository.consultations()
        val today = ScheduleMerger.merge(
            todayDate,
            repository.schedule(todayDate),
            consultations
        )
        val tomorrow = ScheduleMerger.merge(
            todayDate.plusDays(1),
            repository.schedule(todayDate.plusDays(1)),
            consultations
        )
        val data = WidgetDataFactory.build(today, tomorrow, consultations, now)
        val radius = widgetCornerRadius(context)

        provideContent {
            val size = LocalSize.current
            when {
                size.width < 180.dp -> CompactWidget(data, radius)
                size.height < 180.dp -> WideWidget(data, radius)
                else -> TallWidget(data, radius)
            }
        }
    }
}

@Composable
private fun CompactWidget(data: WidgetUiData, radius: Dp) {
    Column(
        modifier = widgetRoot(radius).padding(10.dp)
    ) {
        WidgetHeader(status = data.status)
        Spacer(GlanceModifier.height(3.dp))

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(WidgetPlate)
                .cornerRadius(12.dp)
                .padding(horizontal = 9.dp, vertical = 7.dp)
        ) {
            val item = data.highlighted
            if (item == null) {
                Text(
                    text = data.emptyMessage.ifBlank { "Уроков нет" },
                    style = TextStyle(
                        color = WidgetWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = "Откройте приложение",
                    style = TextStyle(color = WidgetSecondary, fontSize = 9.sp),
                    maxLines = 1
                )
            } else {
                Text(
                    text = item.badge,
                    style = TextStyle(
                        color = WidgetAccent,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(1.dp))
                Text(
                    text = item.title,
                    style = TextStyle(
                        color = WidgetWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2
                )
                if (item.room != null) {
                    Spacer(GlanceModifier.height(3.dp))
                    RoomBadge(room = item.room, compact = true)
                }
            }
        }

        Spacer(GlanceModifier.defaultWeight())
        WidgetFooterRow(footer = data.footer, compact = true)
    }
}

@Composable
private fun WideWidget(data: WidgetUiData, radius: Dp) {
    Column(
        modifier = widgetRoot(radius).padding(10.dp)
    ) {
        WidgetHeader(status = data.status)
        Spacer(GlanceModifier.height(3.dp))

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(WidgetPlate)
                    .cornerRadius(12.dp)
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
                val item = data.highlighted
                if (item == null) {
                    Text(
                        text = data.emptyMessage.ifBlank { "Уроков нет" },
                        style = TextStyle(
                            color = WidgetWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = item.badge,
                        style = TextStyle(
                            color = WidgetAccent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(1.dp))
                    Text(
                        text = item.title,
                        style = TextStyle(
                            color = WidgetWhite,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    if (item.room != null) {
                        Spacer(GlanceModifier.height(3.dp))
                        RoomBadge(room = item.room, compact = true)
                    }
                }
            }

            Spacer(GlanceModifier.width(8.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "ДАЛЬШЕ",
                    style = TextStyle(
                        color = WidgetSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(2.dp))
                val next = data.upcoming.take(3)
                if (next.isEmpty()) {
                    Text(
                        text = "Дальше уроков нет",
                        style = TextStyle(color = WidgetSecondary, fontSize = 9.sp),
                        maxLines = 1
                    )
                } else {
                    next.forEachIndexed { index, item ->
                        if (index > 0) Spacer(GlanceModifier.height(2.dp))
                        MiniLessonRow(item)
                    }
                }
            }
        }

        Spacer(GlanceModifier.height(3.dp))
        WidgetFooterRow(footer = data.footer, compact = false)
    }
}

@Composable
private fun TallWidget(data: WidgetUiData, radius: Dp) {
    Column(
        modifier = widgetRoot(radius).padding(12.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = data.status,
                    style = TextStyle(
                        color = WidgetSecondary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(1.dp))
                Text(
                    text = data.dayTitle,
                    style = TextStyle(
                        color = WidgetWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
            RefreshIcon()
        }

        Spacer(GlanceModifier.height(4.dp))

        // Glance translates at most 10 children per container into RemoteViews, so the
        // lessons are split into small nested groups. Otherwise the tail of the list and
        // the consultation footer silently disappear from the widget.
        val maxRows = ((LocalSize.current.height.value - 75f) / 16f)
            .toInt()
            .coerceIn(3, 12)
        val visibleItems = data.allItems.take(maxRows)

        Column(modifier = GlanceModifier.fillMaxWidth()) {
            if (visibleItems.isEmpty()) {
                Text(
                    text = data.emptyMessage.ifBlank { "Уроков нет" },
                    style = TextStyle(color = WidgetSecondary, fontSize = 11.sp),
                    maxLines = 1
                )
            } else {
                visibleItems.chunked(5).forEachIndexed { chunkIndex, chunk ->
                    if (chunkIndex > 0) LessonDivider()
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        chunk.forEachIndexed { index, item ->
                            if (index > 0) LessonDivider()
                            FullLessonRow(item)
                        }
                    }
                }
            }
        }

        Spacer(GlanceModifier.defaultWeight())
        WidgetFooterRow(footer = data.footer, compact = false)
    }
}

@Composable
private fun LessonDivider() {
    Spacer(
        GlanceModifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(WidgetInnerPlate)
    )
}

@Composable
private fun WidgetHeader(status: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = status,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = WidgetSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        RefreshIcon()
    }
}

@Composable
private fun RefreshIcon() {
    Image(
        provider = ImageProvider(R.drawable.ic_sf_refresh),
        contentDescription = "Обновить",
        modifier = GlanceModifier
            .size(14.dp)
            .clickable(actionRunCallback<ScheduleWidgetRefreshAction>()),
        colorFilter = ColorFilter.tint(WidgetSecondary)
    )
}

@Composable
private fun RoomBadge(room: String, compact: Boolean) {
    Row(
        modifier = GlanceModifier
            .background(WidgetInnerPlate)
            .cornerRadius(8.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_sf_location_fill),
            contentDescription = null,
            modifier = GlanceModifier.size(if (compact) 10.dp else 11.dp),
            colorFilter = ColorFilter.tint(WidgetAccent)
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(
            text = "Кабинет $room",
            style = TextStyle(
                color = WidgetWhite,
                fontSize = if (compact) 9.5.sp else 10.5.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun MiniLessonRow(item: WidgetLesson) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = item.timeStart,
            modifier = GlanceModifier.width(28.dp),
            style = TextStyle(
                color = if (item.isConsultation) WidgetAccent else WidgetWhite,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(
            text = item.title,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = WidgetWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        if (item.room != null) {
            Spacer(GlanceModifier.width(3.dp))
            Image(
                provider = ImageProvider(R.drawable.ic_sf_location_fill),
                contentDescription = null,
                modifier = GlanceModifier.size(8.dp),
                colorFilter = ColorFilter.tint(WidgetAccent)
            )
            Spacer(GlanceModifier.width(1.dp))
            Text(
                text = item.room,
                style = TextStyle(color = WidgetSecondary, fontSize = 8.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FullLessonRow(item: WidgetLesson) {
    val primary = if (item.isPast) WidgetTertiary else WidgetWhite
    val secondary = if (item.isPast) WidgetTertiary else WidgetSecondary
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = item.timeStart,
            modifier = GlanceModifier.width(30.dp),
            style = TextStyle(
                color = if (item.isCurrent) WidgetAccent else primary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        if (item.isConsultation) {
            Image(
                provider = ImageProvider(R.drawable.ic_sf_cap),
                contentDescription = null,
                modifier = GlanceModifier.size(8.5.dp),
                colorFilter = ColorFilter.tint(if (item.isPast) WidgetTertiary else WidgetAccent)
            )
            Spacer(GlanceModifier.width(3.dp))
        }
        Text(
            text = item.title,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = primary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        if (item.room != null) {
            Spacer(GlanceModifier.width(3.dp))
            Image(
                provider = ImageProvider(R.drawable.ic_sf_location_fill),
                contentDescription = null,
                modifier = GlanceModifier.size(9.dp),
                colorFilter = ColorFilter.tint(
                    if (item.isPast) WidgetTertiary else WidgetAccent
                )
            )
            Spacer(GlanceModifier.width(2.dp))
            Text(
                text = item.room,
                style = TextStyle(color = secondary, fontSize = 8.5.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WidgetFooterRow(footer: WidgetFooter, compact: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_sf_cap),
            contentDescription = null,
            modifier = GlanceModifier.size(if (compact) 11.dp else 12.dp),
            colorFilter = ColorFilter.tint(
                if (footer.isConsultation) WidgetAccent else WidgetSecondary
            )
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(
            text = if (compact) footer.compactText else footer.text,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = WidgetSecondary,
                fontSize = if (compact) 9.sp else 10.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

private fun widgetRoot(radius: Dp): GlanceModifier = GlanceModifier
    .fillMaxSize()
    .background(WidgetBackground)
    .cornerRadius(radius)
    .clickable(actionStartActivity<MainActivity>())

private fun widgetCornerRadius(context: Context): Dp {
    val id = context.resources.getIdentifier(
        "system_app_widget_background_radius",
        "dimen",
        "android"
    )
    if (id != 0) {
        val px = runCatching { context.resources.getDimension(id) }.getOrDefault(0f)
        if (px > 0f) return (px / context.resources.displayMetrics.density).dp
    }
    return 22.dp
}

class ScheduleWidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        ScheduleWidget().update(context, glanceId)
    }
}
