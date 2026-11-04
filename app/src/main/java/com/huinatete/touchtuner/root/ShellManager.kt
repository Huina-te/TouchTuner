package com.huinatete.touchtuner.root

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class ShellManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("touchtuner", Context.MODE_PRIVATE)
    private val shizuku = ShizukuShell(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var mode: ExecutionMode = ExecutionMode.fromStorage(
        prefs.getString(KEY_MODE, null)
    )
        private set

    private val _status = MutableStateFlow(ShellStatus(mode = mode))
    val status: StateFlow<ShellStatus> = _status.asStateFlow()

    private val binderReceived = Shizuku.OnBinderReceivedListener {
        if (mode == ExecutionMode.Shizuku) {
            scope.launch { refresh() }
        }
    }

    private val binderDead = Shizuku.OnBinderDeadListener {
        shizuku.reset()
        if (mode == ExecutionMode.Shizuku) {
            _status.value = ShellStatus(
                mode = ExecutionMode.Shizuku,
                state = ShellState.Denied,
                title = "Shizuku остановлен",
                detail = "Служба Shizuku больше не доступна.",
                hint = "Запусти Shizuku снова и повтори проверку."
            )
        }
    }

    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (mode == ExecutionMode.Shizuku) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                scope.launch { refresh() }
            } else {
                _status.value = ShellStatus(
                    mode = ExecutionMode.Shizuku,
                    state = ShellState.Denied,
                    title = "Разрешение не выдано",
                    detail = "Shizuku не получил разрешение от пользователя.",
                    hint = "Нажми «Проверить доступ» и подтверди запрос."
                )
            }
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
    }

    fun close() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
        shizuku.unbind()
        scope.cancel()
    }

    suspend fun refresh() {
        val currentMode = mode
        _status.value = ShellStatus(
            mode = currentMode,
            state = ShellState.Checking,
            title = if (currentMode == ExecutionMode.Root) {
                "Проверка Root..."
            } else {
                "Проверка Shizuku..."
            },
            detail = "",
            hint = ""
        )

        val result = when (currentMode) {
            ExecutionMode.Root -> withContext(Dispatchers.IO) { RootExecutor.check() }
            ExecutionMode.Shizuku -> checkShizuku()
        }
        if (mode == currentMode) {
            _status.value = result
        }
    }

    suspend fun checkOrRequest() {
        if (mode == ExecutionMode.Root) {
            refresh()
            return
        }

        if (!shizuku.isBinderAlive()) {
            _status.value = shizuku.status()
            return
        }

        if (!shizuku.hasPermission()) {
            _status.value = ShellStatus(
                mode = ExecutionMode.Shizuku,
                state = ShellState.Checking,
                title = "Ожидание разрешения Shizuku",
                detail = "Подтверди доступ в приложении Shizuku.",
                hint = ""
            )
            val result = shizuku.requestPermission()
            if (result.isFailure) {
                _status.value = ShellStatus(
                    mode = ExecutionMode.Shizuku,
                    state = ShellState.Error,
                    title = "Ошибка Shizuku",
                    detail = result.exceptionOrNull()?.message ?: "Не удалось запросить разрешение",
                    hint = "Проверь, что Shizuku запущен и обновлён."
                )
            }
            return
        }

        refresh()
    }

    fun setMode(newMode: ExecutionMode) {
        if (newMode == mode) return
        mode = newMode
        prefs.edit().putString(KEY_MODE, newMode.storageValue).apply()

        if (newMode == ExecutionMode.Root) {
            shizuku.unbind()
            _status.value = ShellStatus(mode = newMode)
            scope.launch { refresh() }
            return
        }

        _status.value = ShellStatus(mode = newMode)
        if (!shizuku.isBinderAlive()) {
            _status.value = shizuku.status()
            return
        }

        if (!shizuku.hasPermission()) {
            _status.value = ShellStatus(
                mode = ExecutionMode.Shizuku,
                state = ShellState.Checking,
                title = "Ожидание разрешения Shizuku",
                detail = "Подтверди доступ в приложении Shizuku.",
                hint = ""
            )
            val result = shizuku.requestPermission()
            if (result.isFailure) {
                _status.value = ShellStatus(
                    mode = ExecutionMode.Shizuku,
                    state = ShellState.Error,
                    title = "Ошибка Shizuku",
                    detail = result.exceptionOrNull()?.message ?: "Не удалось запросить разрешение",
                    hint = "Проверь, что Shizuku запущен и обновлён."
                )
            }
            return
        }

        scope.launch { refresh() }
    }

    fun requestShizukuPermission() {
        if (mode != ExecutionMode.Shizuku) return
        if (!shizuku.isBinderAlive() || shizuku.hasPermission()) return
        shizuku.requestPermission()
    }

    suspend fun execute(script: String): ShellResult = when (mode) {
        ExecutionMode.Root -> withContext(Dispatchers.IO) { RootExecutor.execute(script) }
        ExecutionMode.Shizuku -> shizuku.execute(script)
    }

    private suspend fun checkShizuku(): ShellStatus {
        val base = shizuku.status()
        if (base.state != ShellState.Checking) return base

        val service = shizuku.connect()
        return if (service == null) {
            ShellStatus(
                mode = ExecutionMode.Shizuku,
                state = ShellState.Error,
                title = "Shizuku не отвечает",
                detail = "Не удалось подключить службу выполнения команд.",
                hint = "Проверь, что Shizuku запущен, и повтори проверку."
            )
        } else {
            shizuku.status()
        }
    }

    private companion object {
        const val KEY_MODE = "execution_mode"
    }
}
