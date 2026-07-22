package edu.wpi.alcogaitdatagatherer.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import edu.wpi.alcogaitdatagatherer.ui.settings.SettingsScreen
import edu.wpi.alcogaitdatagatherer.ui.settings.SettingsViewModel

class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { finish() }
            )
        }
    }
}
