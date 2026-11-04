package com.huinatete.touchtuner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huinatete.touchtuner.data.ThemeColorField
import com.huinatete.touchtuner.data.ThemePalette
import com.huinatete.touchtuner.data.ThemePreset
import com.huinatete.touchtuner.data.parseHexColor
import com.huinatete.touchtuner.data.title
import com.huinatete.touchtuner.data.toHex
import com.huinatete.touchtuner.ui.components.BackTitleBar
import com.huinatete.touchtuner.ui.components.OutlineActionButton
import com.huinatete.touchtuner.ui.theme.ErrorRed
import com.huinatete.touchtuner.ui.theme.LocalTouchTunerColors

@Composable
fun ThemeEditorScreen(
    palette: ThemePalette,
    onSelectPreset: (ThemePreset) -> Unit,
    onColorChange: (ThemeColorField, Color) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val local = LocalTouchTunerColors.current
    var editingField by remember { mutableStateOf<ThemeColorField?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 28.dp)
    ) {
        BackTitleBar(title = "Редактор темы", onBack = onBack)

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Текущая тема: ${palette.title()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.text
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        PreviewCard(palette = palette)

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Выбор темы",
            style = MaterialTheme.typography.titleMedium,
            color = palette.text
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePreset.entries.forEach { preset ->
                ThemeChoice(
                    preset = preset,
                    selected = palette.presetId == preset.id,
                    onSelect = { onSelectPreset(preset) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Настройки темы",
            style = MaterialTheme.typography.titleMedium,
            color = palette.text
        )
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column {
                ThemeColorField.entries.forEachIndexed { index, field ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingField = field }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = field.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.text,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = palette.hexFor(field),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(palette.colorFor(field))
                                .border(1.dp, local.border, RoundedCornerShape(7.dp))
                        )
                    }
                    if (index < ThemeColorField.entries.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(1.dp)
                                .background(palette.border)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlineActionButton(
            text = "Сбросить тему",
            onClick = onReset,
            icon = Icons.Outlined.Refresh,
            contentColor = ErrorRed
        )
    }

    val field = editingField
    if (field != null) {
        ColorPickerDialog(
            title = field.label,
            initial = palette.colorFor(field),
            onDismiss = { editingField = null },
            onApply = { color ->
                onColorChange(field, color)
                editingField = null
            }
        )
    }
}

@Composable
private fun PreviewCard(palette: ThemePalette) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = palette.card,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = palette.text,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TouchTuner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.text,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.accentSoft,
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Root активен",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = palette.background,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Пример карточки",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.text
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Это пример отображения элементов в выбранной теме.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = palette.onAccent
                        )
                    ) {
                        Text(
                            text = "Пример кнопки",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeChoice(
    preset: ThemePreset,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTouchTunerColors.current
    val label = if (preset == ThemePreset.Green) {
        "Зелёная\n(по умолчанию)"
    } else {
        preset.title
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onSelect,
            modifier = Modifier.size(62.dp),
            shape = RoundedCornerShape(18.dp),
            color = palette.card,
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) palette.accent else palette.border
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(preset.accentValue))
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
            color = if (selected) palette.text else palette.muted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    initial: Color,
    onDismiss: () -> Unit,
    onApply: (Color) -> Unit
) {
    val palette = LocalTouchTunerColors.current
    var hex by rememberSaveable(initial) { mutableStateOf(initial.toHex()) }
    val parsed = parseHexColor(hex)
    val isValid = parsed != null

    val quickColors = listOf(
        Color(0xFF0D0F12),
        Color(0xFF16191E),
        Color(0xFF0F9D58),
        Color(0xFF975BDC),
        Color(0xFF3782E6),
        Color(0xFFFF7A1A),
        Color(0xFFE5E5E5),
        Color(0xFF9AA4B2),
        Color(0xFFFF6B6B),
        Color(0xFFF5C542),
        Color(0xFF29B6F6),
        Color(0xFF7ED321)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = palette.card,
        titleContentColor = palette.text,
        textContentColor = palette.muted,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.text
            )
        },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(parsed ?: initial)
                            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { hex = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = !isValid,
                        label = { Text("HEX") },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = palette.text,
                            unfocusedTextColor = palette.text,
                            cursorColor = palette.accent,
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = palette.border,
                            errorBorderColor = ErrorRed,
                            focusedLabelColor = palette.accent,
                            unfocusedLabelColor = palette.muted,
                            errorLabelColor = ErrorRed
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isValid) "Формат: #RRGGBB" else "Неверный формат цвета",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isValid) palette.muted else ErrorRed
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Быстрый выбор",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.muted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickColors.chunked(6).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(1.dp, palette.text.copy(alpha = 0.22f), CircleShape)
                                        .clickable { hex = color.toHex() }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onApply) },
                enabled = isValid
            ) {
                Text(
                    text = "Применить",
                    color = if (isValid) palette.accent else palette.muted
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Отмена", color = palette.muted)
            }
        }
    )
}
