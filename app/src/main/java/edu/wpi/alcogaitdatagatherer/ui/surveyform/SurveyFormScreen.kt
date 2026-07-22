package edu.wpi.alcogaitdatagatherer.ui.surveyform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import edu.wpi.alcogaitdatagatherer.models.Gender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyFormScreen(
    viewModel: SurveyFormViewModel,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Test Subject") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.subjectID,
                onValueChange = viewModel::onSubjectIDChange,
                label = { Text("Subject ID") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.subjectIDError != null,
                supportingText = { uiState.subjectIDError?.let { Text(it) } }
            )

            Text("Gender", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.gender == Gender.MALE,
                    onClick = { viewModel.onGenderChange(Gender.MALE) }
                )
                Text("Male")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = uiState.gender == Gender.FEMALE,
                    onClick = { viewModel.onGenderChange(Gender.FEMALE) }
                )
                Text("Female")
            }

            OutlinedTextField(
                value = uiState.age,
                onValueChange = viewModel::onAgeChange,
                label = { Text("Age") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.ageError != null,
                supportingText = { uiState.ageError?.let { Text(it) } }
            )

            OutlinedTextField(
                value = uiState.weight,
                onValueChange = viewModel::onWeightChange,
                label = { Text("Weight (lbs)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.weightError != null,
                supportingText = { uiState.weightError?.let { Text(it) } }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = uiState.heightFeet,
                    onValueChange = viewModel::onHeightFeetChange,
                    label = { Text("Height (ft)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.heightFeetError != null,
                    supportingText = { uiState.heightFeetError?.let { Text(it) } }
                )
                OutlinedTextField(
                    value = uiState.heightInches,
                    onValueChange = viewModel::onHeightInchesChange,
                    label = { Text("Height (in)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.heightInchesError != null,
                    supportingText = { uiState.heightInchesError?.let { Text(it) } }
                )
            }

            Button(
                onClick = onSubmitClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isFormValid
            ) {
                Text("SUBMIT")
            }
        }
    }
}
