package com.huinatete.touchtuner.root

enum class ExecutionMode(
    val storageValue: String,
    val label: String,
    val description: String
) {
    Root(
        storageValue = "root",
        label = "Root",
        description = "Команды через su"
    ),
    Shizuku(
        storageValue = "shizuku",
        label = "Shizuku",
        description = "Команды через службу Shizuku"
    );

    companion object {
        fun fromStorage(value: String?): ExecutionMode =
            entries.firstOrNull { it.storageValue == value } ?: Root
    }
}

enum class ShellState {
    Unknown,
    Checking,
    Ready,
    Denied,
    Error
}

data class ShellStatus(
    val mode: ExecutionMode = ExecutionMode.Root,
    val state: ShellState = ShellState.Unknown,
    val title: String = "Не проверено",
    val detail: String = "",
    val hint: String = ""
) {
    val isReady: Boolean get() = state == ShellState.Ready
}

data class ShellResult(
    val output: String,
    val exitCode: Int?,
    val timedOut: Boolean = false,
    val error: String? = null
)
