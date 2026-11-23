package com.historytalk.app.engine

import android.content.Context
import android.os.StatFs
import com.historytalk.app.data.repo.SettingsRepository
import com.historytalk.app.di.AppScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadState {
    data object Idle : DownloadState
    data object Probing : DownloadState
    data class Downloading(val done: Long, val total: Long) : DownloadState
    data class Failed(val message: String) : DownloadState
    data object Completed : DownloadState
}

/** دانلود اختیاری مدل (با نمایش حجم/پیشرفت و ادامه‌ی دانلود ناقص با Range). فقط آدرس‌های https. */
@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val settings: SettingsRepository,
    @AppScope private val scope: CoroutineScope,
) {
    private val dir = File(ctx.filesDir, "models").apply { mkdirs() }
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()
    private val _installed = MutableStateFlow(installedFile()?.name)
    /** نام فایل مدل نصب‌شده یا null */
    val installed: StateFlow<String?> = _installed.asStateFlow()
    private var job: Job? = null

    fun installedFile(): File? = dir.listFiles { f -> f.isFile && isModelName(f.name) }?.firstOrNull()
    fun partialBytes(): Long = dir.listFiles { f -> f.name.endsWith(".part") }?.firstOrNull()?.length() ?: 0L
    fun installedSizeBytes(): Long = installedFile()?.length() ?: 0L

    private fun isModelName(n: String) = n.endsWith(".task") || n.endsWith(".bin")

    private fun fileNameOf(url: String): String? {
        val raw = runCatching { URL(url).path.substringAfterLast('/') }.getOrNull() ?: return null
        val safe = raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return safe.takeIf { isModelName(it) }
    }

    /** حجم فایل را با درخواست HEAD می‌خواند (بدون دانلود). */
    suspend fun probe(url: String): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            require(url.startsWith("https://")) { "فقط آدرس https پذیرفته می‌شود" }
            requireNotNull(fileNameOf(url)) { "آدرس باید به یک فایل .task یا .bin ختم شود" }
            val c = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"; connectTimeout = 15000; readTimeout = 15000; instanceFollowRedirects = true
            }
            try {
                require(c.responseCode in 200..299) { "پاسخ سرور: ${c.responseCode}" }
                c.contentLengthLong.also { require(it > 0) { "حجم فایل مشخص نیست" } }
            } finally { c.disconnect() }
        }
    }

    fun start(url: String) {
        if (job?.isActive == true) return
        val name = fileNameOf(url)
        if (!url.startsWith("https://") || name == null) {
            _state.value = DownloadState.Failed("آدرس معتبر نیست: فقط https و فایل .task یا .bin")
            return
        }
        job = scope.launch(Dispatchers.IO) { download(url, name) }
    }

    /** توقف؛ فایل ناقص برای ادامه نگه داشته می‌شود. */
    fun pause() { job?.cancel(); _state.value = DownloadState.Idle }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        job?.cancel()
        dir.listFiles()?.forEach { it.delete() }
        _installed.value = null
        _state.value = DownloadState.Idle
    }

    private suspend fun download(url: String, name: String) {
        val part = File(dir, "$name.part")
        var conn: HttpURLConnection? = null
        try {
            _state.value = DownloadState.Probing
            var existing = if (part.exists()) part.length() else 0L
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000; readTimeout = 30000; instanceFollowRedirects = true
                if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
            }
            val code = conn.responseCode
            if (code == 200 && existing > 0) { part.delete(); existing = 0 } // سرور Range را نادیده گرفت
            else if (code != 200 && code != 206) { fail("خطای سرور: $code"); return }
            val len = conn.contentLengthLong
            val total = if (len > 0) existing + len else -1L
            val need = if (len > 0) len else 0L
            val free = StatFs(dir.path).availableBytes
            if (free < need + 64L * 1024 * 1024) { fail("حافظه‌ی آزاد کافی نیست (نیاز: ${need / 1048576} مگابایت)"); return }

            var done = existing
            var lastEmit = 0L
            conn.inputStream.use { input ->
                FileOutputStream(part, existing > 0).use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        currentCoroutineContext().ensureActive()
                        out.write(buf, 0, n)
                        done += n
                        val now = System.currentTimeMillis()
                        if (now - lastEmit > 250) { _state.value = DownloadState.Downloading(done, total); lastEmit = now }
                    }
                }
            }
            if (total > 0 && part.length() != total) { fail("دانلود ناقص ماند؛ «ادامه دانلود» را بزنید"); return }
            dir.listFiles { f -> f.isFile && isModelName(f.name) }?.forEach { it.delete() }
            val finalFile = File(dir, name)
            if (!part.renameTo(finalFile)) { fail("ذخیره‌ی فایل مدل ناموفق بود"); return }
            _installed.value = finalFile.name
            _state.value = DownloadState.Completed
        } catch (e: CancellationException) {
            _state.value = DownloadState.Idle
            throw e
        } catch (e: IOException) {
            fail("اتصال قطع شد؛ پیشرفت ذخیره شده و می‌توانید ادامه دهید (${e.javaClass.simpleName})")
        } catch (e: Throwable) {
            fail(e.message ?: "خطای ناشناخته")
        } finally {
            conn?.disconnect()
        }
    }

    private fun fail(msg: String) { _state.value = DownloadState.Failed(msg) }
}
