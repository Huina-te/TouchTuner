package com.huinatete.touchtuner.root

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.huinatete.touchtuner.IUserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class ShizukuShell(context: Context) {
    private val appContext = context.applicationContext
    private val waiters = CopyOnWriteArrayList<kotlinx.coroutines.CancellableContinuation<IUserService>>()

    private var remoteService: IUserService? = null
    private var binding = false

    private val arguments = Shizuku.UserServiceArgs(
        ComponentName(appContext.packageName, ShellUserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shell")
        .tag("touchtuner-shell")
        .version(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binding = false
            val binder = service?.let { IUserService.Stub.asInterface(it) }
            if (binder != null) {
                remoteService = binder
                waiters.forEach { waiter ->
                    if (waiter.isActive) waiter.resume(binder)
                }
            } else {
                waiters.forEach { waiter ->
                    if (waiter.isActive) {
                        waiter.resumeWithException(IllegalStateException("Shizuku вернул пустую службу"))
                    }
                }
            }
            waiters.clear()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remoteService = null
            binding = false
            waiters.forEach { waiter ->
                if (waiter.isActive) {
                    waiter.resumeWithException(IllegalStateException("Соединение с Shizuku потеряно"))
                }
            }
            waiters.clear()
        }
    }

    fun isBinderAlive(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean = runCatching {
        isBinderAlive() &&
            !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun status(): ShellStatus {
        if (!isBinderAlive()) {
            return ShellStatus(
                mode = ExecutionMode.Shizuku,
                state = ShellState.Denied,
                title = "Shizuku не запущен",
                detail = "Служба Shizuku не найдена.",
                hint = "Запусти приложение Shizuku и вернись в TouchTuner."
            )
        }

        val preV11 = runCatching { Shizuku.isPreV11() }.getOrDefault(true)
        if (preV11) {
            return ShellStatus(
                mode = ExecutionMode.Shizuku,
                state = ShellState.Error,
                title = "Нужна новая версия Shizuku",
                detail = "Поддерживается Shizuku API 11 и новее.",
                hint = "Обнови Shizuku."
            )
        }

        val permission = runCatching { Shizuku.checkSelfPermission() }
            .getOrDefault(PackageManager.PERMISSION_DENIED)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            return ShellStatus(
                mode = ExecutionMode.Shizuku,
                state = ShellState.Denied,
                title = "Нет разрешения Shizuku",
                detail = "Приложению не выдан доступ к Shizuku.",
                hint = "Нажми «Проверить доступ» и подтверди разрешение в Shizuku."
            )
        }

        val binder = remoteService
        if (binder != null && runCatching { binder.asBinder().pingBinder() }.getOrDefault(false)) {
            return readyStatus()
        }

        return ShellStatus(
            mode = ExecutionMode.Shizuku,
            state = ShellState.Checking,
            title = "Подключение к Shizuku...",
            detail = "Ожидание службы выполнения команд.",
            hint = ""
        )
    }

    fun requestPermission(): Result<Unit> = runCatching {
        if (!isBinderAlive()) throw IllegalStateException("Shizuku не запущен")
        if (Shizuku.isPreV11()) throw IllegalStateException("Нужна Shizuku API 11 или новее")
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    suspend fun execute(script: String): ShellResult {
        val service = connect()
            ?: return ShellResult(
                output = "",
                exitCode = null,
                error = "Не удалось подключиться к Shizuku"
            )

        return withContext(Dispatchers.IO) {
            try {
                val output = service.run(script)
                val exitCode = runCatching { service.getLastExitCode() }.getOrNull()
                ShellResult(
                    output = output.orEmpty().trim(),
                    exitCode = exitCode
                )
            } catch (e: Exception) {
                ShellResult(
                    output = "",
                    exitCode = null,
                    error = "Ошибка Shizuku: ${e.message ?: "выполнение не удалось"}"
                )
            }
        }
    }

    suspend fun connect(timeoutMillis: Long = 20_000L): IUserService? {
        val existing = remoteService
        if (existing != null && runCatching { existing.asBinder().pingBinder() }.getOrDefault(false)) {
            return existing
        }
        if (!hasPermission()) return null

        val connected = try {
            withTimeoutOrNull<IUserService>(timeoutMillis) {
                suspendCancellableCoroutine<IUserService> { continuation ->
                    val current = remoteService
                    if (current != null &&
                        runCatching { current.asBinder().pingBinder() }.getOrDefault(false)
                    ) {
                        continuation.resume(current)
                        return@suspendCancellableCoroutine
                    }

                    waiters.add(continuation)
                    continuation.invokeOnCancellation { waiters.remove(continuation) }

                    if (!binding) {
                        binding = true
                        try {
                            Shizuku.bindUserService(arguments, connection)
                        } catch (t: Throwable) {
                            binding = false
                            val pending = waiters.toList()
                            waiters.clear()
                            pending.forEach { waiter ->
                                if (waiter.isActive) waiter.resumeWithException(t)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            null
        }

        if (connected == null) {
            binding = false
            runCatching { Shizuku.unbindUserService(arguments, connection, true) }
        }
        return connected
    }

    fun reset() {
        remoteService = null
        binding = false
        waiters.forEach { waiter ->
            if (waiter.isActive) {
                waiter.resumeWithException(IllegalStateException("Shizuku остановлен"))
            }
        }
        waiters.clear()
    }

    fun unbind() {
        val shouldUnbind = remoteService != null || binding
        remoteService = null
        binding = false
        if (shouldUnbind && isBinderAlive()) {
            runCatching { Shizuku.unbindUserService(arguments, connection, true) }
        }
        waiters.forEach { waiter ->
            if (waiter.isActive) {
                waiter.resumeWithException(IllegalStateException("Shizuku отключён"))
            }
        }
        waiters.clear()
    }

    private fun readyStatus(): ShellStatus {
        val uid = runCatching { Shizuku.getUid() }.getOrDefault(-1)
        val detail = when (uid) {
            0 -> "UID 0 · Root-режим"
            2000 -> "UID 2000 · ADB-режим"
            else -> "UID $uid"
        }
        return ShellStatus(
            mode = ExecutionMode.Shizuku,
            state = ShellState.Ready,
            title = "Shizuku активен",
            detail = detail,
            hint = ""
        )
    }

    companion object {
        const val REQUEST_CODE = 42
    }
}
