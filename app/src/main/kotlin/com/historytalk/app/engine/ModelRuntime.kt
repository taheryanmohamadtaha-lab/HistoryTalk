package com.historytalk.app.engine

import com.historytalk.app.di.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RuntimeState {
    data object Unloaded : RuntimeState
    data object Loading : RuntimeState
    data object Ready : RuntimeState
    data class Failed(val message: String) : RuntimeState
}

/** مدیریت چرخه‌ی عمر مدل روی دستگاه: بارگذاری تنبل، آزادسازی در کمبود حافظه، گزارش خطا. */
@Singleton
class ModelRuntime @Inject constructor(
    private val engine: MediaPipeLlmEngine,
    private val models: ModelManager,
    @AppScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<RuntimeState>(RuntimeState.Unloaded)
    val state: StateFlow<RuntimeState> = _state.asStateFlow()
    private var loadJob: Job? = null

    fun ensureLoaded() {
        val file = models.installedFile() ?: return
        if (_state.value is RuntimeState.Ready || _state.value is RuntimeState.Loading) return
        if (_state.value is RuntimeState.Failed) return // تا تلاش مجدد دستی تکرار نشود
        _state.value = RuntimeState.Loading
        loadJob = scope.launch(Dispatchers.Default) {
            try {
                engine.load(file.absolutePath)
                _state.value = RuntimeState.Ready
            } catch (t: Throwable) { // شامل OutOfMemoryError
                engine.unload()
                _state.value = RuntimeState.Failed(t.message ?: t.javaClass.simpleName)
            }
        }
    }

    fun retry() { if (_state.value is RuntimeState.Failed) _state.value = RuntimeState.Unloaded; ensureLoaded() }

    fun unload() {
        loadJob?.cancel()
        engine.unload()
        _state.value = RuntimeState.Unloaded
    }

    /** منتظر آماده شدن می‌ماند؛ true یعنی آماده است. */
    suspend fun awaitReady(timeoutMs: Long): Boolean {
        ensureLoaded()
        val s = withTimeoutOrNull(timeoutMs) { state.first { it is RuntimeState.Ready || it is RuntimeState.Failed } }
        return s is RuntimeState.Ready
    }

    fun generate(prompt: String): Flow<String> = engine.generate(prompt)
    fun stop() = engine.stop()
    fun markFailed(msg: String) { engine.unload(); _state.value = RuntimeState.Failed(msg) }
}
