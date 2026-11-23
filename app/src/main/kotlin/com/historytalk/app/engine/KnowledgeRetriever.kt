package com.historytalk.app.engine

import kotlin.math.ln

data class RetrievalDoc(
    val id: String,
    val category: String,
    val keywords: List<String>,
    val text: String,
    val source: String = "",
)

data class ScoredDoc(val doc: RetrievalDoc, val score: Double, val viaCategory: Boolean = false)

/**
 * بازیابی کلیدی/معنایی سبک: BM25 ساده روی متن + وزن بالاتر برای کلیدواژه‌ها + امتیاز تطابق دسته.
 * حجم دانش هر شخصیت کوچک است، پس همه‌چیز در حافظه و در چند میلی‌ثانیه انجام می‌شود.
 */
class KnowledgeRetriever {

    private class Prepared(val doc: RetrievalDoc, val text: List<String>, val kw: List<String>)

    fun rank(query: String, docs: List<RetrievalDoc>, categoryHint: String? = null, topK: Int = 4): List<ScoredDoc> {
        if (docs.isEmpty()) return emptyList()
        val q = PersianText.tokens(query).distinct()
        val prepared = docs.map {
            Prepared(it, PersianText.tokens(it.text), PersianText.tokens(it.keywords.joinToString(" ")))
        }
        val n = docs.size
        val scored = mutableListOf<ScoredDoc>()
        if (q.isNotEmpty()) {
            val idf = q.associateWith { t ->
                val df = prepared.count { p -> p.text.any { PersianText.softMatch(t, it) } || p.kw.any { PersianText.softMatch(t, it) } }
                ln(1.0 + (n - df + 0.5) / (df + 0.5))
            }
            for (p in prepared) {
                var s = 0.0
                for (t in q) {
                    val kwHits = p.kw.count { PersianText.softMatch(t, it) }
                    val tf = p.text.count { PersianText.softMatch(t, it) }
                    val tfSat = if (tf == 0) 0.0 else tf * 2.2 / (tf + 1.2)
                    s += (idf[t] ?: 0.0) * (kwHits.coerceAtMost(2) * 3.0 + tfSat)
                }
                if (s > 0.0) {
                    if (categoryHint != null && p.doc.category == categoryHint) s += 1.5
                    scored += ScoredDoc(p.doc, s)
                }
            }
        }
        if (scored.isNotEmpty()) {
            return scored.sortedByDescending { it.score }.take(topK)
        }
        // بدون تطابق واژگانی: اگر دسته مشخص است، همان دسته را (با اطمینان پایین) برگردان
        if (categoryHint != null && categoryHint != Cat_GENERAL && categoryHint != Cat_GREETING && categoryHint != Cat_INTRO) {
            return docs.filter { it.category == categoryHint }.take(topK).map { ScoredDoc(it, 0.1, viaCategory = true) }
        }
        return emptyList()
    }

    private companion object {
        const val Cat_GENERAL = "general"
        const val Cat_GREETING = "greeting"
        const val Cat_INTRO = "intro"
    }
}
