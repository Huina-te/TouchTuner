package com.huinatete.touchtuner.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.huinatete.touchtuner.ui.components.BackTitleBar
import com.huinatete.touchtuner.ui.components.OutlineActionButton
import com.huinatete.touchtuner.ui.components.PrimaryActionButton
import com.huinatete.touchtuner.ui.components.SectionCard
import com.huinatete.touchtuner.ui.components.buildAiReportText
import com.huinatete.touchtuner.ui.theme.LocalTouchTunerColors
import com.huinatete.touchtuner.ui.theme.WarningAmber

@Composable
fun SandboxScreen(onBack: () -> Unit) {
    val palette = LocalTouchTunerColors.current
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var report by remember { mutableStateOf<AiReport?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                }.orEmpty()
                when {
                    text.isBlank() -> Toast.makeText(
                        context, "Файл пустой", Toast.LENGTH_SHORT
                    ).show()

                    else -> {
                        input = text.take(200_000)
                        report = null
                        Toast.makeText(
                            context, "Команды импортированы из файла", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(
                    context, "Не удалось прочитать файл", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 24.dp)
    ) {
        BackTitleBar(title = "Песочница", onBack = onBack)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Локальный ИИ-разбор команд на устройстве",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted
        )
        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = WarningAmber.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.28f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Команды не выполняются. ИИ объясняет назначение и риск каждой строки без отправки данных в сеть.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.text
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            placeholder = {
                Text(
                    text = "Вставь команды для предварительной проверки...",
                    color = palette.muted
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 20.sp
            ),
            shape = RoundedCornerShape(18.dp),
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

        Spacer(modifier = Modifier.height(10.dp))

        OutlineActionButton(
            text = "Импорт из txt-файла",
            onClick = { importLauncher.launch("text/plain") },
            icon = Icons.Outlined.UploadFile
        )

        Spacer(modifier = Modifier.height(10.dp))

        PrimaryActionButton(
            text = "ИИ-разбор команд",
            onClick = {
                if (input.isBlank()) {
                    Toast.makeText(context, "Введи команды для проверки", Toast.LENGTH_SHORT).show()
                } else {
                    report = CommandAi.analyze(input)
                }
            },
            icon = Icons.Outlined.Search
        )

        val currentReport = report
        if (currentReport != null) {
            Spacer(modifier = Modifier.height(14.dp))
            SectionCard {
                Text(
                    text = "ИИ-разбор",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = buildAiReportText(
                        report = currentReport,
                        notRecognizedBackground = WarningAmber.copy(alpha = 0.16f)
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    ),
                    color = palette.text
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "ИИ-разбор не гарантирует безопасность команд. Реальный запуск всегда выполняй осознанно.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted
        )
    }
}
