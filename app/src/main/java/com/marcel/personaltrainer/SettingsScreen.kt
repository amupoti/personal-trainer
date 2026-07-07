package com.marcel.personaltrainer

import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marcel.personaltrainer.model.ReminderSettings
import com.marcel.personaltrainer.model.ThemePreference
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: ReminderSettings,
    themePreference: ThemePreference,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int, LocalTime) -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
) {
    val context = LocalContext.current
    val updateInstaller = remember(context) {
        ApkUpdateInstaller(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        val state = updateState
        if (state is UpdateState.ReadyToInstall && updateInstaller.canInstallPackages()) {
            runCatching { updateInstaller.install(state.apkUri) }
                .onFailure { updateState = UpdateState.DownloadError }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.navigation_settings),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.appearance),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.theme_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ThemePreference.entries.forEachIndexed { index, preference ->
                            SegmentedButton(
                                selected = themePreference == preference,
                                onClick = { onThemePreferenceChange(preference) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ThemePreference.entries.size,
                                ),
                                label = {
                                    Text(
                                        stringResource(
                                            when (preference) {
                                                ThemePreference.LIGHT -> R.string.theme_light
                                                ThemePreference.DARK -> R.string.theme_dark
                                                ThemePreference.SYSTEM -> R.string.theme_system
                                            },
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.daily_reminders),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.daily_reminders_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = onEnabledChange,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    ReminderTimeRow(
                        label = stringResource(R.string.first_reminder),
                        time = settings.firstTime,
                        onTimeChange = { onTimeChange(0, it) },
                    )
                    Spacer(Modifier.height(12.dp))
                    ReminderTimeRow(
                        label = stringResource(R.string.second_reminder),
                        time = settings.secondTime,
                        onTimeChange = { onTimeChange(1, it) },
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.app_updates),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.current_version,
                            BuildConfig.VERSION_NAME,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        enabled = updateState !is UpdateState.Checking &&
                            updateState !is UpdateState.Downloading,
                        onClick = {
                            updateState = UpdateState.Checking
                            scope.launch {
                                updateState = try {
                                    val release = AppUpdateChecker.fetchLatestRelease()
                                    if (
                                        isNewerVersion(
                                            release.versionName,
                                            BuildConfig.VERSION_NAME,
                                        )
                                    ) {
                                        UpdateState.Available(release)
                                    } else {
                                        UpdateState.UpToDate
                                    }
                                } catch (_: Exception) {
                                    UpdateState.CheckError
                                }
                            }
                        },
                    ) {
                        Text(
                            if (updateState is UpdateState.Checking) {
                                stringResource(R.string.checking_for_updates)
                            } else {
                                stringResource(R.string.check_for_updates)
                            },
                        )
                    }
                    when (val state = updateState) {
                        is UpdateState.Available -> {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.update_available, state.release.versionName))
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    updateState = UpdateState.Downloading
                                    scope.launch {
                                        updateState = try {
                                            val apkUri = updateInstaller.download(state.release)
                                            val readyState = UpdateState.ReadyToInstall(apkUri)
                                            if (updateInstaller.canInstallPackages()) {
                                                updateInstaller.install(apkUri)
                                            } else {
                                                installPermissionLauncher.launch(
                                                    updateInstaller.installPermissionIntent(),
                                                )
                                            }
                                            readyState
                                        } catch (_: Exception) {
                                            UpdateState.DownloadError
                                        }
                                    }
                                },
                            ) {
                                Text(stringResource(R.string.download_and_install_update))
                            }
                        }

                        UpdateState.Downloading -> {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.downloading_update))
                        }

                        is UpdateState.ReadyToInstall -> {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.update_downloaded))
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (updateInstaller.canInstallPackages()) {
                                        runCatching { updateInstaller.install(state.apkUri) }
                                            .onFailure {
                                                updateState = UpdateState.DownloadError
                                            }
                                    } else {
                                        installPermissionLauncher.launch(
                                            updateInstaller.installPermissionIntent(),
                                        )
                                    }
                                },
                            ) {
                                Text(stringResource(R.string.install_update))
                            }
                        }

                        UpdateState.UpToDate -> {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.app_is_up_to_date))
                        }

                        UpdateState.CheckError -> {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.update_check_failed))
                        }

                        UpdateState.DownloadError -> {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.update_download_failed))
                        }

                        UpdateState.Checking,
                        UpdateState.Idle,
                        -> Unit
                    }
                }
            }
        }
    }
}

private sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data object CheckError : UpdateState
    data object Downloading : UpdateState
    data object DownloadError : UpdateState
    data class Available(val release: AppRelease) : UpdateState
    data class ReadyToInstall(val apkUri: Uri) : UpdateState
}

@Composable
private fun ReminderTimeRow(
    label: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedButton(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onTimeChange(LocalTime.of(hour, minute)) },
                    time.hour,
                    time.minute,
                    true,
                ).show()
            },
        ) {
            Text(time.format(TIME_FORMATTER))
        }
    }
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
