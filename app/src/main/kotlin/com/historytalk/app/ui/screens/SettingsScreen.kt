package com.historytalk.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.historytalk.app.data.repo.EngineModes
import com.historytalk.app.engine.DownloadState
import com.historytalk.app.engine.RuntimeState
import com.historytalk.app.ui.SettingsViewModel
import com.historytalk.app.ui.components.ConfirmDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onAbout: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settingsState.collectAsStateWithLifecycle()
    val engine by vm.engine.collectAsStateWithLifecycle()
    val download by vm.download.collectAsStateWithLifecycle()
    val installed by vm.installed.collectAsStateWithLifecycle()
    val rt by vm.runtimeState.collectAsStateWithLifecycle()
    val info by vm.info.collectAsStateWithLifecycle()
    val probe by vm.probe.collectAsStateWithLifecycle()

    var url by remember(s.modelUrl) { mutableStateOf(s.modelUrl) }
    var fontDraft by remember(s.fontScale) { mutableFloatStateOf(s.fontScale) }
    var confirmHistory by remember { mutableStateOf(false) }
    var confirmModel by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText().take(2_000_000) }
                }.getOrNull()
            }
            if (text.isNullOrBlank()) vm.importPack("") else vm.importPack(text)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── ظاهر ──
            SectionTitle("ظاهر")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "سیستم", "light" to "روشن", "dark" to "تاریک").forEach { (k, label) ->
                    FilterChip(selected = s.themeMode == k, onClick = { vm.setTheme(k) }, label = { Text(label) })
                }
            }
            Text("اندازه‌ی فونت: ${(fontDraft * 100).toInt()}٪", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = fontDraft,
                onValueChange = { fontDraft = it },
                onValueChangeFinished = { vm.setFont(fontDraft) },
                valueRange = 0.8f..1.5f,
            )
            HorizontalDivider()

            // ── موتور ──
            SectionTitle("موتور گفتگو")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("حالت فعال: ${engine?.label ?: "…"}", fontWeight = FontWeight.Bold)
                    Text(engine?.detail.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    Text(
                        "دستگاه: رم ${vm.device.totalRamMb} مگابایت · ${if (vm.device.is64Bit) "۶۴بیتی" else "۳۲بیتی"} · " +
                            if (vm.device.modelCapable) "مناسب اجرای مدل کوچک" else "پیشنهاد: حالت پایه",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(EngineModes.AUTO to "خودکار", EngineModes.BASE to "فقط پایه", EngineModes.MODEL to "مدل (اجباری)").forEach { (k, label) ->
                    FilterChip(selected = s.engineMode == k, onClick = { vm.setEngine(k) }, label = { Text(label) })
                }
            }
            Text(
                "خودکار: اگر مدل دانلود شده و دستگاه توانش را دارد از مدل استفاده می‌شود، وگرنه حالت پایه. " +
                    "«مدل (اجباری)» روی دستگاه ضعیف ممکن است کند شود یا حافظه کم بیاورد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()

            // ── مدل ──
            SectionTitle("مدل زبانی اختیاری (روی دستگاه)")
            Text(
                "برنامه بدون مدل هم کامل کار می‌کند. برای دانلود، نشانی مستقیم و https یک فایل مدل با قالب MediaPipe (پسوند .task یا .bin) را وارد کنید. " +
                    "مدل‌هایی که نیاز به ورود/پذیرش مجوز دارند (مثل برخی مدل‌های Gemma در Hugging Face) با دانلود مستقیم کار نمی‌کنند؛ " +
                    "در آن صورت فایل را جداگانه دانلود کنید. حجم مدل‌ها معمولاً صدها مگابایت تا چند گیگابایت است؛ ترجیحاً با Wi-Fi دانلود کنید. " +
                    "کیفیت پاسخ فارسی مدل‌های کوچک محدود است.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("نشانی مدل (https://…/model.task)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            probe?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            when (val d = download) {
                is DownloadState.Downloading -> {
                    val frac = if (d.total > 0) (d.done.toFloat() / d.total).coerceIn(0f, 1f) else null
                    if (frac != null) LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth())
                    else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        "${d.done / 1_048_576} از ${if (d.total > 0) "${d.total / 1_048_576}" else "؟"} مگابایت",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = vm::pauseDownload) { Text("توقف (پیشرفت نگه داشته می‌شود)") }
                }
                is DownloadState.Probing -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("در حال اتصال…", style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    if (d is DownloadState.Failed) Text(d.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    if (d is DownloadState.Completed) Text("دانلود کامل شد.", style = MaterialTheme.typography.bodySmall)
                    val partial = vm.partialMb()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.probeUrl(url) }, enabled = url.isNotBlank()) { Text("بررسی حجم") }
                        Button(onClick = { vm.startDownload(url) }, enabled = url.isNotBlank()) {
                            Text(if (partial > 0) "ادامه دانلود ($partial مگابایت)" else "دانلود مدل")
                        }
                    }
                }
            }

            if (installed != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("مدل نصب‌شده: $installed (${vm.installedSizeMb()} مگابایت)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            when (val r = rt) {
                                is RuntimeState.Ready -> "وضعیت: بارگذاری شده و آماده"
                                is RuntimeState.Loading -> "وضعیت: در حال بارگذاری…"
                                is RuntimeState.Failed -> "وضعیت: خطا — ${r.message}"
                                is RuntimeState.Unloaded -> "وضعیت: هنوز بارگذاری نشده (با اولین پیام بارگذاری می‌شود)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (rt is RuntimeState.Failed) OutlinedButton(onClick = vm::retryModel) { Text("تلاش مجدد بارگذاری") }
                            OutlinedButton(onClick = { confirmModel = true }) { Text("حذف مدل") }
                        }
                    }
                }
            }
            HorizontalDivider()

            // ── داده ──
            SectionTitle("داده‌ها")
            OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("افزودن شخصیت از فایل JSON")
            }
            OutlinedButton(onClick = { confirmHistory = true }, modifier = Modifier.fillMaxWidth()) { Text("پاک کردن تاریخچه‌ی همه‌ی گفتگوها") }
            HorizontalDivider()

            OutlinedButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) { Text("درباره‌ی برنامه، منابع و محدودیت‌ها") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmHistory) ConfirmDialog("پاک کردن تاریخچه", "همه‌ی گفتگوها برای همیشه پاک می‌شود.", "پاک کن", { vm.clearHistory() }, { confirmHistory = false })
    if (confirmModel) ConfirmDialog("حذف مدل", "فایل مدل و دانلود ناقص حذف می‌شود.", "حذف", { vm.deleteModel() }, { confirmModel = false })
    info?.let {
        AlertDialog(
            onDismissRequest = vm::clearInfo,
            text = { Text(it) },
            confirmButton = { TextButton(onClick = vm::clearInfo) { Text("باشه") } },
        )
    }
}
