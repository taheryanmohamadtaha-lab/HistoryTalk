package com.historytalk.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.historytalk.app.BuildConfig
import com.historytalk.app.ui.components.DisclaimerBanner

private val limitations = listOf(
    "حالت پایه پاسخ را از متن‌های ثبت‌شده‌ی پایگاه دانش می‌سازد؛ برای پرسش‌هایی که در دانش ثبت‌شده نیست، صادقانه می‌گوید پاسخ مستندی ندارد.",
    "بازیابی دانش کلیدواژه‌ای است (نه درک معنایی عمیق)؛ جمله‌بندی‌های غیرمعمول ممکن است پاسخ مرتبط پیدا نکنند.",
    "مدل کوچک روی دستگاه ممکن است اشتباه کند یا جمله‌بندی را عوض کند؛ برای همین دکمه‌ی «حقیقت‌سنجی تاریخی» مرجع را نشان می‌دهد.",
    "فیلتر ایمنی قاعده‌محور و آفلاین است و بی‌نقص نیست؛ ممکن است برخی درخواست‌های مجاز را رد کند یا بازنویسی‌های خاص را نگیرد.",
    "نگاره‌ی شخصیت‌ها نمادین و ساخته‌ی داخل برنامه است (عکس واقعی یا لینک خارجی استفاده نشده).",
    "ورودی‌های دانش خلاصه و آموزشی‌اند و جایگزین منابع علمی و تاریخ‌نگاری تخصصی نیستند.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("درباره‌ی برنامه") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("گفتگوی تاریخ", style = MaterialTheme.typography.headlineSmall)
            Text("نسخه‌ی ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium)
            DisclaimerBanner()
            Text(
                "برنامه‌ای آموزشی و آفلاین برای گفتگوی متنی با بازآفرینی شخصیت‌های تاریخی؛ " +
                    "با پایگاه دانش محلی، مدل اختیاری روی دستگاه و حالت پایه بدون مدل.",
                style = MaterialTheme.typography.bodyMedium,
            )
            SectionTitle("حریم خصوصی")
            Text(
                "همه‌ی داده‌ها (گفتگوها، تنظیمات، مدل) فقط روی دستگاه شما ذخیره می‌شود. برنامه ردیاب و آنالیتیکس ندارد " +
                    "و مجوز اینترنت فقط برای دانلود اختیاری مدل است.",
                style = MaterialTheme.typography.bodyMedium,
            )
            SectionTitle("منابع")
            Text(
                "منبع هر بخش دانشی در پروفایل شخصیت و در پنجره‌ی «حقیقت‌سنجی تاریخی» نمایش داده می‌شود. " +
                    "نقل‌قول‌ها با برچسب «مستند»، «منسوب» یا «بازآفرینی» مشخص شده‌اند. فونت: وزیرمتن (مجوز OFL).",
                style = MaterialTheme.typography.bodyMedium,
            )
            SectionTitle("محدودیت‌های شناخته‌شده")
            limitations.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            SectionTitle("سازنده")
            Text("محمدطاها طاهریان", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
