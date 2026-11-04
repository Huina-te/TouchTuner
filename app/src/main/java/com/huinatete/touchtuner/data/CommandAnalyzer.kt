package com.huinatete.touchtuner.data

data class CommandAnalysis(
    val commandCount: Int,
    val skippedCount: Int,
    val longLineCount: Int,
    val preview: String
) {
    fun report(): String = buildString {
        append("Строк с командами: ").append(commandCount).append('\n')
        append("Пропущено пустых и комментариев: ").append(skippedCount).append('\n')
        append("Слишком длинных строк: ").append(longLineCount).append('\n')
        append('\n')
        append(if (preview.isBlank()) "Команды не найдены." else "Предпросмотр:\n$preview")
        append('\n')
        append("Команды не запускались.")
    }
}

data class ImportResult(
    val lines: List<String>,
    val skippedCount: Int
)

object CommandAnalyzer {
    private val controlCharacters = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]")
    private const val MAX_LINE_LENGTH = 1000
    private const val MAX_PREVIEW_LENGTH = 2000

    fun analyze(raw: String): CommandAnalysis {
        var commandCount = 0
        var skippedCount = 0
        var longLineCount = 0
        val preview = StringBuilder()

        raw.lineSequence().forEach { line ->
            val item = line.trim()
            when {
                item.isEmpty() || item.startsWith("#") -> skippedCount++
                item.length > MAX_LINE_LENGTH || controlCharacters.containsMatchIn(item) -> longLineCount++
                else -> {
                    commandCount++
                    if (preview.length < MAX_PREVIEW_LENGTH) {
                        preview.append("• ").append(item).append('\n')
                    }
                }
            }
        }

        return CommandAnalysis(
            commandCount = commandCount,
            skippedCount = skippedCount,
            longLineCount = longLineCount,
            preview = preview.toString().trimEnd()
        )
    }

    fun prepareImport(raw: String): ImportResult {
        var skippedCount = 0
        val lines = mutableListOf<String>()

        raw.lineSequence().forEach { line ->
            val item = line.trim()
            when {
                item.isEmpty() || item.startsWith("#") -> skippedCount++
                item.length > MAX_LINE_LENGTH || controlCharacters.containsMatchIn(item) -> skippedCount++
                else -> lines += item
            }
        }

        return ImportResult(lines = lines, skippedCount = skippedCount)
    }
}
