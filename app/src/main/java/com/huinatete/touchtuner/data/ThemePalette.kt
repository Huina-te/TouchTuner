package com.huinatete.touchtuner.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import java.util.Locale

@Immutable
data class ThemePalette(
    val presetId: String?,
    val background: Color,
    val card: Color,
    val accent: Color,
    val text: Color
) {
    val muted: Color get() = text.copy(alpha = 0.62f)
    val border: Color get() = text.copy(alpha = 0.12f)
    val accentSoft: Color get() = accent.copy(alpha = 0.16f)
    val onAccent: Color get() = if (accent.luminance() > 0.55f) Color(0xFF0D0F12) else Color.White

    fun colorFor(field: ThemeColorField): Color = when (field) {
        ThemeColorField.Background -> background
        ThemeColorField.Card -> card
        ThemeColorField.Accent -> accent
        ThemeColorField.Text -> text
    }

    fun withColor(field: ThemeColorField, color: Color): ThemePalette = copy(
        presetId = null,
        background = if (field == ThemeColorField.Background) color else background,
        card = if (field == ThemeColorField.Card) color else card,
        accent = if (field == ThemeColorField.Accent) color else accent,
        text = if (field == ThemeColorField.Text) color else text
    )

    fun hexFor(field: ThemeColorField): String = colorFor(field).toHex()
}

enum class ThemeColorField(val label: String) {
    Background("Основной фон"),
    Card("Карточки"),
    Accent("Акцентный цвет"),
    Text("Текст (основной)")
}

enum class ThemePreset(
    val id: String,
    val title: String,
    val backgroundValue: Long,
    val cardValue: Long,
    val accentValue: Long,
    val textValue: Long
) {
    Green(
        id = "green",
        title = "Зелёная",
        backgroundValue = 0xFF0D0F12,
        cardValue = 0xFF16191E,
        accentValue = 0xFF0F9D58,
        textValue = 0xFFE5E5E5
    ),
    Purple(
        id = "purple",
        title = "Фиолетовая",
        backgroundValue = 0xFF120E19,
        cardValue = 0xFF1F1825,
        accentValue = 0xFF975BDC,
        textValue = 0xFFF2EBFA
    ),
    Blue(
        id = "blue",
        title = "Синяя",
        backgroundValue = 0xFF0A101D,
        cardValue = 0xFF131D30,
        accentValue = 0xFF3782E6,
        textValue = 0xFFE6EEFA
    ),
    Orange(
        id = "orange",
        title = "Оранжевая",
        backgroundValue = 0xFF1A120B,
        cardValue = 0xFF2A1D12,
        accentValue = 0xFFFF7A1A,
        textValue = 0xFFFBEADB
    );

    fun toPalette(): ThemePalette = ThemePalette(
        presetId = id,
        background = Color(backgroundValue),
        card = Color(cardValue),
        accent = Color(accentValue),
        text = Color(textValue)
    )

    companion object {
        fun fromId(id: String?): ThemePreset =
            entries.firstOrNull { it.id == id } ?: Green
    }
}

val DefaultPalette: ThemePalette = ThemePreset.Green.toPalette()

fun ThemePalette.title(): String {
    val preset = ThemePreset.entries.firstOrNull { it.id == presetId }
    return when {
        preset == null -> "Пользовательская"
        preset == ThemePreset.Green -> "Зелёная (по умолчанию)"
        else -> preset.title
    }
}

fun Color.toHex(): String =
    String.format(Locale.US, "#%06x", toArgb() and 0xFFFFFF)

fun parseHexColor(raw: String): Color? {
    val cleaned = raw.trim().removePrefix("#")
    val normalized = when (cleaned.length) {
        6 -> "FF$cleaned"
        8 -> cleaned
        else -> return null
    }
    if (!normalized.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    val value = normalized.toLongOrNull(16) ?: return null
    return Color(value)
}
