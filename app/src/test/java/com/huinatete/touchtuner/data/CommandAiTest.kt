package com.huinatete.touchtuner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandAiTest {

    @Test
    fun recognizesPointerSpeedSetting() {
        val report = CommandAi.analyze("settings put system pointer_speed 7")

        assertEquals(1, report.commandCount)
        assertEquals(RiskLevel.Medium, report.overallRisk)
        assertTrue(report.insights.first().purpose.contains("указател"))
    }

    @Test
    fun marksDestructiveCommandAsCritical() {
        val report = CommandAi.analyze("rm -rf /")

        assertEquals(RiskLevel.Critical, report.overallRisk)
    }

    @Test
    fun readOnlyCommandsHaveNoRisk() {
        val report = CommandAi.analyze(
            "getprop ro.build.version.release\nsettings get system pointer_speed"
        )

        assertEquals(RiskLevel.None, report.overallRisk)
        assertTrue(report.insights.all { it.readOnly })
    }

    @Test
    fun buildsGoalAndSkipsComments() {
        val report = CommandAi.analyze(
            "# настройка\nsettings put system pointer_speed 7\n\nwm density 420"
        )

        assertEquals(2, report.commandCount)
        assertEquals(2, report.skippedCount)
        assertTrue(report.goal.contains("настройки"))
        assertTrue(report.goal.contains("экран"))
    }

    @Test
    fun readOnlyGoalUsesReadVerbs() {
        val report = CommandAi.analyze(
            "settings get system pointer_speed\nwm size\ngetprop ro.build.version.release"
        )

        assertEquals(RiskLevel.None, report.overallRisk)
        assertTrue(report.goal.contains("прочитать"))
        assertTrue(report.goal.contains("проверить"))
    }

    @Test
    fun unknownCommandIsMarkedUnknown() {
        val report = CommandAi.analyze("totally_unknown_command --flag")

        assertEquals(RiskLevel.Unknown, report.overallRisk)
        assertTrue(report.warnings.any { it.contains("не распознана") })
    }
}
