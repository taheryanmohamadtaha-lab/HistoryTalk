package com.historytalk.app.engine

enum class BlockReason { HOLOCAUST_DENIAL, HATE, VIOLENCE, WEAPONS, CURRENT_EVENTS, IDEOLOGY_PRAISE, INAPPROPRIATE }

sealed class SafetyVerdict {
    object Allowed : SafetyVerdict()
    data class Blocked(val reason: BlockReason) : SafetyVerdict()
}

/**
 * فیلتر ایمنی آفلاین (قاعده‌محور) روی ورودی و خروجی.
 * محدودیت: مبتنی بر کلیدواژه/الگوست و نمی‌تواند همه‌ی بازنویسی‌های ممکن را بگیرد؛ مثبت کاذب و منفی کاذب ممکن است.
 */
object SafetyFilter {

    private fun n(s: String) = PersianText.normalize(s)

    private val genocideWords = listOf("هولوکاست", "هولوکوست", "قتل عام یهودیان", "اردوگاه های مرگ", "اتاق های گاز", "اتاق گاز", "نسل کشی", "قتل عام ارامنه", "هولودومور")
    private val denialWords = listOf("دروغ", "ساختگی", "افسانه", "اتفاق نیفتاد", "رخ نداد", "وجود نداشت", "انکار", "جعلی", "اغراق", "توهم", "حقیقت ندارد", "نبوده", "نبود", "تبلیغات")

    private val groupWords = listOf("یهودی", "مسلمان", "مسیحی", "بهایی", "سیاه پوست", "کولی", "عرب ها", "ترک ها", "کردها", "فارس ها", "لرها", "بلوچ", "افغان", "شیعه", "سنی", "زنان", "همجنس")
    private val hatePatterns = listOf(
        Regex("نژاد (برتر|پست)"), Regex("برتری نژاد"), Regex("پاکسازی قومی را"),
        Regex("(باید|بایستی) .{0,40}(نابود|ریشه کن|کشته|حذف|پاکسازی) .{0,12}(شوند|شود|بشوند|بشود|گردند)"),
        Regex("(متنفرم|نفرت دارم) از"),
    )
    private val hateExtra = listOf("پست هستند", "حیوان هستند", "انگل هستند", "کثیف هستند", "لایق زندگی نیستند", "نابودشان کن")

    private val violencePhrases = listOf(
        "مردم را به کشتن", "ترغیب کن", "تحریک کن", "بکشید", "حمله کنید", "دعوت به خشونت", "بیایید بکشیم",
        "حمله تروریستی", "چگونه کسی را بکشم", "چطور کسی را بکشم", "قتل را توجیه", "به خشونت دعوت",
    )

    private val howToPhrases = listOf("دستورالعمل ساخت", "روش ساخت", "طرز ساخت", "مراحل ساخت", "فرمول ساخت", "آموزش ساخت", "طرز تهیه", "راه ساخت", "نحوه ساخت", "نحوه تهیه")
    private val makeVerbs = setOf("بسازم", "بسازیم", "بساز", "بسازید", "تهیه", "مونتاژ")
    private val weaponWords = listOf("بمب", "سلاح", "اسلحه", "مواد منفجره", "نارنجک", "گاز سمی", "سم ", "باروت", "ماده منفجره", "سلاح شیمیایی", "سلاح بیولوژیک")

    private val ideologyWords = listOf("نازیسم", "فاشیسم", "نژادپرستی", "نژاد برتر", "برتری نژاد", "پاکسازی قومی", "نسل کشی", "هولوکاست", "هولوکوست")
    private val promoteWords = listOf("ستایش", "تمجید", "تبلیغ", "ترویج", "دفاع", "توجیه", "تایید کن", "درست بود", "حق داشتی", "ایده آل", "بنویس که", "افتخار")

    private val currentPhrases = listOf(
        "اخبار امروز", "اخبار روز", "رویدادهای امروز", "اوضاع امروز", "این روزها", "سیاست امروز", "رئیس جمهور فعلی", "نخست وزیر فعلی",
        "رهبر فعلی", "دولت فعلی", "اگر امروز زنده بودی", "جنگ اوکراین", "جنگ غزه", "انتخابات امسال", "درباره رئیس جمهور",
        "ترامپ", "بایدن", "پوتین", "نتانیاهو", "زلنسکی", "شی جین پینگ", "ایلان ماسک", "هوش مصنوعی", "چت جی پی تی",
    )
    private val yearRegex = Regex("(^| )(20\\d\\d)( |$)")

    private val badTokens = setOf("عوضی", "حرامزاده", "کثافت", "احمق", "جنده", "کیر", "کونی", "سکس", "پورن", "گوه")

    fun checkInput(text: String): SafetyVerdict {
        val t = " " + n(text) + " "
        val tokens = t.trim().split(' ').toSet()

        if (tokens.any { it in badTokens }) return SafetyVerdict.Blocked(BlockReason.INAPPROPRIATE)

        // انکار هولوکاست/نسل‌کشی
        if (genocideWords.any { t.contains(n(it)) } && denialWords.any { t.contains(n(it)) }) {
            return SafetyVerdict.Blocked(BlockReason.HOLOCAUST_DENIAL)
        }
        if (violencePhrases.any { t.contains(n(it)) }) return SafetyVerdict.Blocked(BlockReason.VIOLENCE)

        val weapon = weaponWords.any { t.contains(n(it).let { w -> if (it.endsWith(" ")) "$w " else w }) }
        if (weapon && (howToPhrases.any { t.contains(n(it)) } || tokens.any { it in makeVerbs })) {
            return SafetyVerdict.Blocked(BlockReason.WEAPONS)
        }
        if (hatePatterns.any { it.containsMatchIn(t) } || hateExtra.any { t.contains(n(it)) } ||
            (groupWords.any { t.contains(n(it)) } && (t.contains("نابودشان") || t.contains("ریشه کن")))
        ) return SafetyVerdict.Blocked(BlockReason.HATE)

        if (ideologyWords.any { t.contains(n(it)) } && promoteWords.any { t.contains(n(it)) }) {
            return SafetyVerdict.Blocked(BlockReason.IDEOLOGY_PRAISE)
        }
        if (currentPhrases.any { t.contains(n(it)) } || yearRegex.containsMatchIn(t)) {
            return SafetyVerdict.Blocked(BlockReason.CURRENT_EVENTS)
        }
        return SafetyVerdict.Allowed
    }

    /** فیلتر خروجی: برای متن تولیدشده (مدل یا الگو). محافظه‌کارانه‌تر است. */
    fun checkOutput(text: String): SafetyVerdict {
        val sentences = text.split('.', '؟', '!', '\n', '؛')
        for (s in sentences) {
            val t = " " + n(s) + " "
            val tokens = t.trim().split(' ').toSet()
            if (tokens.any { it in badTokens }) return SafetyVerdict.Blocked(BlockReason.INAPPROPRIATE)
            if (genocideWords.any { t.contains(n(it)) } &&
                listOf("دروغ", "ساختگی", "افسانه", "رخ نداد", "اتفاق نیفتاد", "جعلی", "توهم").any { t.contains(n(it)) } &&
                !listOf("منکر", "انکار", "ادعا", "نادرست", "ادعای دروغ").any { t.contains(n(it)) }
            ) return SafetyVerdict.Blocked(BlockReason.HOLOCAUST_DENIAL)
            if (hatePatterns.any { it.containsMatchIn(t) } || hateExtra.any { t.contains(n(it)) }) {
                return SafetyVerdict.Blocked(BlockReason.HATE)
            }
            if (howToPhrases.any { t.contains(n(it)) } &&
                weaponWords.any { t.contains(n(it).let { w -> if (it.endsWith(" ")) "$w " else w }) }
            ) return SafetyVerdict.Blocked(BlockReason.WEAPONS)
            if (violencePhrases.any { t.contains(n(it)) }) return SafetyVerdict.Blocked(BlockReason.VIOLENCE)
        }
        return SafetyVerdict.Allowed
    }

    /** پیام خروج از نقش + ارجاع به واقعیت تاریخی. */
    fun refusalMessage(reason: BlockReason): String = when (reason) {
        BlockReason.HOLOCAUST_DENIAL ->
            "از نقش بیرون می‌آیم: این درخواست را نمی‌پذیرم، چون انکار نسل‌کشی در این برنامه ممکن نیست.\n\n" +
            "واقعیت تاریخی: هولوکاست یکی از مستندترین رویدادهای تاریخ است. رژیم نازی و همدستانش حدود شش میلیون یهودی را " +
            "به‌صورت سازمان‌یافته کشتند؛ اسناد خود آلمان، شهادت بازماندگان و عاملان، دادگاه‌های نورنبرگ و بقایای اردوگاه‌ها آن را تأیید می‌کنند. " +
            "نسل‌کشی‌های دیگر (مانند ارمنیان و روآندا) نیز با اسناد و شهادت‌های فراوان ثبت شده‌اند."
        BlockReason.HATE ->
            "از نقش بیرون می‌آیم: نمی‌توانم نفرت‌پراکنی علیه گروه‌های قومی، مذهبی یا نژادی بنویسم.\n\n" +
            "واقعیت تاریخی: ایدئولوژی‌های مبتنی بر برتری نژادی و قومی در قرن بیستم به کشتار و رنج گسترده انجامیدند. " +
            "اگر می‌خواهید، می‌توانم درباره ریشه‌ها و پیامدهای تاریخی آن‌ها به‌صورت مستند توضیح بدهم."
        BlockReason.VIOLENCE ->
            "از نقش بیرون می‌آیم: نمی‌توانم به خشونت امروزی دعوت یا آن را ترویج کنم.\n\n" +
            "اگر درباره‌ی جنگ‌ها و خشونت‌های تاریخی همان دوران و پیامدهای آن‌ها سؤال دارید، با کمال میل به‌صورت تاریخی و مستند پاسخ می‌دهم."
        BlockReason.WEAPONS ->
            "از نقش بیرون می‌آیم: دستورالعمل ساخت سلاح یا مواد خطرناک نمی‌دهم.\n\n" +
            "می‌توانم از دید تاریخی توضیح بدهم که فناوری نظامی آن دوران چه نقشی در جنگ‌ها داشت، بدون جزئیات عملی ساخت."
        BlockReason.CURRENT_EVENTS ->
            "از نقش بیرون می‌آیم: این شخصیت به دوران خودش تعلق دارد و نمی‌توانم از زبان او درباره‌ی افراد زنده یا رویدادهای امروز اظهارنظر بسازم.\n\n" +
            "می‌توانید درباره‌ی دوران، تصمیم‌ها و دانش زمانه‌ی خود او بپرسید."
        BlockReason.IDEOLOGY_PRAISE ->
            "از نقش بیرون می‌آیم: نمی‌توانم ایدئولوژی‌های نفرت‌محور، نسل‌کشی یا خشونت سازمان‌یافته را ستایش یا تبلیغ کنم.\n\n" +
            "واقعیت تاریخی: این ایدئولوژی‌ها به جنایت‌های گسترده علیه غیرنظامیان منجر شدند. می‌توانم تاریخ‌شان و پیامدهایشان را مستند توضیح دهم."
        BlockReason.INAPPROPRIATE ->
            "از نقش بیرون می‌آیم: لطفاً محترمانه‌تر بپرسید. برای ادامه‌ی گفتگو، پرسش تاریخی‌تان را بدون الفاظ نامناسب مطرح کنید."
    }
}
