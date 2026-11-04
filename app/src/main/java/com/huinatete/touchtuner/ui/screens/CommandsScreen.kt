package com.huinatete.touchtuner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huinatete.touchtuner.data.CommandAi
import com.huinatete.touchtuner.data.RiskLevel
import com.huinatete.touchtuner.root.ExecutionMode
import com.huinatete.touchtuner.ui.components.BackTitleBar
import com.huinatete.touchtuner.ui.components.OutlineActionButton
import com.huinatete.touchtuner.ui.components.PrimaryActionButton
import com.huinatete.touchtuner.ui.components.buildAiReportText
import com.huinatete.touchtuner.ui.theme.ErrorRed
import com.huinatete.touchtuner.ui.theme.LocalTouchTunerColors
import com.huinatete.touchtuner.ui.theme.WarningAmber

@Composable
fun CommandsScreen(
    commands: String,
    running: Boolean,
    executionMode: ExecutionMode,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRun: () -> Unit
) {
    val palette = LocalTouchTunerColors.current
    var showConfirmation by remember { mutableStateOf(false) }
    var showAiReport by remember { mutableStateOf(false) }
    val report = remember(commands) { CommandAi.analyze(commands) }
    val riskColor = when (report.overallRisk) {
        RiskLevel.High, RiskLevel.Critical -> ErrorRed
        else -> palette.muted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        BackTitleBar(title = "Команды", onBack = onBack)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Сохранённый набор команд",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted
        )
        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            if (commands.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Команды ещё не добавлены. Открой редактор.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    Text(
                        text = commands,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 19.sp
                        ),
                        color = palette.text
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlineActionButton(
            text = "Открыть редактор",
            onClick = onEdit,
            icon = Icons.Outlined.Edit
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlineActionButton(
            text = "ИИ-разбор команд",
            onClick = { showAiReport = true },
            icon = Icons.Outlined.AutoAwesome,
            enabled = commands.isNotBlank()
        )
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryActionButton(
            text = "Выполнить команды",
            onClick = { showConfirmation = true },
            icon = Icons.Outlined.PlayArrow,
            enabled = commands.isNotBlank(),
            loading = running
        )
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = {
                Text(
                    text = "Подтвердить выполнение",
                    color = palette.text
                )
            },
            text = {
                Column {
                    Text(
                        text = "Способ: ${executionMode.label}. Риск набора: ${report.overallRisk.label}. Рабочих команд: ${report.workingCount}, нерабочих: ${report.notWorkingCount}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = riskColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = report.goal,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Команды будут запущены через ${executionMode.label}. Проверь их содержимое. Неизвестные команды могут изменить настройки или работу системы. Продолжить?",
                        color = palette.muted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmation = false
                        onRun()
                    }
                ) {
                    Text(text = "Выполнить", color = palette.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text(text = "Отмена", color = ErrorRed)
                }
            }
        )
    }

    if (showAiReport) {
        AlertDialog(
            onDismissRequest = { showAiReport = false },
            title = {
                Text(
                    text = "ИИ-разбор команд",
                    color = palette.text
                )
            },
            text = {
                Column {
                    Text(
                        text = "Локальный анализ на устройстве. Команды не выполняются.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildAiReportText(
                            report = report,
                            notRecognizedBackground = WarningAmber.copy(alpha = 0.16f)
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        ),
                        color = palette.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiReport = false }) {
                    Text(text = "Понятно", color = palette.accent)
                }
            }
        )
    }
}
