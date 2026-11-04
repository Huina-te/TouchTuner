package com.huinatete.touchtuner.data

import android.content.Context
import androidx.compose.ui.graphics.toArgb

class AppPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("touchtuner", Context.MODE_PRIVATE)

    fun loadPalette(): ThemePalette {
        val presetId = prefs.getString(KEY_THEME_PRESET, ThemePreset.Green.id)
        if (presetId == CUSTOM_THEME) {
            val fallback = ThemePreset.Green.toPalette()
            return ThemePalette(
                presetId = null,
                background = androidx.compose.ui.graphics.Color(
                    prefs.getInt(KEY_BACKGROUND, fallback.background.toArgb())
                ),
                card = androidx.compose.ui.graphics.Color(
                    prefs.getInt(KEY_CARD, fallback.card.toArgb())
                ),
                accent = androidx.compose.ui.graphics.Color(
                    prefs.getInt(KEY_ACCENT, fallback.accent.toArgb())
                ),
                text = androidx.compose.ui.graphics.Color(
                    prefs.getInt(KEY_TEXT, fallback.text.toArgb())
                )
            )
        }
        return ThemePreset.fromId(presetId).toPalette()
    }

    fun savePreset(preset: ThemePreset) {
        val palette = preset.toPalette()
        prefs.edit()
            .putString(KEY_THEME_PRESET, preset.id)
            .putInt(KEY_BACKGROUND, palette.background.toArgb())
            .putInt(KEY_CARD, palette.card.toArgb())
            .putInt(KEY_ACCENT, palette.accent.toArgb())
            .putInt(KEY_TEXT, palette.text.toArgb())
            .apply()
    }

    fun saveCustom(palette: ThemePalette) {
        prefs.edit()
            .putString(KEY_THEME_PRESET, CUSTOM_THEME)
            .putInt(KEY_BACKGROUND, palette.background.toArgb())
            .putInt(KEY_CARD, palette.card.toArgb())
            .putInt(KEY_ACCENT, palette.accent.toArgb())
            .putInt(KEY_TEXT, palette.text.toArgb())
            .apply()
    }

    fun resetPalette(): ThemePalette {
        savePreset(ThemePreset.Green)
        return ThemePreset.Green.toPalette()
    }

    fun loadCommands(): String = prefs.getString(KEY_COMMANDS, "").orEmpty()

    fun saveCommands(commands: String) {
        prefs.edit().putString(KEY_COMMANDS, commands).apply()
    }

    fun clearCommands() {
        prefs.edit().remove(KEY_COMMANDS).apply()
    }

    fun loadLastResult(): String = prefs.getString(KEY_LAST_RESULT, "").orEmpty()

    fun saveLastResult(result: String) {
        prefs.edit().putString(KEY_LAST_RESULT, result).apply()
    }

    fun clearLastResult() {
        prefs.edit().remove(KEY_LAST_RESULT).apply()
    }

    private companion object {
        const val KEY_THEME_PRESET = "theme_preset"
        const val KEY_BACKGROUND = "theme_background"
        const val KEY_CARD = "theme_card"
        const val KEY_ACCENT = "theme_accent"
        const val KEY_TEXT = "theme_text"
        const val KEY_COMMANDS = "commands"
        const val KEY_LAST_RESULT = "last_result"
        const val CUSTOM_THEME = "custom"
    }
}
