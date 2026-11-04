package com.huinatete.touchtuner.data

enum class RiskLevel(val label: String, val weight: Int) {
    None("без риска", 0),
    Low("низкий", 1),
    Medium("средний", 2),
    High("высокий", 3),
    Critical("критический", 4),
    Unknown("неизвестный", 2)
}

data class CommandInsight(
    val line: String,
    val category: String,
    val purpose: String,
    val risk: RiskLevel,
    val readOnly: Boolean,
    val recognized: Boolean = true
)

data class AiReport(
    val commandCount: Int,
    val skippedCount: Int,
    val overallRisk: RiskLevel,
    val goal: String,
    val insights: List<CommandInsight>,
    val warnings: List<String>,
    val omittedCount: Int = 0
) {
    val workingCount: Int get() = insights.count { it.recognized }
    val notWorkingCount: Int get() = insights.count { !it.recognized }
}

object CommandAi {
    private const val MAX_INSIGHTS = 60

    fun analyze(raw: String): AiReport {
        val insights = mutableListOf<CommandInsight>()
        var commandCount = 0
        var skippedCount = 0

        raw.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) {
                skippedCount++
                return@forEach
            }

            commandCount++
            if (insights.size < MAX_INSIGHTS) {
                insights += explain(line)
            }
        }

        val overallRisk = insights.maxByOrNull { it.risk.weight }?.risk ?: RiskLevel.None
        val warnings = buildWarnings(insights, raw, commandCount)
        return AiReport(
            commandCount = commandCount,
            skippedCount = skippedCount,
            overallRisk = overallRisk,
            goal = buildGoal(insights),
            insights = insights,
            warnings = warnings,
            omittedCount = (commandCount - insights.size).coerceAtLeast(0)
        )
    }

    private fun explain(line: String): CommandInsight {
        val tokens = line.split(Regex("\\s+"))
        val command = tokens.first().substringAfterLast('/').lowercase()
        val second = tokens.getOrNull(1)?.lowercase().orEmpty()

        return when (command) {
            "settings" -> explainSettings(line, tokens)
            "wm" -> explainWindow(line, tokens)
            "input" -> explainInput(line, tokens)
            "svc" -> explainService(line, second)
            "pm" -> explainPackage(line, second)
            "am" -> explainActivity(line, second)
            "cmd" -> explainCmd(line, second)
            "getprop" -> readOnly(line, "Свойства системы", "Читает системное свойство${argument(tokens, 1)}.")
            "setprop" -> insight(
                line, "Свойства системы",
                "Меняет системное свойство${argument(tokens, 1)}. Неверное значение может нарушить работу системы.",
                RiskLevel.High
            )
            "dumpsys" -> readOnly(line, "Диагностика", "Выводит подробную диагностику системы или приложения.")
            "logcat" -> readOnly(line, "Диагностика", "Показывает системный журнал Android.")
            "cat", "head", "tail", "grep", "find", "ls", "stat", "wc", "df", "du" ->
                readOnly(line, "Файлы", "Читает файлы или выводит информацию о них.")
            "rm" -> {
                val critical = line.contains("-rf") && (line.contains(" /") || line.contains("/*"))
                insight(
                    line, "Файлы",
                    if (critical) {
                        "Рекурсивно удаляет файлы в системном пути. Это может сделать устройство неработоспособным."
                    } else {
                        "Удаляет файлы или каталоги."
                    },
                    if (critical) RiskLevel.Critical else RiskLevel.High
                )
            }
            "mv", "cp" -> insight(line, "Файлы", "Копирует или перемещает файлы.", RiskLevel.Medium)
            "mkdir", "touch", "ln" -> insight(line, "Файлы", "Создаёт файл, каталог или ссылку.", RiskLevel.Low)
            "chmod", "chown" -> insight(
                line, "Файлы",
                "Меняет права доступа или владельца файла. Ошибка может закрыть доступ к системе.",
                RiskLevel.High
            )
            "dd", "mkfs", "wipe", "format" -> insight(
                line, "Файлы",
                "Низкоуровневая запись или форматирование. Может безвозвратно уничтожить данные.",
                RiskLevel.Critical
            )
            "mount", "umount" -> insight(
                line, "Файлы",
                "Подключает или отключает файловую систему.",
                RiskLevel.High
            )
            "reboot", "shutdown", "poweroff", "halt" -> insight(
                line, "Питание",
                "Перезагружает или выключает устройство.",
                RiskLevel.High
            )
            "kill", "killall", "pkill" -> insight(
                line, "Процессы",
                "Завершает процессы. Можно потерять несохранённые данные приложения.",
                RiskLevel.Medium
            )
            "su" -> insight(
                line, "Привилегии",
                "Запускает оболочку или команду с правами суперпользователя.",
                RiskLevel.High
            )
            "sh", "bash" -> insight(
                line, "Скрипты",
                "Запускает shell-скрипт. Проверь, что именно он выполняет.",
                RiskLevel.Medium
            )
            "adb", "fastboot" -> insight(
                line, "Отладка",
                "Команда отладки устройства. Непреднамеренный запуск может изменить систему.",
                RiskLevel.Critical
            )
            "setenforce" -> insight(
                line, "Безопасность",
                "Переключает режим SELinux. Отключение ослабляет защиту Android.",
                RiskLevel.Critical
            )
            "resetprop", "magisk", "ksud", "apd" -> insight(
                line, "Модификации",
                "Меняет системные свойства или настройки root-модификации.",
                RiskLevel.High
            )
            "sendevent" -> insight(
                line, "Ввод",
                "Отправляет низкоуровневое событие ввода. Может имитировать касания и кнопки.",
                RiskLevel.High
            )
            "getevent" -> readOnly(line, "Диагностика", "Показывает события устройств ввода.")
            "uiautomator" -> insight(
                line, "Автоматизация",
                "Управляет интерфейсом через автоматизацию. Может нажимать элементы за тебя.",
                RiskLevel.Medium
            )
            "curl", "wget", "nc", "ping" -> insight(
                line, "Сеть",
                "Выполняет сетевой запрос или проверку соединения.",
                RiskLevel.Medium
            )
            "echo", "printf", "sleep", "true", "false" ->
                insight(line, "Служебные", "Служебная команда без изменения системы.", RiskLevel.None)
            else -> CommandInsight(
                line = line,
                category = "Другое",
                purpose = "Команда не распознана. Проверь её назначение вручную перед запуском.",
                risk = RiskLevel.Unknown,
                readOnly = false,
                recognized = false
            )
        }
    }

    private fun explainSettings(line: String, tokens: List<String>): CommandInsight {
        val action = tokens.getOrNull(1)?.lowercase().orEmpty()
        val namespace = tokens.getOrNull(2).orEmpty()
        val key = tokens.getOrNull(3).orEmpty()
        val value = tokens.drop(4).joinToString(" ")

        return when (action) {
            "get" -> readOnly(
                line,
                "Системные настройки",
                "Читает параметр «$key» из раздела $namespace."
            )
            "put" -> {
                val hint = settingHint(key)
                val purpose = buildString {
                    append("Меняет параметр «").append(key).append("»")
                    if (hint != null) append(" — ").append(hint)
                    append(" в разделе ").append(namespace).append('.')
                    if (value.isNotBlank()) append(" Новое значение: ").append(value.take(80)).append('.')
                }
                insight(line, "Системные настройки", purpose, RiskLevel.Medium)
            }
            "delete" -> insight(
                line, "Системные настройки",
                "Удаляет переопределение параметра «$key» из раздела $namespace.",
                RiskLevel.Medium
            )
            "list" -> readOnly(
                line,
                "Системные настройки",
                "Показывает список параметров раздела $namespace."
            )
            else -> insight(
                line, "Системные настройки",
                "Работает с системными настройками Android.",
                RiskLevel.Unknown
            )
        }
    }

    private fun explainWindow(line: String, tokens: List<String>): CommandInsight {
        return when (tokens.getOrNull(1)?.lowercase()) {
            "size" -> if (tokens.size > 2) {
                insight(line, "Экран", "Задаёт разрешение экрана ${tokens[2]}.", RiskLevel.Medium)
            } else {
                readOnly(line, "Экран", "Показывает текущее разрешение экрана.")
            }
            "density" -> if (tokens.size > 2) {
                insight(line, "Экран", "Задаёт плотность экрана (DPI) ${tokens[2]}.", RiskLevel.Medium)
            } else {
                readOnly(line, "Экран", "Показывает текущую плотность экрана.")
            }
            "reset" -> insight(line, "Экран", "Сбрасывает разрешение и плотность экрана.", RiskLevel.Low)
            else -> insight(line, "Экран", "Управляет параметрами экрана.", RiskLevel.Low)
        }
    }

    private fun explainInput(line: String, tokens: List<String>): CommandInsight {
        val action = tokens.getOrNull(1)?.lowercase().orEmpty()
        val purpose = when (action) {
            "tap" -> "Имитирует касание в точке экрана."
            "swipe" -> "Имитирует свайп по экрану."
            "text" -> "Вводит текст в активное поле."
            "keyevent" -> "Имитирует нажатие кнопки или клавиши."
            "press" -> "Имитирует нажатие и отпускание клавиши."
            else -> "Имитирует действия пользователя на устройстве."
        }
        return insight(line, "Ввод", purpose, RiskLevel.Low)
    }

    private fun explainService(line: String, service: String): CommandInsight {
        return when (service) {
            "power" -> insight(
                line, "Питание",
                "Управляет питанием: например, запрещает экрану гаснуть.",
                RiskLevel.Low
            )
            "data", "wifi", "bluetooth", "usb" -> insight(
                line, "Связь",
                "Включает или выключает модуль связи ($service).",
                RiskLevel.Medium
            )
            else -> insight(line, "Службы Android", "Управляет системной службой Android.", RiskLevel.Medium)
        }
    }

    private fun explainPackage(line: String, action: String): CommandInsight {
        return when (action) {
            "list" -> readOnly(line, "Приложения", "Показывает список установленных приложений.")
            "uninstall", "clear" -> insight(
                line, "Приложения",
                if (action == "clear") "Очищает данные приложения." else "Удаляет приложение.",
                RiskLevel.High
            )
            "disable", "enable", "grant", "revoke" -> insight(
                line, "Приложения",
                "Меняет состояние приложения или его разрешения.",
                RiskLevel.High
            )
            "install" -> insight(line, "Приложения", "Устанавливает приложение из указанного файла.", RiskLevel.Medium)
            else -> insight(line, "Приложения", "Управляет приложениями Android.", RiskLevel.Medium)
        }
    }

    private fun explainActivity(line: String, action: String): CommandInsight {
        return when (action) {
            "start" -> insight(line, "Приложения", "Запускает экран или приложение.", RiskLevel.Low)
            "force-stop", "kill" -> insight(
                line, "Приложения",
                "Принудительно останавливает приложение.",
                RiskLevel.Medium
            )
            "broadcast", "instrument" -> insight(
                line, "Приложения",
                "Отправляет системное сообщение или запускает инструментальный тест.",
                RiskLevel.High
            )
            else -> insight(line, "Приложения", "Управляет активностями Android.", RiskLevel.Medium)
        }
    }

    private fun explainCmd(line: String, subcommand: String): CommandInsight {
        return insight(
            line, "Системные команды",
            "Выполняет системную подкоманду cmd ${subcommand.ifBlank { "..." }}.",
            RiskLevel.Medium
        )
    }

    private fun settingHint(key: String): String? = when (key) {
        "pointer_speed" -> "скорость указателя мыши или тачпада"
        "window_animation_scale", "transition_animation_scale", "animator_duration_scale" ->
            "скорость анимаций интерфейса"
        "tap_duration" -> "длительность касания"
        "multi_press_timeout" -> "интервал мультинажатия"
        "long_press_timeout" -> "задержка долгого нажатия"
        "key_repeat_delay", "key_repeat_timeout" -> "повтор клавиш"
        "touch_blocking_period" -> "защита от случайных касаний"
        "show_touches" -> "отображение точек касания"
        "pointer_location" -> "координаты касаний на экране"
        "haptic_feedback_enabled" -> "тактильная отдача"
        "screen_brightness" -> "яркость экрана"
        "screen_off_timeout" -> "тайм-аут выключения экрана"
        "font_scale" -> "размер шрифта"
        "accelerometer_rotation", "user_rotation" -> "автоповорот и ориентация"
        "default_input_method" -> "клавиатура по умолчанию"
        "enabled_accessibility_services" -> "службы доступности"
        "touch_exploration_enabled" -> "режим исследования касанием"
        else -> null
    }

    private fun buildGoal(insights: List<CommandInsight>): String {
        if (insights.isEmpty()) return "команды не найдены"

        val categories = insights.map { it.category }.distinct()
        val actions = mutableListOf<String>()

        val settings = insights.filter { it.category == "Системные настройки" }
        if (settings.isNotEmpty()) {
            actions += if (settings.any { !it.readOnly }) {
                "изменить системные настройки"
            } else {
                "прочитать системные настройки"
            }
        }

        val screen = insights.filter { it.category == "Экран" }
        if (screen.isNotEmpty()) {
            actions += if (screen.any { !it.readOnly }) {
                "настроить экран"
            } else {
                "проверить параметры экрана"
            }
        }

        val properties = insights.filter { it.category == "Свойства системы" }
        if (properties.isNotEmpty()) {
            actions += if (properties.any { !it.readOnly }) {
                "изменить свойства системы"
            } else {
                "прочитать свойства системы"
            }
        }
        if ("Ввод" in categories) actions += "управлять вводом"
        if ("Питание" in categories || "Связь" in categories) actions += "управлять питанием и связью"
        if ("Приложения" in categories) actions += "управлять приложениями"
        if ("Файлы" in categories) actions += "работать с файлами"
        if ("Диагностика" in categories) actions += "получить диагностику"
        if ("Привилегии" in categories || "Модификации" in categories) actions += "использовать повышенные привилегии"
        if ("Сеть" in categories) actions += "выполнить сетевые действия"
        if ("Автоматизация" in categories) actions += "автоматизировать интерфейс"
        if ("Отладка" in categories) actions += "выполнить команды отладки"
        if ("Безопасность" in categories) actions += "изменить настройки безопасности"
        if ("Процессы" in categories) actions += "управлять процессами"

        val result = if (actions.isEmpty()) {
            "выполнить команды из категорий: ${categories.joinToString(", ")}"
        } else {
            actions.distinct().joinToString(", ")
        }
        return result.replaceFirstChar { it.uppercase() }
    }

    private fun buildWarnings(
        insights: List<CommandInsight>,
        raw: String,
        commandCount: Int
    ): List<String> {
        val warnings = mutableListOf<String>()
        val risk = insights.maxByOrNull { it.risk.weight }?.risk ?: RiskLevel.None

        if (risk == RiskLevel.Critical) {
            warnings += "Есть потенциально разрушительные команды. Не запускай набор без полного понимания."
        } else if (risk == RiskLevel.High) {
            warnings += "Есть команды с высоким риском. Проверь каждую строку."
        }

        if (insights.any { it.risk == RiskLevel.Unknown }) {
            warnings += "Часть команд или действий не распознана — проверь их вручную."
        }

        if (Regex("[;&|<>]").containsMatchIn(raw)) {
            warnings += "Набор содержит составные команды (;, &&, |, >). Проверь все части."
        }

        if (commandCount > MAX_INSIGHTS) {
            warnings += "Показан разбор только первых $MAX_INSIGHTS команд."
        }

        return warnings
    }

    private fun insight(
        line: String,
        category: String,
        purpose: String,
        risk: RiskLevel
    ): CommandInsight = CommandInsight(
        line = line,
        category = category,
        purpose = purpose,
        risk = risk,
        readOnly = false
    )

    private fun readOnly(
        line: String,
        category: String,
        purpose: String
    ): CommandInsight = CommandInsight(
        line = line,
        category = category,
        purpose = purpose,
        risk = RiskLevel.None,
        readOnly = true
    )

    private fun argument(tokens: List<String>, index: Int): String {
        val value = tokens.getOrNull(index)
        return if (value.isNullOrBlank()) "" else " «$value»"
    }
}
