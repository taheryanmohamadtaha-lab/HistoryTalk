package com.historytalk.app

import com.historytalk.app.engine.KnowledgeRetriever
import com.historytalk.app.engine.RetrievalDoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeRetrieverTest {
    private val docs = listOf(
        RetrievalDoc("a#0", "life", listOf("تولد", "کودکی"), "من در شهر کوچکی زاده شدم و کودکی‌ام در روستا گذشت."),
        RetrievalDoc("a#1", "war", listOf("جنگ", "نبرد", "لشکر"), "در جنگ بزرگ لشکر من پیروز شد و شهر را فتح کردیم."),
        RetrievalDoc("a#2", "family", listOf("همسر", "فرزند"), "همسر و دو فرزند داشتم."),
        RetrievalDoc("a#3", "beliefs", listOf("باور", "خدا"), "به عدالت و خدا باور داشتم."),
    )
    private val r = KnowledgeRetriever()

    @Test fun findsDocByKeyword() {
        val res = r.rank("درباره جنگ‌هایت بگو", docs, "war", 3)
        assertEquals("a#1", res.first().doc.id)
    }

    @Test fun findsDocByBodyText() {
        val res = r.rank("فرزندانت چند نفر بودند؟", docs, null, 3)
        assertEquals("a#2", res.first().doc.id)
    }

    @Test fun noOverlapAndGeneralCategoryReturnsNothing() {
        assertTrue(r.rank("آب‌وهوای مریخ چطور است", docs, "general", 3).isEmpty())
    }

    @Test fun categoryFallbackIsMarkedLowConfidence() {
        val res = r.rank("آب‌وهوای مریخ چطور است", docs, "family", 3)
        assertEquals(1, res.size)
        assertTrue(res.first().viaCategory)
    }

    @Test fun emptyDocsGiveEmptyResult() {
        assertTrue(r.rank("جنگ", emptyList(), "war").isEmpty())
    }

    @Test fun topKIsRespected() {
        assertTrue(r.rank("جنگ شهر لشکر همسر خدا", docs, null, 2).size <= 2)
    }
}
