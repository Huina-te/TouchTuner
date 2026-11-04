package com.huinatete.touchtuner.root

import com.huinatete.touchtuner.IUserService
import java.io.IOException
import java.util.concurrent.TimeUnit

class ShellUserService : IUserService.Stub() {
    @Volatile
    private var lastCode: Int = -1

    override fun run(script: String): String {
        var process: Process? = null
        return try {
            val started = ProcessBuilder("sh")
                .redirectErrorStream(true)
                .start()
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
                val normalized = script.trim()
                    .removePrefix("\uFEFF")
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                started.outputStream.use { input ->
                    input.write((normalized + "\nexit\n").toByteArray(Charsets.UTF_8))
                    input.flush()
                }
            } catch (_: IOException) {
            }

            val finished = started.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                started.destroy()
                if (started.isAlive) started.destroyForcibly()
            }
            reader.join(1500)

            lastCode = if (finished) started.exitValue() else -1
            synchronized(output) { output.toString() }
                .trim()
                .take(MAX_OUTPUT)
        } catch (e: Exception) {
            lastCode = -1
            "Ошибка Shizuku: ${e.message ?: "выполнение не удалось"}"
        } finally {
            if (process?.isAlive == true) process.destroy()
        }
    }

    override fun getLastExitCode(): Int = lastCode

    override fun destroy() {
        System.exit(0)
    }

    private companion object {
        const val TIMEOUT_SECONDS = 120L
        const val MAX_OUTPUT = 64_000
    }
}
