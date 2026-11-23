package com.historytalk.app.data.repo

import com.historytalk.app.data.db.SettingDao
import com.historytalk.app.data.db.SettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

object EngineModes { const val AUTO = "auto"; const val BASE = "base"; const val MODEL = "model" }

data class AppSettings(
    val themeMode: String = "system",   // system | light | dark
    val fontScale: Float = 1f,
    val engineMode: String = EngineModes.AUTO,
    val modelUrl: String = "",
    val disclaimerAccepted: Boolean = false,
)

object SettingKeys {
    const val THEME = "theme"
    const val FONT = "font_scale"
    const val ENGINE = "engine_mode"
    const val MODEL_URL = "model_url"
    const val DISCLAIMER = "disclaimer_accepted"
    const val PACK_VERSION = "pack_version"
}

@Singleton
class SettingsRepository @Inject constructor(private val dao: SettingDao) {
    val settings: Flow<AppSettings> = dao.observeAll().map { list ->
        val m = list.associate { it.key to it.value }
        AppSettings(
            themeMode = m[SettingKeys.THEME] ?: "system",
            fontScale = m[SettingKeys.FONT]?.toFloatOrNull()?.coerceIn(0.8f, 1.5f) ?: 1f,
            engineMode = m[SettingKeys.ENGINE] ?: EngineModes.AUTO,
            modelUrl = m[SettingKeys.MODEL_URL] ?: "",
            disclaimerAccepted = m[SettingKeys.DISCLAIMER] == "1",
        )
    }

    suspend fun current(): AppSettings = settings.first()
    suspend fun set(key: String, value: String) = dao.put(SettingEntity(key, value))
    suspend fun raw(key: String): String? = dao.observeAll().first().firstOrNull { it.key == key }?.value
}
