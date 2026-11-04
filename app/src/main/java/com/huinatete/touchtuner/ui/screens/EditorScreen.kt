package com.huinatete.touchtuner.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huinatete.touchtuner.data.AiReport
import com.huinatete.touchtuner.data.CommandAi
import com.huinatete.touchtuner.data.CommandAnalyzer
import com.huinatete.touchtuner.data.ImportResult
import com.huinatete.touchtuner.ui.components.BackTitleBar
import com.huinatete.touchtuner.ui.components.OutlineActionButton
import com.huinatete.touchtuner.ui.components.PrimaryActionButton
import com.huinatete.touchtuner.ui.components.buildAiReportText
import com.huinatete.touchtuner.ui.theme.ErrorRed
import com.huinatete.touchtuner.ui.theme.LocalTouchTunerColors
import com.huinatete.touchtuner.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val MAX_IMPORT_BYTES = 131_072

@Composable
fun EditorScreen(
    initialCommands: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    val palette = LocalTouchTunerColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable(initialCommands) { mutableStateOf(initialCommands) }
    var importPreview by remember { mutableStateOf<ImportResult?>(null) }
    var aiReport by remember { mutableStateOf<AiReport?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val read = withContext(Dispatchers.IO) { readLimitedText(context, uri) }
                read.onSuccess { raw ->
                    val result = CommandAnalyzer.prepareImport(raw)
                    if (result.lines.isEmpty()) {
                        Toast.makeText(
                            context,
                            "В файле не найдено подходящих строк",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        importPreview = result
                    }
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        "Ошибка чтения файла: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        BackTitleBar(title = "Редактор", onBack = onBack)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Одна команда на каждой строке",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = { aiReport = CommandAi.analyze(text) },
                enabled = text.isNotBlank()
            ) {
                Text(text = "ИИ-разбор", color = palette.accent)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = {
                Text(
                    text = "Вставь команды сюда...",
                    color = palette.muted
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 20.sp
            ),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = palette.text,
                unfocusedTextColor = palette.text,
                cursorColor = palette.accent,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.border,
                focusedContainerColor = palette.card,
                unfocusedContainerColor = palette.card,
                focusedPlaceholderColor = palette.muted,
                unfocusedPlaceholderColor = palette.muted
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlineActionButton(
            text = "Импортировать из TXT",
            onClick = { importLauncher.launch(arrayOf("text/plain")) },
            icon = Icons.Outlined.FileOpen
        )
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryActionButton(
            text = "Сохранить команды",
            onClick = { onSave(text.trim()) },
            icon = Icons.Outlined.Save
        )
    }

    val preview = importPreview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = { importPreview = null },
            title = {
                Text(
                    text = "Предпросмотр импорта",
                    color = palette.text
                )
            },
            text = {
                Column {
                    Text(
                        text = "Найдено команд: ${preview.lines.size}\nПропущено строк: ${preview.skippedCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = preview.lines.joinToString("\n").take(1800),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        ),
                        color = palette.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Проверка формата не означает, что команды безопасны.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        text = preview.lines.joinToString("\n")
                        importPreview = null
                    }
                ) {
                    Text(text = "Импортировать", color = palette.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { importPreview = null }) {
                    Text(text = "Отмена", color = palette.muted)
                }
            }
        )
    }

    val reportResult = aiReport
    if (reportResult != null) {
        AlertDialog(
            onDismissRequest = { aiReport = null },
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
                            report = reportResult,
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
                TextButton(onClick = { aiReport = null }) {
                    Text(text = "Понятно", color = palette.accent)
                }
            }
        )
    }
}

private fun readLimitedText(context: Context, uri: Uri): Result<String> = try {
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Не удалось открыть файл")
    input.use { stream ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var total = 0
        while (true) {
            val read = stream.read(buffer)
            if (read == -1) break
            total += read
            if (total > MAX_IMPORT_BYTES) {
                throw IOException("Файл больше 128 КБ")
            }
            output.write(buffer, 0, read)
        }
        Result.success(output.toByteArray().toString(Charsets.UTF_8))
    }
} catch (e: Exception) {
    Result.failure(e)
}
