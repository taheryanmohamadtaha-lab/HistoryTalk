package com.historytalk.app.engine

data class HistoryTurn(val fromUser: Boolean, val text: String)

enum class PromptTemplate { GEMMA, CHATML, PLAIN;
    companion object {
        fun forModelFile(name: String): PromptTemplate {
            val n = name.lowercase()
            return when {
                "gemma" in n -> GEMMA
                "qwen" in n || "smollm" in n || "chatml" in n -> CHATML
                else -> PLAIN
            }
        }
    }
}

/** ساخت پرامپت سیستمی شخصیت + دانش بازیابی‌شده (RAG) برای مدل روی دستگاه. */
object PromptBuilder {
    private const val MAX_PROMPT_CHARS = 2400

    fun systemPrompt(figure: FigureProfile, knowledge: List<RetrievalDoc>): String {
        val ctx = knowledge.mapIndexed { i, d -> "[${i + 1}] ${d.text.trim()}" }.joinToString("\n")
        val voice = if (figure.voice == "historian")
            "به‌صورت یک راوی تاریخی دقیق درباره‌ی ${figure.name} پاسخ بده (سوم‌شخص)، نه با ستایش یا همدلی با او. اگر درباره‌ی جنایت‌ها و پیامدها پرسیده شد، صادقانه و مستند بگو."
        else
            "در نقش ${figure.name} (${figure.lifespan}) و اول‌شخص پاسخ بده، با لحن و واژگان دوران خودت" +
                (if (figure.vocabulary.isNotEmpty()) " (مثل: ${figure.vocabulary.take(5).joinToString("، ")})" else "") + "."
        return buildString {
            appendLine("تو یک بازآفرینی آموزشی و خیالی هستی؛ نه نظر واقعی آن شخص.")
            appendLine(voice)
            appendLine("قواعد: ۱) فقط بر پایه‌ی «دانش مرجع» زیر پاسخ بده. ۲) اگر پاسخ در آن نیست بگو «در دانش من نیست» و چیزی نساز. ۳) فارسی روان، ۲ تا ۵ جمله.")
            appendLine("۴) ایدئولوژی نفرت‌محور را ستایش نکن، هولوکاست یا نسل‌کشی را انکار نکن، علیه گروه‌ها نفرت نپراکن، دستور ساخت سلاح نده، درباره‌ی افراد زنده یا رویدادهای امروز اظهارنظر نکن.")
            appendLine("دانش مرجع:")
            append(ctx)
        }
    }

    fun build(
        template: PromptTemplate,
        system: String,
        history: List<HistoryTurn>,
        userMessage: String,
    ): String {
        // تاریخچه را از انتها می‌چینیم تا از سقف طول تجاوز نشود.
        val budget = MAX_PROMPT_CHARS - system.length - userMessage.length - 120
        val kept = ArrayDeque<HistoryTurn>()
        var used = 0
        for (t in history.asReversed()) {
            val len = t.text.length.coerceAtMost(300)
            if (used + len > budget) break
            kept.addFirst(HistoryTurn(t.fromUser, t.text.take(300)))
            used += len
        }
        return when (template) {
            PromptTemplate.GEMMA -> buildString {
                append("<start_of_turn>user\n").append(system).append("\n")
                if (kept.isNotEmpty()) append("\nگفتگوی قبلی:\n").append(kept.joinToString("\n") { (if (it.fromUser) "کاربر: " else "تو: ") + it.text }).append("\n")
                append("\nپرسش کاربر: ").append(userMessage).append("<end_of_turn>\n<start_of_turn>model\n")
            }
            PromptTemplate.CHATML -> buildString {
                append("<|im_start|>system\n").append(system).append("<|im_end|>\n")
                kept.forEach { append("<|im_start|>").append(if (it.fromUser) "user" else "assistant").append('\n').append(it.text).append("<|im_end|>\n") }
                append("<|im_start|>user\n").append(userMessage).append("<|im_end|>\n<|im_start|>assistant\n")
            }
            PromptTemplate.PLAIN -> buildString {
                append(system).append("\n\n")
                kept.forEach { append(if (it.fromUser) "کاربر: " else "پاسخ: ").append(it.text).append('\n') }
                append("کاربر: ").append(userMessage).append("\nپاسخ: ")
            }
        }
    }
}
