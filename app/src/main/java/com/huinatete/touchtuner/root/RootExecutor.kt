package com.huinatete.touchtuner.root

import java.io.IOException
import java.util.concurrent.TimeUnit

object RootExecutor {
    private const val CHECK_TIMEOUT_SECONDS = 20L
    private const val EXECUTE_TIMEOUT_SECONDS = 120L
    private const val MAX_OUTPUT = 64_000

    private val suCandidates = listOf(
        "su",
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/debug_ramdisk/su",
        "/data/adb/ksu/bin/su",
        "/data/adb/ap/bin/su"
    )

    fun check(): ShellStatus {
        val result = run("id", CHECK_TIMEOUT_SECONDS)
        return when {
            result.error != null -> ShellStatus(
                mode = ExecutionMode.Root,
                state = ShellState.Error,
                title = "Ошибка Root",
                detail = result.error,
                hint = "Проверь менеджер суперпользователя и разрешение для TouchTuner."
            )

            result.timedOut -> ShellStatus(
                mode = ExecutionMode.Root,
                state = ShellState.Error,
                title = "Root не ответил",
                detail = "Запрос su превысил таймаут.",
                hint = "Подтверди запрос Root в менеджере суперпользователя и повтори."
            )

            result.output.contains("uid=0") -> ShellStatus(
                mode = ExecutionMode.Root,
                state = ShellState.Ready,
                title = "Root активен",
                detail = "Команды выполняются от UID 0.",
                hint = ""
            )

            else -> ShellStatus(
                mode = ExecutionMode.Root,
                state = ShellState.Denied,
                title = "Root недоступен",
                detail = result.output.ifBlank { "su не предоставил доступ." },
                hint = "Выдай Root-доступ приложению в менеджере суперпользователя."
            )
        }
    }

    fun execute(script: String): ShellResult =
        run(script, EXECUTE_TIMEOUT_SECONDS)

    private fun run(script: String, timeoutSeconds: Long): ShellResult {
        val normalized = script.trim()
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        if (normalized.isBlank()) {
            return ShellResult(output = "", exitCode = 0)
        }

        var process: Process? = null
        return try {
            val started = startSu()
            process = started

            val output = StringBuilder()
            val reader = Thread {
                try {
                    started.inputStream.bufferedReader(Charsets.UTF_8).use { stream ->
                        val buffer = CharArray(4096)
                        while (true) {
                            val read = stream.read(buffer)
                            if (read < 0) break
                            synchronized(output) {
                                if (output.length < MAX_OUTPUT) {
                                    output.append(buffer, 0, read)
                                }
                            }
                        }
                    }
                } catch (_: IOException) {
                }
            }
            reader.isDaemon = true
            reader.start()

            try {
                started.outputStream.use { input ->
                    input.write((normalized + "\nexit\n").toByteArray(Charsets.UTF_8))
                    input.flush()
                }
            } catch (_: IOException) {
            }

            val finished = started.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                started.destroy()
                if (started.isAlive) started.destroyForcibly()
            }
            reader.join(1500)

            val text = synchronized(output) { output.toString() }
                .trim()
                .take(MAX_OUTPUT)

            if (!finished) {
                ShellResult(
                    output = text,
                    exitCode = null,
                    timedOut = true,
                    error = "Выполнение превысило $timeoutSeconds секунд"
                )
            } else {
                ShellResult(
                    output = text,
                    exitCode = started.exitValue()
                )
            }
        } catch (e: Exception) {
            ShellResult(
                output = "",
                exitCode = null,
                error = when (e) {
                    is IOException -> "su не найден или запуск запрещён"
                    else -> e.message ?: "Не удалось выполнить команду через su"
                }
            )
        } finally {
            if (process?.isAlive == true) process.destroy()
        }
    }

    private fun startSu(): Process {
        var last: Exception? = null
        for (candidate in suCandidates) {
            try {
                return ProcessBuilder(candidate)
                    .redirectErrorStream(true)
                    .start()
            } catch (e: Exception) {
                last = e
            }
        }
        throw IOException("su не найден", last)
    }
}
