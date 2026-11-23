package com.historytalk.app

import android.app.Application
import com.historytalk.app.engine.ModelRuntime
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HistoryTalkApp : Application() {

    @Inject lateinit var runtime: ModelRuntime

    /** در کمبود حافظه مدل را آزاد می‌کنیم تا برنامه کرش نکند؛ بارگذاری بعدی تنبل و خودکار است. */
    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // 15 = TRIM_MEMORY_RUNNING_CRITICAL، 40+ = برنامه در پس‌زمینه و حافظه‌ی سیستم کم است (۲۰ = فقط مخفی شدن UI نادیده گرفته می‌شود)
        if (level == 15 || level >= 40) runCatching { runtime.unload() }
    }
}
