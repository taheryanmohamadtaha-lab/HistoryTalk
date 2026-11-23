package com.historytalk.app.engine

import com.historytalk.app.data.model.Cat
import kotlin.math.abs

/** اطلاعات لازم موتور از یک شخصیت (مستقل از Room). */
data class FigureProfile(
    val id: String,
    val name: String,
    val lifespan: String,
    val summary: String,
    val sensitive: Boolean,
    val voice: String,            // persona | historian
    val openers: List<String>,
    val closers: List<String>,
    val vocabulary: List<String>,
    val suggestions: List<String>,
)

data class ComposedAnswer(val text: String, val sourceIds: List<String>)

/** حالت جایگزین بدون مدل: الگوهای شخصیتی + دانش بازیابی‌شده. */
object ResponseComposer {

    private fun <T> pick(list: List<T>, seed: String): T? =
        if (list.isEmpty()) null else list[abs(seed.hashCode()) % list.size]

    fun compose(figure: FigureProfile, question: String, category: String, results: List<ScoredDoc>): ComposedAnswer {
        val historian = figure.voice == "historian"
        val opener = pick(figure.openers, question) ?: if (historian) "به روایت منابع تاریخی:" else ""

        when (category) {
            Cat.GREETING -> {
                val text = if (historian)
                    "این گفتگو درباره‌ی ${figure.name} است و پاسخ‌ها به‌صورت روایت تاریخی ارائه می‌شود. هر پرسش تاریخی‌ای دارید بپرسید."
                else
                    "$opener من ${figure.name} هستم (${figure.lifespan}). هر چه می‌خواهی درباره‌ی زندگی و روزگارم بپرس."
                return ComposedAnswer(text.trim(), emptyList())
            }
            Cat.INTRO -> {
                val text = if (historian) "${figure.name} (${figure.lifespan}): ${figure.summary}" else "$opener ${figure.summary}"
                return ComposedAnswer(text.trim(), emptyList())
            }
        }

        if (results.isEmpty()) {
            val hints = figure.suggestions.take(3).joinToString("؛ ") { "«$it»" }
            val text = (if (historian) "" else "") +
                "در دانش تاریخی‌ای که در این برنامه برای ${figure.name} ثبت شده، پاسخ مستندی برای این پرسش نیست و نمی‌خواهم چیزی از خودم بسازم." +
                (if (hints.isNotEmpty()) " می‌توانید مثلاً بپرسید: $hints." else "")
            return ComposedAnswer(text, emptyList())
        }

        val top = results.first()
        val chosen = mutableListOf(top)
        results.drop(1).firstOrNull { it.score >= top.score * 0.6 && !it.viaCategory }?.let { chosen += it }
        val body = chosen.joinToString(" ") { it.doc.text.trim() }
        val closer = if (historian) null else pick(figure.closers, question + "x")
        val text = buildString {
            if (opener.isNotBlank()) append(opener).append(' ')
            append(body)
            if (!closer.isNullOrBlank()) append(' ').append(closer)
        }
        return ComposedAnswer(text.trim(), chosen.map { it.doc.id })
    }

    /** تقسیم متن برای نمایش تدریجی (استریم) در حالت پایه. */
    fun chunks(text: String): List<String> {
        val words = text.split(' ')
        val out = mutableListOf<String>()
        val step = maxOf(1, words.size / 45)
        var i = 0
        while (i < words.size) {
            val end = minOf(words.size, i + step)
            out += words.subList(i, end).joinToString(" ") + if (end < words.size) " " else ""
            i = end
        }
        return out
    }
}
