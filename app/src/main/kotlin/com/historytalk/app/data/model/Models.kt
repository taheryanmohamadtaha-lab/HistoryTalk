package com.historytalk.app.data.model

import kotlinx.serialization.Serializable

/** قالب JSON پک داده یک شخصیت (assets/figures/*.json یا پک خارجی). */
@Serializable
data class FigurePack(
    val id: String,
    val name: String,
    val nameEn: String = "",
    val lifespan: String,
    val era: String,
    val eraGroup: String,          // ANCIENT | MEDIEVAL | EARLY_MODERN | MODERN
    val civilization: String,
    val role: String,
    val roleGroup: String,         // RULER | THINKER | LEADER
    val nationality: String,
    val sensitive: Boolean = false,
    val colorHex: String = "#8B6F3A",
    val summary: String,
    val voice: String = "persona", // persona = اول‌شخص | historian = راوی تاریخی (برای شخصیت‌های جنجالی)
    val openers: List<String> = emptyList(),
    val closers: List<String> = emptyList(),
    val vocabulary: List<String> = emptyList(),
    val places: List<String> = emptyList(),
    val relations: List<String> = emptyList(),
    val timeline: List<TimelineItem> = emptyList(),
    val knowledge: List<KnowledgeItem> = emptyList(),
    val quotes: List<QuoteItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val challenges: List<ChallengeItem> = emptyList(),
)

@Serializable data class TimelineItem(val year: String, val text: String)

@Serializable
data class KnowledgeItem(
    val category: String,
    val keywords: List<String> = emptyList(),
    val text: String,
    val source: String = "",
)

/** type: DOCUMENTED = مستند، ATTRIBUTED = منسوب، RECREATED = بازآفرینی */
@Serializable data class QuoteItem(val text: String, val source: String = "", val type: String = "RECREATED")

@Serializable data class ChallengeItem(val question: String, val keywords: List<String>, val explanation: String)

/** دسته‌بندی‌های پرسش/دانش. */
object Cat {
    const val LIFE = "life"
    const val WAR = "war"
    const val POLITICS = "politics"
    const val FAMILY = "family"
    const val BELIEFS = "beliefs"
    const val SCIENCE = "science"
    const val LEGACY = "legacy"
    const val CRIMES = "crimes"
    const val ERA = "era"
    const val GREETING = "greeting"
    const val INTRO = "intro"
    const val GENERAL = "general"

    val labels = mapOf(
        LIFE to "زندگی", WAR to "جنگ", POLITICS to "سیاست", FAMILY to "خانواده",
        BELIEFS to "عقاید", SCIENCE to "دانش و آثار", LEGACY to "میراث", CRIMES to "پیامدها و جنایات",
        ERA to "دوران و جامعه", GENERAL to "عمومی",
    )
}

const val DISCLAIMER =
    "این گفتگو یک بازآفرینی آموزشی و خیالی بر پایه منابع تاریخی است و نظر واقعی آن شخص یا نظر برنامه نیست."
