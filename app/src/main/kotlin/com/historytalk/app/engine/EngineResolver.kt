package com.historytalk.app.engine

import android.content.Context
import com.historytalk.app.data.repo.AppSettings
import com.historytalk.app.data.repo.EngineModes
import com.historytalk.app.data.repo.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class EngineStatus(
    val useModel: Boolean,          // آیا الان از مدل استفاده می‌شود
    val wantsModel: Boolean,        // آیا کاربر/خودکار مدل را می‌خواهد (حتی اگر هنوز آماده نباشد)
    val label: String,
    val detail: String,
)

/** تعیین حالت فعال موتور: مدل یا پایه، و توضیح روشن دلیل آن. */
@Singleton
class EngineResolver @Inject constructor(
    @ApplicationContext ctx: Context,
    private val settings: SettingsRepository,
    private val runtime: ModelRuntime,
    private val models: ModelManager,
) {
    val device: DeviceInfo = DeviceCapability.inspect(ctx)

    val status: Flow<EngineStatus> = combine(settings.settings, runtime.state, models.installed) { s, r, installed ->
        compute(s, r, installed, device)
    }

    suspend fun current(): EngineStatus = status.first()

    companion object {
        const val BASE_LABEL = "حالت پایه (بدون مدل)"
        const val MODEL_LABEL = "حالت مدل (روی دستگاه)"

        fun compute(s: AppSettings, r: RuntimeState, installed: String?, device: DeviceInfo): EngineStatus {
            fun base(detail: String, wants: Boolean = false) = EngineStatus(false, wants, BASE_LABEL, detail)
            if (s.engineMode == EngineModes.BASE) return base("به‌صورت دستی انتخاب شده")
            if (installed == null) return base("مدلی دانلود نشده؛ از تنظیمات می‌توانید دانلود کنید")
            if (s.engineMode == EngineModes.AUTO && !device.modelCapable) {
                return base("دستگاه برای اجرای مدل مناسب نیست (رم ${device.totalRamMb} مگابایت)")
            }
            return when (r) {
                is RuntimeState.Ready -> EngineStatus(true, true, MODEL_LABEL, "مدل $installed بارگذاری شده")
                is RuntimeState.Loading -> base("در حال بارگذاری مدل…", wants = true)
                is RuntimeState.Failed -> base("مدل با خطا روبه‌رو شد: ${r.message}")
                is RuntimeState.Unloaded -> base("مدل هنوز بارگذاری نشده", wants = true)
            }
        }
    }
}
