package com.historytalk.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.historytalk.app.ui.AppViewModel
import com.historytalk.app.ui.components.DisclaimerDialog
import com.historytalk.app.ui.components.LoadingState
import com.historytalk.app.ui.nav.AppNav
import com.historytalk.app.ui.theme.HistoryTalkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = hiltViewModel()
            val settings by vm.settingsState.collectAsStateWithLifecycle()
            val ready by vm.dataReady.collectAsStateWithLifecycle()
            val current = settings
            val dark = when (current?.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            // آیکون‌های نوار وضعیت/ناوبری متناسب با تم انتخابی برنامه (نه فقط تم سیستم)
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            HistoryTalkTheme(dark = dark, fontScale = current?.fontScale ?: 1f) {
                // رابط کاملاً راست‌به‌چپ
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (current == null || !ready) {
                            LoadingState("در حال آماده‌سازی…")
                        } else {
                            AppNav()
                            if (!current.disclaimerAccepted) DisclaimerDialog(onAccept = vm::acceptDisclaimer)
                        }
                    }
                }
            }
        }
    }
}
