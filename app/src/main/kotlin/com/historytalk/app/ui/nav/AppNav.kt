package com.historytalk.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.historytalk.app.ui.screens.AboutScreen
import com.historytalk.app.ui.screens.ChatScreen
import com.historytalk.app.ui.screens.FigureListScreen
import com.historytalk.app.ui.screens.HistoryScreen
import com.historytalk.app.ui.screens.ProfileScreen
import com.historytalk.app.ui.screens.SettingsScreen

object Routes {
    const val LIST = "list"
    const val PROFILE = "profile/{id}"
    const val CHAT = "chat/{convId}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            FigureListScreen(
                onOpenFigure = { nav.navigate("profile/$it") },
                onHistory = { nav.navigate(Routes.HISTORY) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.PROFILE, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
            ProfileScreen(onBack = { nav.popBackStack() }, onOpenChat = { nav.navigate("chat/$it") })
        }
        composable(Routes.CHAT, arguments = listOf(navArgument("convId") { type = NavType.LongType })) {
            ChatScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { nav.popBackStack() }, onOpenChat = { nav.navigate("chat/$it") })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() }, onAbout = { nav.navigate(Routes.ABOUT) })
        }
        composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }) }
    }
}
