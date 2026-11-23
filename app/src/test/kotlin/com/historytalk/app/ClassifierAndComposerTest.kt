package com.historytalk.app

import com.historytalk.app.data.model.Cat
import com.historytalk.app.engine.FigureProfile
import com.historytalk.app.engine.HistoryTurn
import com.historytalk.app.engine.PromptBuilder
import com.historytalk.app.engine.PromptTemplate
import com.historytalk.app.engine.QuestionClassifier
import com.historytalk.app.engine.ResponseComposer
import com.historytalk.app.engine.RetrievalDoc
import com.historytalk.app.engine.ScoredDoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassifierAndComposerTest {
    private val persona = FigureProfile(
        "x", "کوروش", "۵۹۰ تا ۵۳۰ پ‌م", "شاه هخامنشی بودم.", false, "persona",
        listOf("ای دوست،"), listOf("این بود روایت من."), listOf("شاه"), listOf("درباره‌ی جنگ‌هایت بگو"),
    )
    private val historian = persona.copy(voice = "historian", sensitive = true, openers = listOf("به روایت منابع تاریخی:"), closers = emptyList())

    @Test fun classifiesCategories() {
        assertEquals(Cat.GREETING, QuestionClassifier.classify("سلام"))
        assertEquals(Cat.INTRO, QuestionClassifier.classify("خودت را معرفی کن"))
        assertEquals(Cat.WAR, QuestionClassifier.classify("درباره جنگ‌هایت بگو"))
        assertEquals(Cat.FAMILY, QuestionClassifier.classify("همسر و فرزندانت چه شدند؟"))
        assertEquals(Cat.CRIMES, QuestionClassifier.classify("درباره کشتار و جنایت‌ها بگو"))
        assertEquals(Cat.GENERAL, QuestionClassifier.classify("ققنوس"))
    }

    @Test fun answerWithoutKnowledgeIsHonest() {
        val a = ResponseComposer.compose(persona, "آب‌وهوای مریخ؟", Cat.GENERAL, emptyList())
        assertTrue(a.text.contains("پاسخ مستندی"))
        assertTrue(a.sourceIds.isEmpty())
    }

    @Test fun answerUsesRetrievedKnowledgeAndReportsSources() {
        val doc = RetrievalDoc("x#1", Cat.WAR, listOf("جنگ"), "بابل را بی‌جنگ گشودم.")
        val a = ResponseComposer.compose(persona, "جنگ", Cat.WAR, listOf(ScoredDoc(doc, 5.0)))
        assertTrue(a.text.contains("بابل را بی‌جنگ گشودم."))
        assertEquals(listOf("x#1"), a.sourceIds)
    }

    @Test fun historianVoiceHasNoFirstPersonCloser() {
        val doc = RetrievalDoc("x#1", Cat.CRIMES, listOf("جنایت"), "این رویداد مستند است.")
        val a = ResponseComposer.compose(historian, "جنایت", Cat.CRIMES, listOf(ScoredDoc(doc, 5.0)))
        assertTrue(a.text.startsWith("به روایت منابع تاریخی:"))
    }

    @Test fun greetingDiffersByVoice() {
        assertTrue(ResponseComposer.compose(persona, "سلام", Cat.GREETING, emptyList()).text.contains("من کوروش هستم"))
        assertTrue(ResponseComposer.compose(historian, "سلام", Cat.GREETING, emptyList()).text.contains("روایت تاریخی"))
    }

    @Test fun chunksReassembleToOriginal() {
        val text = (1..200).joinToString(" ") { "کلمه$it" }
        assertEquals(text, ResponseComposer.chunks(text).joinToString(""))
    }

    @Test fun promptContainsKnowledgeAndRespectsBudget() {
        val docs = listOf(RetrievalDoc("x#1", Cat.WAR, emptyList(), "متن مرجع شماره یک"))
        val sys = PromptBuilder.systemPrompt(persona, docs)
        assertTrue(sys.contains("متن مرجع شماره یک"))
        assertTrue(sys.contains("فقط بر پایه"))
        val long = (1..50).map { HistoryTurn(it % 2 == 0, "پیام طولانی ".repeat(40)) }
        val p = PromptBuilder.build(PromptTemplate.GEMMA, sys, long, "سؤال فعلی")
        assertTrue(p.length < 4000)
        assertTrue(p.contains("<start_of_turn>model"))
        assertTrue(p.contains("سؤال فعلی"))
    }

    @Test fun templateIsChosenByModelFileName() {
        assertEquals(PromptTemplate.GEMMA, PromptTemplate.forModelFile("gemma-2b-it.task"))
        assertEquals(PromptTemplate.CHATML, PromptTemplate.forModelFile("Qwen2.5-0.5B.task"))
        assertEquals(PromptTemplate.PLAIN, PromptTemplate.forModelFile("unknown.bin"))
    }
}
