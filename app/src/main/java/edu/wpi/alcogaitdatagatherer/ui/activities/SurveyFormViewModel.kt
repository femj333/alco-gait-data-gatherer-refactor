package edu.wpi.alcogaitdatagatherer.ui.activities

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import edu.wpi.alcogaitdatagatherer.data.SurveyRepository
import edu.wpi.alcogaitdatagatherer.models.Gender
import edu.wpi.alcogaitdatagatherer.models.TestSubject

data class SurveyFormUiState(
    val subjectID: String = "",
    val gender: Gender? = null,
    val age: String = "",
    val weight: String = "",
    val heightFeet: String = "",
    val heightInches: String = "",
    val subjectIDError: String? = null,
    val ageError: String? = null,
    val weightError: String? = null,
    val heightFeetError: String? = null,
    val heightInchesError: String? = null,
    val isFormValid: Boolean = false
)

class SurveyFormViewModel : ViewModel() {
    private val repository = SurveyRepository()

    private val _uiState = MutableStateFlow(SurveyFormUiState())
    val uiState: StateFlow<SurveyFormUiState> = _uiState.asStateFlow()

    fun onSubjectIDChange(id: String) {
        val existingIds = repository.getExistingIds()
        val error = when {
            id.isEmpty() -> "Please enter data"
            existingIds.contains(id.trim().replaceFirst("^0+(?!$)", "")) -> "This ID already exists."
            id.toIntOrNull()?.let { it < 101 || it > 996 } ?: true -> "Subject ID has to be between 101 and 996"
            else -> null
        }
        _uiState.update { it.copy(subjectID = id, subjectIDError = error) }
        validateForm()
    }

    fun onGenderChange(gender: Gender) {
        _uiState.update { it.copy(gender = gender) }
        validateForm()
    }

    fun onAgeChange(age: String) {
        val error = when {
            age.isEmpty() -> "Please enter data"
            age.toIntOrNull()?.let { it < 21 || it > 65 } ?: true -> "Invalid. 21 Minimum. 65 Maximum."
            else -> null
        }
        _uiState.update { it.copy(age = age, ageError = error) }
        validateForm()
    }

    fun onWeightChange(weight: String) {
        val weightVal = if (weight.startsWith(".")) "0$weight" else weight
        val error = when {
            weight.isEmpty() -> "Please enter data"
            weightVal.toDoubleOrNull()?.let { it < 85 || it > 230 } ?: true -> "Invalid. 85 Minimum. 230 Maximum."
            else -> null
        }
        _uiState.update { it.copy(weight = weight, weightError = error) }
        validateForm()
    }

    fun onHeightFeetChange(feet: String) {
        val error = when {
            feet.isEmpty() -> "Please enter data"
            feet.toIntOrNull()?.let { it < 4 || it > 7 } ?: true -> "Invalid. 4 Feet Minimum. 7 Feet Maximum."
            else -> null
        }
        _uiState.update { it.copy(heightFeet = feet, heightFeetError = error) }
        validateForm()
    }

    fun onHeightInchesChange(inches: String) {
        val error = when {
            inches.isEmpty() -> "Please enter data"
            inches.toIntOrNull()?.let { it > 11 } ?: true -> "Invalid. 11 Inches Maximum"
            else -> null
        }
        _uiState.update { it.copy(heightInches = inches, heightInchesError = error) }
        validateForm()
    }

    private fun validateForm() {
        val state = _uiState.value
        val isValid = state.subjectID.isNotEmpty() && state.subjectIDError == null &&
                state.gender != null &&
                state.age.isNotEmpty() && state.ageError == null &&
                state.weight.isNotEmpty() && state.weightError == null &&
                state.heightFeet.isNotEmpty() && state.heightFeetError == null &&
                state.heightInches.isNotEmpty() && state.heightInchesError == null
        
        _uiState.update { it.copy(isFormValid = isValid) }
    }

    fun createTestSubject(): TestSubject? {
        val state = _uiState.value
        if (!state.isFormValid) return null
        
        val formattedID = String.format("%03d", state.subjectID.trim().toInt())
        val weightVal = if (state.weight.startsWith(".")) "0${state.weight}" else state.weight
        
        return TestSubject(
            formattedID,
            state.gender!!,
            state.age.toInt(),
            weightVal.toDouble(),
            state.heightFeet.toInt(),
            state.heightInches.toInt()
        )
    }
}
