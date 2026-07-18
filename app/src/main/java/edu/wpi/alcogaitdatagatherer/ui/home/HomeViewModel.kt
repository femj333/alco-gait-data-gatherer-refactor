package edu.wpi.alcogaitdatagatherer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.wpi.alcogaitdatagatherer.data.SurveyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class HomeViewModel(private val repository: SurveyRepository = SurveyRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val files = repository.getSurveyFiles()
                _uiState.value = HomeUiState.Success(files)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val files: List<File>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
