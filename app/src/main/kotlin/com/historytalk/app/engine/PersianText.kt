package com.historytalk.app.engine

/** نرمال‌سازی و توکن‌سازی متن فارسی (آفلاین، بدون وابستگی به اندروید). */
object PersianText {
    private val stopWords = setOf(
        "و", "در", "از", "به", "که", "را", "این", "آن", "با", "برای", "است", "بود", "هست", "من", "تو",
        "شما", "ما", "می", "چه", "چرا", "چگونه", "کی", "کجا", "آیا", "هم", "یا", "تا", "بر", "شد", "کرد",
        "ها", "های", "یک", "بود", "بوده", "باشد", "نیز", "اما", "ولی", "اگر", "پس", "هر", "همه", "بگو",
        "درباره", "مورد", "بیشتر", "لطفا", "ممکن", "است", "کن", "کنی", "کنید", "چی", "چطور", "نظر", "خودت",
        "خود", "او", "ات", "ام", "اش", "هستی", "هستید", "داری", "دارید", "دارم", "داشت", "شده", "شود", "بین",
    )

    fun normalize(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when {
                ch == 'ي' || ch == 'ى' || ch == 'ئ' -> sb.append('ی')
                ch == 'ك' -> sb.append('ک')
                ch == 'ة' || ch == 'ۀ' -> sb.append('ه')
                ch == 'آ' || ch == 'أ' || ch == 'إ' || ch == 'ٱ' -> sb.append('ا')
                ch == 'ؤ' -> sb.append('و')
                ch == '‌' || ch == '‍' || ch == '‏' || ch == '‎' -> sb.append(' ')
                ch in 'ً'..'ٟ' || ch == 'ٰ' || ch == 'ـ' -> Unit
                ch in '۰'..'۹' -> sb.append('0' + (ch - '۰'))
                ch in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
                Character.isLetterOrDigit(ch) -> sb.append(ch.lowercaseChar())
                else -> sb.append(' ')
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    fun tokens(input: String, removeStopWords: Boolean = true): List<String> =
        normalize(input).split(' ')
            .filter { it.isNotEmpty() }
            .filter { it.length >= 2 || it.all(Char::isDigit) }
            .filter { !removeStopWords || it !in stopWords }

    /** تطبیق نرم: برابری یا پیشوند (حداقل ۳ حرف) برای پوشش شکل‌های جمع/ضمیر. */
    fun softMatch(a: String, b: String): Boolean {
        if (a == b) return true
        val m = minOf(a.length, b.length)
        if (m < 3) return false
        return a.startsWith(b) || b.startsWith(a)
    }
}
