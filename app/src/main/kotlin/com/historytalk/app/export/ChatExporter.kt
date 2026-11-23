package com.historytalk.app.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.historytalk.app.data.db.ConversationEntity
import com.historytalk.app.data.db.FigureEntity
import com.historytalk.app.data.db.MessageEntity
import com.historytalk.app.data.model.DISCLAIMER
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** خروجی گرفتن گفتگو به TXT یا PDF در cache/exports (قابل اشتراک‌گذاری با FileProvider). */
@Singleton
class ChatExporter @Inject constructor(@ApplicationContext private val ctx: Context) {

    private fun exportDir() = File(ctx.cacheDir, "exports").apply { mkdirs() }

    private fun fileBase(figure: FigureEntity?, conv: ConversationEntity): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date(conv.updatedAt))
        return "historytalk-${figure?.id ?: "chat"}-$stamp"
    }

    private fun visible(messages: List<MessageEntity>) =
        messages.filter { it.engine != MessageEntity.ENGINE_NOTICE && it.text.isNotBlank() }

    fun buildText(figure: FigureEntity?, conv: ConversationEntity, messages: List<MessageEntity>): String = buildString {
        appendLine("گفتگوی تاریخ — ${figure?.name ?: conv.title}")
        appendLine(DISCLAIMER)
        appendLine("تاریخ: " + SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(conv.updatedAt)))
        appendLine("────────────────────")
        for (m in visible(messages)) {
            val who = if (m.role == MessageEntity.USER) "شما" else (figure?.name ?: "شخصیت")
            appendLine("$who:")
            appendLine(m.text.trim())
            appendLine()
        }
    }

    suspend fun exportTxt(figure: FigureEntity?, conv: ConversationEntity, messages: List<MessageEntity>): File =
        withContext(Dispatchers.IO) {
            File(exportDir(), fileBase(figure, conv) + ".txt").apply { writeText(buildText(figure, conv, messages), Charsets.UTF_8) }
        }

    suspend fun exportPdf(figure: FigureEntity?, conv: ConversationEntity, messages: List<MessageEntity>): File =
        withContext(Dispatchers.IO) {
            val file = File(exportDir(), fileBase(figure, conv) + ".pdf")
            val typeface = runCatching {
                if ("Vazirmatn-Regular.ttf" in ctx.assets.list("fonts").orEmpty())
                    Typeface.createFromAsset(ctx.assets, "fonts/Vazirmatn-Regular.ttf") else Typeface.DEFAULT
            }.getOrDefault(Typeface.DEFAULT)

            val pageW = 595; val pageH = 842; val margin = 40
            val width = pageW - 2 * margin
            val doc = PdfDocument()
            var pageNo = 0
            var page: PdfDocument.Page? = null
            var y = 0f

            fun newPage() {
                page?.let { doc.finishPage(it) }
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
                y = margin.toFloat()
            }

            fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                this.textSize = size; this.color = color
                this.typeface = if (bold) Typeface.create(typeface, Typeface.BOLD) else typeface
            }

            fun drawBlock(text: String, tp: TextPaint, gapAfter: Float) {
                val layout = StaticLayout.Builder.obtain(text, 0, text.length, tp, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setTextDirection(TextDirectionHeuristics.RTL)
                    .setLineSpacing(3f, 1f)
                    .build()
                if (page == null) newPage()
                for (i in 0 until layout.lineCount) {
                    val top = layout.getLineTop(i); val bottom = layout.getLineBottom(i)
                    val h = (bottom - top).toFloat()
                    if (y + h > pageH - margin) newPage()
                    val c = page!!.canvas
                    c.save()
                    c.clipRect(margin.toFloat(), y, (margin + width).toFloat(), y + h)
                    c.translate(margin.toFloat(), y - top)
                    layout.draw(c)
                    c.restore()
                    y += h
                }
                y += gapAfter
            }

            newPage()
            drawBlock("گفتگوی تاریخ — ${figure?.name ?: conv.title}", paint(18f, Color.BLACK, bold = true), 6f)
            drawBlock(DISCLAIMER, paint(10f, Color.DKGRAY), 14f)
            for (m in visible(messages)) {
                val mine = m.role == MessageEntity.USER
                drawBlock(if (mine) "شما:" else "${figure?.name ?: "شخصیت"}:", paint(11f, if (mine) Color.rgb(0x6B, 0x4A, 0x12) else Color.rgb(0x2A, 0x21, 0x18), bold = true), 2f)
                drawBlock(m.text.trim(), paint(12f, Color.BLACK), 12f)
            }
            page?.let { doc.finishPage(it) }
            file.outputStream().use { doc.writeTo(it) }
            doc.close()
            file
        }

    fun shareIntent(file: File, mime: String): Intent {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "اشتراک‌گذاری گفتگو",
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** حذف خروجی‌های موقت. */
    fun clearCache() { exportDir().listFiles()?.forEach { it.delete() } }
}
