package com.huinatete.touchtuner.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huinatete.touchtuner.root.ShellState
import com.huinatete.touchtuner.root.ShellStatus
import com.huinatete.touchtuner.ui.components.LogoMark
import com.huinatete.touchtuner.ui.components.OutlineActionButton
import com.huinatete.touchtuner.ui.components.SectionCard
import com.huinatete.touchtuner.ui.components.StatusIndicator
import com.huinatete.touchtuner.ui.theme.ErrorRed
import com.huinatete.touchtuner.ui.theme.LocalTouchTunerColors

@Composable
fun HomeScreen(
    status: ShellStatus,
    lastResult: String,
    onCheckAccess: () -> Unit,
    onOpenCommands: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenSandbox: () -> Unit,
    onClearResult: () -> Unit
) {
    val palette = LocalTouchTunerColors.current
    var resultExpanded by rememberSaveable { mutableStateOf(lastResult.isNotBlank()) }

    LaunchedEffect(lastResult) {
        if (lastResult.isNotBlank()) resultExpanded = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoMark()
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "TouchTuner",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                modifier = Modifier.weight(1f)
            )
            StatusIndicator(status = status)
        }

        Spacer(modifier = Modifier.height(18.dp))

        SectionCard {
            Text(
                text = "Доступ к командам",
                style = MaterialTheme.typography.titleLarge,
                color = palette.text
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Способ: ${status.mode.label} · ${status.mode.description}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlineActionButton(
                text = if (status.state == ShellState.Checking) {
                    "Проверка..."
                } else {
                    "Проверить доступ"
                },
                onClick = onCheckAccess,
                icon = Icons.Outlined.Security,
                enabled = status.state != ShellState.Checking
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (status.state == ShellState.Checking) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 1.8.dp,
                            color = palette.accent
                        )
                        Text(
                            text = status.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = status.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor(status),
                            textAlign = TextAlign.Center
                        )
                        if (status.detail.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = status.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (status.hint.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = status.hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionTile(
                title = "Команды",
                subtitle = "Применение",
                icon = Icons.Outlined.Code,
                onClick = onOpenCommands,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                title = "Редактор",
                subtitle = "Изменение команд",
                icon = Icons.Outlined.Edit,
                onClick = onOpenEditor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Песочница",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text
                )
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = palette.muted,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Безопасный разбор команд без запуска и риска для системы",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlineActionButton(
                text = "Открыть песочницу",
                onClick = onOpenSandbox,
                icon = Icons.Outlined.PlayArrow
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(
            modifier = Modifier.animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { resultExpanded = !resultExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Результат выполнения",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text,
                    modifier = Modifier.weight(1f)
                )
                if (lastResult.isNotBlank()) {
                    IconButton(
                        onClick = onClearResult,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Очистить результат",
                            tint = palette.muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Icon(
                    imageVector = if (resultExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = palette.muted,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (resultExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = palette.background,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Text(
                        text = lastResult.ifBlank { "Здесь появится результат команд..." }
                            .take(20_000),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        ),
                        color = if (lastResult.isBlank()) palette.muted else palette.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 76.dp)
                            .padding(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Перед запуском проверь команды. Изменения системных настроек могут повлиять на работу телефона.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted
        )
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTouchTunerColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(128.dp),
        shape = RoundedCornerShape(20.dp),
        color = palette.card,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.text,
                modifier = Modifier.size(26.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted
                )
            }
        }
    }
}

@Composable
private fun statusColor(status: ShellStatus) = when (status.state) {
    ShellState.Ready -> LocalTouchTunerColors.current.accent
    ShellState.Denied, ShellState.Error -> ErrorRed
    else -> LocalTouchTunerColors.current.muted
}
