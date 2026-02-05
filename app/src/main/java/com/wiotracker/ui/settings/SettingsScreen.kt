package com.wiotracker.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wiotracker.R
import com.wiotracker.data.preferences.AppPreferences
import java.util.Calendar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(
            AppPreferences(LocalContext.current),
            LocalContext.current
        )
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(
                message = stringResource(R.string.save_success),
                duration = SnackbarDuration.Short
            )
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            delay(3000) // Clear error after showing
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // WiFi Name Input
            OutlinedTextField(
                value = uiState.targetWifiName,
                onValueChange = {
                    viewModel.updateTargetWifiName(it)
                    // Clear error when user starts typing
                    if (uiState.wifiNameError != null) {
                        viewModel.clearError()
                    }
                },
                label = { Text(stringResource(R.string.wifi_name)) },
                placeholder = { Text(stringResource(R.string.wifi_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.wifiNameError != null,
                supportingText = uiState.wifiNameError?.let { { Text(it) } }
            )

            // Start Time Picker
            var showStartTimePicker by remember { mutableStateOf(false) }
            val startHour = uiState.startHour

            OutlinedTextField(
                value = String.format("%02d:00", startHour),
                onValueChange = {},
                label = { Text(stringResource(R.string.scan_start_time)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showStartTimePicker = true }) {
                        Text("选择")
                    }
                }
            )

            if (showStartTimePicker) {
                TimePickerDialog(
                    initialHour = startHour,
                    onTimeSelected = { hour ->
                        viewModel.updateStartHour(hour)
                        showStartTimePicker = false
                    },
                    onDismiss = { showStartTimePicker = false }
                )
            }

            // End Time Picker
            var showEndTimePicker by remember { mutableStateOf(false) }
            val endHour = uiState.endHour

            OutlinedTextField(
                value = String.format("%02d:00", endHour),
                onValueChange = {},
                label = { Text(stringResource(R.string.scan_end_time)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                isError = uiState.timeRangeError != null,
                supportingText = if (uiState.timeRangeError != null) {
                    { Text(uiState.timeRangeError!!) }
                } else null,
                trailingIcon = {
                    IconButton(onClick = { showEndTimePicker = true }) {
                        Text(stringResource(R.string.select_time))
                    }
                }
            )

            if (showEndTimePicker) {
                TimePickerDialog(
                    initialHour = endHour,
                    onTimeSelected = { hour ->
                        viewModel.updateEndHour(hour)
                        showEndTimePicker = false
                    },
                    onDismiss = { showEndTimePicker = false }
                )
            }

            // Error message display
            if (uiState.errorMessage != null && uiState.wifiNameError == null && uiState.timeRangeError == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Save Button
            Button(
                onClick = viewModel::saveSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isSaving && uiState.targetWifiName.isNotBlank()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    initialHour: Int,
    onTimeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("小时: $selectedHour")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        if (selectedHour > 0) selectedHour--
                    }) {
                        Text("-")
                    }
                    Text(
                        text = String.format("%02d:00", selectedHour),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Button(onClick = {
                        if (selectedHour < 23) selectedHour++
                    }) {
                        Text("+")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onTimeSelected(selectedHour) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
