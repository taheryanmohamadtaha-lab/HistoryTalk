package com.historytalk.app.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Build

data class DeviceInfo(val totalRamMb: Long, val is64Bit: Boolean) {
    /** مدل‌های کوچک (حدود ۱ میلیارد پارامتر، کوانتیزه) به حدود ۴ گیگ رم و ABI ۶۴بیتی نیاز دارند. */
    val modelCapable: Boolean get() = totalRamMb >= 3500 && is64Bit
}

object DeviceCapability {
    fun inspect(ctx: Context): DeviceInfo {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return DeviceInfo(mi.totalMem / (1024 * 1024), Build.SUPPORTED_64_BIT_ABIS.isNotEmpty())
    }
}
