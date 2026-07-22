package edu.wpi.alcogaitdatagatherer.ui.datagathering

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import edu.wpi.alcogaitdatagatherercommon.CommonCode

data class DataGatheringUiState(
    val countdown: Int = CommonCode.RECORD_TIME_IN_SECONDS,
    val walkNumberInfo: String = "Walk Number 0 : Normal Walk",
    val bacInput: String = "",
    val isRecording: Boolean = false,
    val isCountingDown: Boolean = false,
    val walkLog: String = "",
    val showProgressBar: Boolean = false,
    val progressBarMessage: String = "Connecting to watch...",
    val isWearableConnected: Boolean = false,
    val bacError: String? = null
)

class DataGatheringViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DataGatheringUiState())
    val uiState: StateFlow<DataGatheringUiState> = _uiState.asStateFlow()

    fun updateCountdown(seconds: Int) {
        _uiState.update { it.copy(countdown = seconds) }
    }

    fun updateWalkNumberInfo(info: String) {
        _uiState.update { it.copy(walkNumberInfo = info) }
    }

    fun updateBacInput(input: String) {
        var error: String? = null
        if (input.isNotEmpty()) {
            val bac = input.toDoubleOrNull()
            if (bac != null && bac > 5.0) {
                error = "Invalid. 5.0 Maximum"
            }
        }
        _uiState.update { it.copy(bacInput = input, bacError = error) }
    }

    fun setRecording(recording: Boolean) {
        _uiState.update { it.copy(isRecording = recording) }
    }

    fun setCountingDown(countingDown: Boolean) {
        _uiState.update { it.copy(isCountingDown = countingDown) }
    }

    fun updateWalkLog(log: String) {
        _uiState.update { it.copy(walkLog = log) }
    }

    fun setShowProgressBar(show: Boolean, message: String = "Connecting to watch...") {
        _uiState.update { it.copy(showProgressBar = show, progressBarMessage = message) }
    }

    fun setWearableConnected(connected: Boolean) {
        _uiState.update { it.copy(isWearableConnected = connected) }
    }
}
