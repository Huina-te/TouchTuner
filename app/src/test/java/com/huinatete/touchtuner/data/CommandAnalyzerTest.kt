package com.huinatete.touchtuner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandAnalyzerTest {

    @Test
    fun analyze_countsCommandsAndSkipsComments() {
        val result = CommandAnalyzer.analyze(
            """
            # comment
            settings put global window_animation_scale 0

            svc power stayon true
            """.trimIndent()
        )

        assertEquals(2, result.commandCount)
        assertEquals(2, result.skippedCount)
        assertEquals(0, result.longLineCount)
        assertTrue(result.report().contains("Команды не запускались."))
    }

    @Test
    fun analyze_marksLongLinesSeparately() {
        val result = CommandAnalyzer.analyze("echo " + "a".repeat(1200))

        assertEquals(0, result.commandCount)
        assertEquals(1, result.longLineCount)
    }

    @Test
    fun prepareImport_filtersServiceLines() {
        val result = CommandAnalyzer.prepareImport(
            """
            # header
            echo hello
            ${"x".repeat(1200)}

            echo world
            """.trimIndent()
        )

        assertEquals(listOf("echo hello", "echo world"), result.lines)
        assertEquals(3, result.skippedCount)
    }
}
