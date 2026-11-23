package ru.school9d.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import ru.school9d.schedule.ui.MainScreen
import ru.school9d.schedule.ui.MainViewModel
import ru.school9d.schedule.ui.theme.Schedule9DTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.recognizeImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            Schedule9DTheme {
                MainScreen(
                    viewModel = viewModel,
                    onPickImage = { pickImage.launch("image/*") }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDate()
        viewModel.refreshWidget()
    }
}
