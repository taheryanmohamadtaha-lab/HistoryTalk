package com.historytalk.app.engine

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface LlmEngine {
    suspend fun load(modelPath: String)
    /** خروجی: تکه‌های افزایشی متن. */
    fun generate(prompt: String): Flow<String>
    fun stop()
    fun unload()
}

/** پیاده‌سازی با MediaPipe LLM Inference (فایل‌های .task / .bin سازگار). */
@Singleton
class MediaPipeLlmEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : LlmEngine {

    @Volatile private var llm: LlmInference? = null
    @Volatile private var stopped = false

    override suspend fun load(modelPath: String) = withContext(Dispatchers.Default) {
        unload()
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(MAX_TOKENS)
            .build()
        llm = LlmInference.createFromOptions(context, options)
        Unit
    }

    override fun generate(prompt: String): Flow<String> = callbackFlow {
        val engine = llm
        if (engine == null) {
            close(IllegalStateException("مدل بارگذاری نشده است"))
            return@callbackFlow
        }
        stopped = false
        try {
            engine.generateResponseAsync(prompt) { partial, done ->
                if (!stopped && partial != null) trySend(partial)
                if (done || stopped) close()
            }
        } catch (t: Throwable) {
            close(t)
        }
        awaitClose { stopped = true }
    }

    // MediaPipe API پایدار لغو تولید را در همه نسخه‌ها ندارد؛ پس خروجی‌های بعدی را نادیده می‌گیریم.
    override fun stop() { stopped = true }

    override fun unload() {
        stopped = true
        runCatching { llm?.close() }
        llm = null
    }

    private companion object { const val MAX_TOKENS = 1536 }
}
