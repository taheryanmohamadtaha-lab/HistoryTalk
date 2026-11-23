package com.historytalk.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.historytalk.app.data.model.DISCLAIMER
import com.historytalk.app.ui.components.DisclaimerBanner
import com.historytalk.app.ui.components.DisclaimerDialog
import com.historytalk.app.ui.components.EmptyState
import com.historytalk.app.ui.theme.HistoryTalkTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** تست UI پایه (روی دستگاه/شبیه‌ساز): پیام ثابت آموزشی و حالت‌های خالی. */
class DisclaimerUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun bannerShowsFixedDisclaimer() {
        rule.setContent { HistoryTalkTheme { DisclaimerBanner() } }
        rule.onNodeWithText(DISCLAIMER).assertIsDisplayed()
    }

    @Test fun firstRunDialogMustBeAccepted() {
        var accepted = false
        rule.setContent { HistoryTalkTheme { DisclaimerDialog(onAccept = { accepted = true }) } }
        rule.onNodeWithText(DISCLAIMER).assertIsDisplayed()
        rule.onNodeWithText("متوجه شدم").performClick()
        assertTrue(accepted)
    }

    @Test fun emptyStateShowsTitle() {
        rule.setContent { HistoryTalkTheme { EmptyState(title = "شخصیتی پیدا نشد", hint = "فیلترها را تغییر دهید.") } }
        rule.onNodeWithText("شخصیتی پیدا نشد").assertIsDisplayed()
    }
}
