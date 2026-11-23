package ru.school9d.schedule.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import ru.school9d.schedule.ui.theme.AppPalette
import ru.school9d.schedule.ui.theme.Schedule9DTheme

class ShareReceiverActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setFinishOnTouchOutside(false)

        viewModel.start(extractImageUri(intent))

        setContent {
            Schedule9DTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                ShareDialog(state = state, onClose = { finish() })
            }
        }
    }

    private fun extractImageUri(intent: Intent?): Uri? {
        if (intent == null) return null
        val stream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        return stream ?: intent.clipData
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.uri
    }
}

@Composable
private fun ShareDialog(state: ShareUiState, onClose: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(state.successMessage) {
        val message = state.successMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        delay(900)
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPalette.Background.copy(alpha = 0.18f))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(24.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppPalette.Border, shape),
            shape = shape,
            color = AppPalette.Surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    state.error != null -> {
                        Text(
                            text = "НЕ УДАЛОСЬ РАСПОЗНАТЬ",
                            color = AppPalette.Danger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = state.error,
                            color = AppPalette.TextPrimary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppPalette.Accent,
                                contentColor = AppPalette.DarkText
                            )
                        ) {
                            Text("Закрыть", fontWeight = FontWeight.Bold)
                        }
                    }

                    state.successMessage != null -> {
                        Text(
                            text = "ГОТОВО",
                            color = AppPalette.Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = state.successMessage,
                            color = AppPalette.TextPrimary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> {
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
                            text = "Читаю колонку 9Д через Vision-модель OpenRouter. Обычно 10–30 секунд.",
                            color = AppPalette.TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
