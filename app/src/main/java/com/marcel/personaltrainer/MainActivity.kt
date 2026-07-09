package com.marcel.personaltrainer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marcel.personaltrainer.data.ProgressRepository
import com.marcel.personaltrainer.model.ThemePreference
import com.marcel.personaltrainer.ui.ProgressUiState
import com.marcel.personaltrainer.ui.ProgressViewModel
import com.marcel.personaltrainer.ui.ProgressViewModelFactory
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    private val viewModel: ProgressViewModel by viewModels {
        ProgressViewModelFactory(
            repository = ProgressRepository(applicationContext),
            reminderScheduler = ReminderScheduler(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AppTheme(themePreference = state.themePreference) {
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        viewModel.setRemindersEnabled(true)
                    }
                }
                ReusableApp(
                    state = state,
                    onRemindersEnabledChange = { enabled ->
                        if (
                            !enabled ||
                            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.setRemindersEnabled(enabled)
                        } else {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS,
                            )
                        }
                    },
                    onReminderTimeChange = viewModel::setReminderTime,
                    onThemePreferenceChange = viewModel::setThemePreference,
                )
            }
        }
    }
}

@Composable
private fun AppTheme(
    themePreference: ThemePreference,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF81D5C7),
            onPrimary = Color(0xFF003730),
            primaryContainer = Color(0xFF005047),
            onPrimaryContainer = Color(0xFF9EF2E2),
            secondary = Color(0xFFB1CCC5),
            secondaryContainer = Color(0xFF334B46),
            onSecondaryContainer = Color(0xFFCDE8E1),
            background = Color(0xFF0E1513),
            surface = Color(0xFF0E1513),
            surfaceVariant = Color(0xFF3F4946),
            outline = Color(0xFF89938F),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF006B5F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF9EF2E2),
            onPrimaryContainer = Color(0xFF00201C),
            secondary = Color(0xFF4A635E),
            secondaryContainer = Color(0xFFCDE8E1),
            onSecondaryContainer = Color(0xFF06201B),
            background = Color(0xFFF5FBF8),
            surface = Color(0xFFF5FBF8),
            surfaceVariant = Color(0xFFDAE5E1),
            outline = Color(0xFF6F7976),
        )
    }
    val systemBarColor = Color.Transparent.toArgb()
    val activity = LocalContext.current as? ComponentActivity
    SideEffect {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = systemBarColor,
                darkScrim = systemBarColor,
                detectDarkMode = { darkTheme },
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = systemBarColor,
                darkScrim = systemBarColor,
                detectDarkMode = { darkTheme },
            ),
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            headlineLarge = Typography().headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            headlineMedium = Typography().headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
        content = content,
    )
}

@Composable
private fun ReusableApp(
    state: ProgressUiState,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, LocalTime) -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
) {
    var selectedView by rememberSaveable { mutableIntStateOf(0) }
    val destinations = listOf(
        Pair(stringResource(R.string.navigation_home), Icons.Rounded.Home),
        Pair(stringResource(R.string.navigation_settings), Icons.Rounded.Settings),
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedView == index,
                        onClick = { selectedView = index },
                        icon = {
                            Icon(
                                imageVector = destination.second,
                                contentDescription = destination.first,
                            )
                        },
                        label = { Text(destination.first) },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (selectedView == 0) {
                HomeScreen()
            } else {
                SettingsScreen(
                    settings = state.reminderSettings,
                    themePreference = state.themePreference,
                    onEnabledChange = onRemindersEnabledChange,
                    onTimeChange = onReminderTimeChange,
                    onThemePreferenceChange = onThemePreferenceChange,
                )
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_description),
                        style = MaterialTheme.typography.bodyLarge,
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
                        text = stringResource(R.string.home_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.home_empty_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AppTheme(themePreference = ThemePreference.LIGHT) {
        HomeScreen()
    }
}
