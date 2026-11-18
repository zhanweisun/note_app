package app.notedraft.noteall.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.notedraft.noteall.BuildConfig
import app.notedraft.noteall.R
import app.notedraft.noteall.data.AppSettings
import app.notedraft.noteall.ui.MainViewModel
import app.notedraft.noteall.ui.theme.AccentPresets
import kotlin.math.roundToInt

/**
 * 设置页。条目与原始 APK 的 strings.xml（settings_* / common_*）一一对应，
 * 每个开关直接写回 settings 表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.common_close))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { SectionHeader(stringResource(R.string.settings_display_title)) }

            item {
                Text(
                    stringResource(R.string.settings_theme_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    val options = listOf(
                        "System" to stringResource(R.string.settings_theme_system),
                        "Light" to stringResource(R.string.theme_light_mode),
                        "Dark" to stringResource(R.string.theme_dark_mode)
                    )
                    options.forEachIndexed { i, (value, label) ->
                        SegmentedButton(
                            selected = s.theme.equals(value, ignoreCase = true),
                            onClick = { vm.saveSettings(s.copy(theme = value)) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            icon = {}
                        ) {
                            Text(label, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        stringResource(R.string.settings_theme_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("A", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = s.fontScale,
                            onValueChange = { vm.saveSettings(s.copy(fontScale = it)) },
                            valueRange = 0.8f..1.5f,
                            steps = 6,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                        Text("A", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        "${(s.fontScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        stringResource(R.string.settings_writing_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AccentPresets.forEach { (name, argb) ->
                            val selected = (s.accentColor ?: "Default") == name
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .then(
                                        if (selected) Modifier.padding(6.dp) else Modifier
                                    )
                                    .clickable { vm.saveSettings(s.copy(accentColor = name)) }
                            )
                        }
                    }
                }
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_widget_previews)) },
                    supportingContent = { Text(stringResource(R.string.settings_widget_previews_sub)) },
                    trailingContent = {
                        Switch(
                            checked = s.gridView,
                            onCheckedChange = { vm.saveSettings(s.copy(gridView = it)) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_voice_offline_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_voice_offline_sub_off)) },
                    trailingContent = {
                        Switch(
                            checked = s.spellCheck,
                            onCheckedChange = { vm.saveSettings(s.copy(spellCheck = it)) }
                        )
                    }
                )
            }

            item { SectionHeader(stringResource(R.string.settings_security_title)) }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_lock_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_pin_sub_off)) },
                    trailingContent = {
                        Switch(
                            checked = s.lockEnabled,
                            onCheckedChange = { vm.saveSettings(s.copy(lockEnabled = it)) }
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_biometric_title)) },
                    trailingContent = {
                        Switch(
                            checked = s.biometrics,
                            onCheckedChange = { vm.saveSettings(s.copy(biometrics = it)) }
                        )
                    }
                )
            }

            item { SectionHeader(stringResource(R.string.nav_profile)) }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_name_short)) },
                    supportingContent = { Text("${BuildConfig.VERSION_NAME} · Premium") }
                )
            }

            item {
                Text(
                    stringResource(R.string.main_empty_local),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Column {
        Spacer(Modifier.size(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider()
    }
}

fun defaultSettings() = AppSettings.DEFAULT