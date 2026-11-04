package com.huinatete.touchtuner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huinatete.touchtuner.root.ExecutionMode
import com.huinatete.touchtuner.root.ShellState
import com.huinatete.touchtuner.root.ShellStatus
import com.huinatete.touchtuner.ui.components.DividerLine
import com.huinatete.touchtuner.ui.components.IconBadge
import com.huinatete.touchtuner.ui.components.OutlineActionButton
import com.huinatete.touchtuner.ui.components.SectionCard
import com.huinatete.touchtuner.ui.components.StatusIndicator
import com.huinatete.touchtuner.ui.theme.ErrorRed
import com.huinatete.touchtuner.ui.theme.LocalTouchTunerColors

@Composable
fun SettingsScreen(
    executionMode: ExecutionMode,
    status: ShellStatus,
    commandCount: Int,
    onModeChange: (ExecutionMode) -> Unit,
    onCheckAccess: () -> Unit,
    onClearCommands: () -> Unit
) {
    val palette = LocalTouchTunerColors.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 28.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = palette.text
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Параметры TouchTuner",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted
        )

        Spacer(modifier = Modifier.height(18.dp))

        SectionCard {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(
                    icon = Icons.Outlined.Security,
                    size = 46
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Способ выполнения",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = executionMode.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Выбери, откуда приложение получает права на выполнение команд.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted
            )
            Spacer(modifier = Modifier.height(14.dp))

            ExecutionMode.entries.forEachIndexed { index, mode ->
                ModeOption(
                    mode = mode,
                    selected = executionMode == mode,
                    onClick = { onModeChange(mode) }
                )
                if (index != ExecutionMode.entries.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            DividerLine()
            Spacer(modifier = Modifier.height(14.dp))

            StatusIndicator(status = status)
            if (status.detail.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = status.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted
                )
            }
            if (status.hint.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlineActionButton(
                text = if (status.state == ShellState.Checking) "Проверка..." else "Проверить доступ",
                onClick = onCheckAccess,
                icon = Icons.Outlined.Security,
                enabled = status.state != ShellState.Checking
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Режим применяется к следующим запускам команд.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard {
            Text(
                text = "Данные",
                style = MaterialTheme.typography.titleMedium,
                color = palette.text
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Сохранено строк команд: $commandCount",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlineActionButton(
                text = "Очистить команды",
                onClick = { showClearDialog = true },
                icon = Icons.Outlined.DeleteOutline,
                enabled = commandCount > 0,
                contentColor = ErrorRed
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard {
            Text(
                text = "О приложении",
                style = MaterialTheme.typography.titleMedium,
                color = palette.text
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(label = "Версия", value = "1.2")
            Spacer(modifier = Modifier.height(10.dp))
            InfoRow(label = "Пакет", value = "com.huinatete.touchtuner")
            Spacer(modifier = Modifier.height(10.dp))
            InfoRow(label = "Мин. Android", value = "7.0")
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = palette.muted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Проверяй команды перед запуском. Неизвестные команды могут изменить систему.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Очистить команды?",
                    color = palette.text
                )
            },
            text = {
                Text(
                    text = "Сохранённый набор команд будет удалён без возможности восстановления.",
                    color = palette.muted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearCommands()
                    }
                ) {
                    Text(text = "Очистить", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = "Отмена", color = palette.muted)
                }
            }
        )
    }
}

@Composable
private fun ModeOption(
    mode: ExecutionMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalTouchTunerColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) palette.accentSoft else palette.background,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) palette.accent.copy(alpha = 0.45f) else palette.border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = palette.accent,
                    unselectedColor = palette.muted
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (mode) {
                        ExecutionMode.Root -> "Root (su)"
                        ExecutionMode.Shizuku -> "Shizuku"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (mode) {
                        ExecutionMode.Root -> "Команды через суперпользователя (UID 0)."
                        ExecutionMode.Shizuku -> "Через службу Shizuku: ADB (UID 2000) или Root (UID 0)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val palette = LocalTouchTunerColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.text
        )
    }
}
