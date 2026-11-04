package com.huinatete.touchtuner.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.huinatete.touchtuner.data.AiReport

fun buildAiReportText(
    report: AiReport,
    notRecognizedBackground: Color
): AnnotatedString = buildAnnotatedString {
    append("Цель: ")
    append(report.goal)
    append('\n')
    append("Общий риск: ")
    append(report.overallRisk.label)
    append('\n')
    append("Команд: ")
    append(report.commandCount.toString())
    append(" · пропущено строк: ")
    append(report.skippedCount.toString())
    append('\n')
    append("Рабочих: ")
    append(report.workingCount.toString())
    append(" · нерабочих: ")
    append(report.notWorkingCount.toString())
    append('\n')

    if (report.warnings.isNotEmpty()) {
        append('\n')
        report.warnings.forEach { warning ->
            append("! ")
            append(warning)
            append('\n')
        }
    }

    report.insights.forEachIndexed { index, insight ->
        append('\n')
        val lineStart = length
        append((index + 1).toString())
        append(". ")
        append(insight.line.take(180))
        if (!insight.recognized) {
            addStyle(SpanStyle(background = notRecognizedBackground), lineStart, length)
        }
        append('\n')
        append("   ")
        append(insight.category)
        append(" · риск: ")
        append(insight.risk.label)
        if (!insight.recognized) {
            append(" · не распознана")
        } else if (insight.readOnly) {
            append(" · только чтение")
        }
        append('\n')
        append("   ")
        append(insight.purpose)
        if (index != report.insights.lastIndex) append('\n')
    }

    if (report.omittedCount > 0) {
        append('\n')
        append("Ещё команд без разбора: ")
        append(report.omittedCount.toString())
        append('\n')
    }
}
