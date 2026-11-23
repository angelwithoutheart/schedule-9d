package ru.school9d.schedule.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ru.school9d.schedule.MainActivity
import ru.school9d.schedule.alarm.AlarmScheduler
import ru.school9d.schedule.data.ScheduleMerger
import ru.school9d.schedule.data.ScheduleRepository
import java.time.LocalDateTime

private val BackgroundColor = ColorProvider(Color(0xFF161821))
private val PlateColor = ColorProvider(Color(0xFF232736))
private val AccentColor = ColorProvider(Color(0xFF5CA8FF))
private val MutedColor = ColorProvider(Color(0xFF8E92A4))
private val BorderColor = ColorProvider(Color(0xFF2E3346))
private val WhiteColor = ColorProvider(Color.White)

class ScheduleWidget : GlanceAppWidget() {

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
        val data = WidgetDataFactory.build(today, tomorrow, now.toLocalTime())

        runCatching { AlarmScheduler.rescheduleSuspend(context) }

        provideContent {
            WidgetContent(data)
        }
    }
}

@Composable
private fun WidgetContent(data: WidgetUiData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BackgroundColor)
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.Top
    ) {
        Text(
            text = data.status,
            style = TextStyle(
                color = MutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            maxLines = 1
        )

        Spacer(GlanceModifier.height(6.dp))

        Row(
            modifier = GlanceModifier
                .background(PlateColor)
                .cornerRadius(20.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = data.badge,
                style = TextStyle(
                    color = AccentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }

        Spacer(GlanceModifier.defaultWeight())

        Text(
            text = data.subject,
            style = TextStyle(
                color = WhiteColor,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            maxLines = 2
        )

        data.roomLabel?.let { room ->
            Spacer(GlanceModifier.height(10.dp))
            Row(
                modifier = GlanceModifier
                    .background(BorderColor)
                    .cornerRadius(16.dp)
                    .padding(1.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier
                        .background(BackgroundColor)
                        .cornerRadius(15.dp)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "→",
                        style = TextStyle(
                            color = AccentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        text = room,
                        style = TextStyle(
                            color = WhiteColor,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(GlanceModifier.defaultWeight())
    }
}
