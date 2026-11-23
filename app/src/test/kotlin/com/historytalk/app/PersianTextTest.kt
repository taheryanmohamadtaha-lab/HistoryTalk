package com.historytalk.app

import com.historytalk.app.engine.PersianText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersianTextTest {
    @Test fun normalizesArabicLettersAndDigits() {
        assertEquals("کتاب ی 123", PersianText.normalize("كتاب ي ١٢٣"))
        assertEquals("1889", PersianText.normalize("۱۸۸۹"))
    }

    @Test fun zeroWidthNonJoinerBecomesSpace() {
        assertEquals("می خواهم", PersianText.normalize("می‌خواهم"))
    }

    @Test fun removesDiacriticsAndPunctuation() {
        assertEquals("سلام دنیا", PersianText.normalize("سَلام، دنیا!"))
    }

    @Test fun tokensDropStopWords() {
        val t = PersianText.tokens("درباره جنگ هایت بگو")
        assertTrue("جنگ" in t)
        assertFalse("درباره" in t)
        assertFalse("بگو" in t)
    }

    @Test fun softMatchHandlesPrefixes() {
        assertTrue(PersianText.softMatch("جنگ", "جنگها"))
        assertTrue(PersianText.softMatch("کوروش", "کوروش"))
        assertFalse(PersianText.softMatch("جنگ", "جشن"))
        assertFalse(PersianText.softMatch("به", "بهار")) // کمتر از ۳ حرف
    }
}
