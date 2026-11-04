package com.huinatete.touchtuner.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huinatete.touchtuner.data.AppPreferences
import com.huinatete.touchtuner.data.ThemeColorField
import com.huinatete.touchtuner.data.ThemePalette
import com.huinatete.touchtuner.data.ThemePreset
import com.huinatete.touchtuner.root.ShellManager
import com.huinatete.touchtuner.root.ShellState
import com.huinatete.touchtuner.ui.components.DividerLine
import com.huinatete.touchtuner.ui.screens.CommandsScreen
import com.huinatete.touchtuner.ui.screens.EditorScreen
import com.huinatete.touchtuner.ui.screens.HomeScreen
import com.huinatete.touchtuner.ui.screens.SandboxScreen
import com.huinatete.touchtuner.ui.screens.SettingsScreen
import com.huinatete.touchtuner.ui.screens.ThemeEditorScreen
import com.huinatete.touchtuner.ui.theme.LocalTouchTunerColors
import com.huinatete.touchtuner.ui.theme.TouchTunerTheme
import kotlinx.coroutines.launch

private enum class AppScreen {
    Home,
    Theme,
    Settings,
    Commands,
    Editor,
    Sandbox
}

@Composable
fun TouchTunerApp() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    val shellManager = remember { ShellManager(context) }
    val shellStatus by shellManager.status.collectAsState()

    var palette by remember { mutableStateOf(prefs.loadPalette()) }
    var commands by remember { mutableStateOf(prefs.loadCommands()) }
    var lastResult by remember { mutableStateOf(prefs.loadLastResult()) }
    var screen by rememberSaveable { mutableStateOf(AppScreen.Home) }
    var editorParent by rememberSaveable { mutableStateOf(AppScreen.Home) }
    var runningCommands by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { shellManager.close() }
    }

    LaunchedEffect(Unit) {
        shellManager.refresh()
    }

    val checkAccess: () -> Unit = {
        if (shellStatus.state != ShellState.Checking) {
            scope.launch {
                shellManager.checkOrRequest()
            }
        }
    }

    val executeCommands: () -> Unit = {
        if (!runningCommands && commands.isNotBlank()) {
            runningCommands = true
            val mode = shellManager.mode
            scope.launch {
                val result = shellManager.execute(commands)
                runningCommands = false
                val message = buildString {
                    when {
                        result.timedOut -> append(
                            "Выполнение прервано: ${result.error ?: "таймаут"}"
                        )

                        result.error != null -> append(
                            "Ошибка ${mode.label}: ${result.error}"
                        )

                        else -> append(
                            if (result.output.isBlank()) {
                                "Команды выполнены без вывода."
                            } else {
                                result.output
                            }
                        )
                    }
                    append("\n\nСпособ выполнения: ").append(mode.label)
                    result.exitCode?.let { code ->
                        append("\nКод завершения: ").append(code)
                    }
                }
                lastResult = message
                prefs.saveLastResult(message)
                screen = AppScreen.Home
            }
        }
    }

    val saveCommands: (String) -> Unit = { value ->
        commands = value
        prefs.saveCommands(value)
        Toast.makeText(context, "Команды сохранены", Toast.LENGTH_SHORT).show()
        screen = AppScreen.Commands
    }

    TouchTunerTheme(palette = palette) {
        Scaffold(
            containerColor = LocalTouchTunerColors.current.background,
            bottomBar = {
                if (screen.showsBottomBar()) {
                    TouchTunerBottomBar(
                        current = screen,
                        onSelect = { screen = it }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (screen) {
                    AppScreen.Home -> HomeScreen(
                        status = shellStatus,
                        lastResult = lastResult,
                        onCheckAccess = checkAccess,
                        onOpenCommands = { screen = AppScreen.Commands },
                        onOpenEditor = {
                            editorParent = AppScreen.Home
                            screen = AppScreen.Editor
                        },
                        onOpenSandbox = { screen = AppScreen.Sandbox },
                        onClearResult = {
                            lastResult = ""
                            prefs.clearLastResult()
                        }
                    )

                    AppScreen.Theme -> ThemeEditorScreen(
                        palette = palette,
                        onSelectPreset = { preset: ThemePreset ->
                            palette = preset.toPalette()
                            prefs.savePreset(preset)
                        },
                        onColorChange = { field: ThemeColorField, color ->
                            val updated: ThemePalette = palette.withColor(field, color)
                            palette = updated
                            prefs.saveCustom(updated)
                        },
                        onReset = { palette = prefs.resetPalette() },
                        onBack = { screen = AppScreen.Home }
                    )

                    AppScreen.Settings -> SettingsScreen(
                        executionMode = shellStatus.mode,
                        status = shellStatus,
                        commandCount = commands.lineSequence().count { it.isNotBlank() },
                        onModeChange = { shellManager.setMode(it) },
                        onCheckAccess = checkAccess,
                        onClearCommands = {
                            commands = ""
                            prefs.clearCommands()
                        }
                    )

                    AppScreen.Commands -> CommandsScreen(
                        commands = commands,
                        running = runningCommands,
                        executionMode = shellStatus.mode,
                        onBack = { screen = AppScreen.Home },
                        onEdit = {
                            editorParent = AppScreen.Commands
                            screen = AppScreen.Editor
                        },
                        onRun = executeCommands
                    )

                    AppScreen.Editor -> EditorScreen(
                        initialCommands = commands,
                        onBack = { screen = editorParent },
                        onSave = saveCommands
                    )

                    AppScreen.Sandbox -> SandboxScreen(
                        onBack = { screen = AppScreen.Home }
                    )
                }
            }
        }

        BackHandler(enabled = screen != AppScreen.Home) {
            screen = when (screen) {
                AppScreen.Editor -> editorParent
                else -> AppScreen.Home
            }
        }
    }
}

@Composable
private fun TouchTunerBottomBar(
    current: AppScreen,
    onSelect: (AppScreen) -> Unit
) {
    val palette = LocalTouchTunerColors.current
    Column {
        DividerLine()
        NavigationBar(
            containerColor = palette.background,
            tonalElevation = 0.dp
        ) {
            listOf(
                Triple(AppScreen.Home, "Главная", Icons.Outlined.Home),
                Triple(AppScreen.Theme, "Тема", Icons.Outlined.Palette),
                Triple(AppScreen.Settings, "Настройки", Icons.Outlined.Settings)
            ).forEach { (target, label, icon) ->
                NavigationBarItem(
                    selected = current == target,
                    onClick = { onSelect(target) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = label
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = palette.accent,
                        selectedTextColor = palette.accent,
                        indicatorColor = palette.accentSoft,
                        unselectedIconColor = palette.muted,
                        unselectedTextColor = palette.muted
                    )
                )
            }
        }
    }
}

private fun AppScreen.showsBottomBar(): Boolean = when (this) {
    AppScreen.Home, AppScreen.Theme, AppScreen.Settings -> true
    AppScreen.Commands, AppScreen.Editor, AppScreen.Sandbox -> false
}
