package edu.wpi.alcogaitdatagatherer.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import edu.wpi.alcogaitdatagatherer.R

data class SettingsUiState(
    val wearCollectionEnabled: Boolean = false,
    val boxIntegrationEnabled: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val wearKey = application.getString(R.string.wear_collection_preference)
    private val boxKey = application.getString(R.string.box_integration_preference)

    init {
        _uiState.update { 
            it.copy(
                wearCollectionEnabled = sharedPreferences.getBoolean(wearKey, false),
                boxIntegrationEnabled = sharedPreferences.getBoolean(boxKey, false)
            )
        }
    }

    fun toggleWearCollection(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(wearKey, enabled).apply()
        _uiState.update { it.copy(wearCollectionEnabled = enabled) }
    }

    fun toggleBoxIntegration(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(boxKey, enabled).apply()
        _uiState.update { it.copy(boxIntegrationEnabled = enabled) }
    }
}
