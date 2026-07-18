package edu.wpi.alcogaitdatagatherer.ui.activities

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.wpi.alcogaitdatagatherer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataGatheringScreen(
    viewModel: DataGatheringViewModel,
    onBackClick: () -> Unit,
    onRefreshWatchClick: () -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onRedoSetClick: () -> Unit,
    onRedoLastClick: () -> Unit,
    onFinishClick: () -> Unit,
    isWearableEnabled: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Gait Data") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isWearableEnabled) {
                        IconButton(onClick = onRefreshWatchClick) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Watch")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onRedoSetClick) {
                        Text("REDO SET")
                    }
                    TextButton(onClick = onRedoLastClick) {
                        Text("REDO LAST")
                    }
                    TextButton(onClick = onFinishClick) {
                        Text("P COMPLETE")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.walk_instructions_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.walk_instructions_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = uiState.walkNumberInfo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.bacInput,
                    onValueChange = { viewModel.updateBacInput(it) },
                    label = { Text("Record BAC") },
                    modifier = Modifier.width(150.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.bacError != null,
                    supportingText = {
                        if (uiState.bacError != null) {
                            Text(uiState.bacError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    enabled = !uiState.isRecording
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!uiState.isRecording) {
                    Button(onClick = onStartClick) {
                        Text("START WALK")
                    }
                } else {
                    Text(
                        text = "Recording",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.countdown.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 40.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onStopClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("STOP WALK")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.walkLog.isNotEmpty()) {
                    Text(
                        text = uiState.walkLog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Progress Overlay
            AnimatedVisibility(
                visible = uiState.showProgressBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.progressBarMessage,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
