package com.historytalk.app

import com.historytalk.app.data.model.Cat
import com.historytalk.app.data.model.FigurePack
import com.historytalk.app.engine.KnowledgeRetriever
import com.historytalk.app.engine.PersianText
import com.historytalk.app.engine.QuestionClassifier
import com.historytalk.app.engine.RetrievalDoc
import com.historytalk.app.engine.SafetyFilter
import com.historytalk.app.engine.SafetyVerdict
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** اعتبارسنجی پک‌های داده‌ی داخلی (assets/figures) با همان مدل و موتور برنامه. */
class FigurePacksTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val packs: List<FigurePack> by lazy {
        val dir = listOf(File("src/main/assets/figures"), File("app/src/main/assets/figures")).first { it.isDirectory }
        dir.listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }
            .map { json.decodeFromString<FigurePack>(it.readText(Charsets.UTF_8)) }
    }

    private fun docs(p: FigurePack) = p.knowledge.mapIndexed { i, k -> RetrievalDoc("${p.id}#$i", k.category, k.keywords, k.text, k.source) }

    @Test fun atLeastFifteenFiguresWithUniqueIds() {
        assertTrue(packs.size >= 15)
        assertEquals(packs.size, packs.map { it.id }.toSet().size)
        assertTrue(packs.all { Regex("^[a-z0-9_]{2,40}$").matches(it.id) })
    }

    @Test fun requiredFieldsAndEnumsAreValid() {
        for (p in packs) {
            assertTrue("${p.id}: eraGroup", p.eraGroup in setOf("ANCIENT", "MEDIEVAL", "EARLY_MODERN", "MODERN"))
            assertTrue("${p.id}: roleGroup", p.roleGroup in setOf("RULER", "THINKER", "LEADER"))
            assertTrue("${p.id}: voice", p.voice in setOf("persona", "historian"))
            assertTrue("${p.id}: summary", p.summary.length > 40)
            assertTrue("${p.id}: knowledge", p.knowledge.size >= 8)
            assertTrue("${p.id}: timeline", p.timeline.size >= 4)
            assertTrue("${p.id}: suggestions", p.suggestions.size >= 4)
            assertTrue("${p.id}: challenges", p.challenges.size >= 2)
            assertTrue("${p.id}: quote types", p.quotes.all { it.type in setOf("DOCUMENTED", "ATTRIBUTED", "RECREATED") })
            assertTrue("${p.id}: documented quotes need a source", p.quotes.filter { it.type == "DOCUMENTED" }.all { it.source.isNotBlank() })
        }
    }

    @Test fun everySuggestionIsAnswerableFromTheKnowledgeBase() {
        val r = KnowledgeRetriever()
        for (p in packs) for (s in p.suggestions) {
            val cat = QuestionClassifier.classify(s)
            if (cat == Cat.GREETING || cat == Cat.INTRO) continue
            assertTrue("${p.id}: «$s» بی‌پاسخ", r.rank(s, docs(p), cat, 3).isNotEmpty())
            assertTrue("${p.id}: «$s» در ورودی مسدود", SafetyFilter.checkInput(s) is SafetyVerdict.Allowed)
        }
    }

    @Test fun allContentPassesOutputSafetyFilter() {
        for (p in packs) {
            val texts = p.knowledge.map { it.text } + p.openers + p.closers + p.summary
            for (t in texts) assertTrue("${p.id}: «${t.take(40)}» مسدود", SafetyFilter.checkOutput(t) is SafetyVerdict.Allowed)
        }
    }

    @Test fun challengeKeywordsAppearInExplanations() {
        for (p in packs) for (c in p.challenges) {
            assertTrue("${p.id}: ${c.question}", c.keywords.any { PersianText.normalize(c.explanation).contains(PersianText.normalize(it)) })
        }
    }

    @Test fun sensitiveFiguresStateConsequencesHonestly() {
        for (id in listOf("hitler", "stalin")) {
            val p = packs.first { it.id == id }
            assertTrue(p.sensitive)
            assertEquals("historian", p.voice)
            assertTrue("$id باید دانشی درباره‌ی جنایت‌ها داشته باشد", p.knowledge.any { it.category == Cat.CRIMES })
        }
    }
}
