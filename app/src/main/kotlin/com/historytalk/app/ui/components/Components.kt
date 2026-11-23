package com.historytalk.app.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.historytalk.app.data.model.DISCLAIMER

fun parseColor(hex: String, fallback: Color = Color(0xFF8B6F3A)): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

/** نگاره‌ی نمادین تولیدشده داخل برنامه (بدون عکس خارجی/حق‌نشر): دایره‌ی گرادیان + حرف اول نام. */
@Composable
fun FigureAvatar(name: String, colorHex: String, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    val c = remember(colorHex) { parseColor(colorHex) }
    val dark = remember(c) { lerp(c, Color.Black, 0.35f) }
    Box(
        modifier = modifier
            .size(size)
            .background(Brush.linearGradient(listOf(c, dark)), CircleShape)
            .border(2.dp, Color(0xFFE3BE6B), CircleShape)
            .semantics { contentDescription = "نگاره‌ی نمادین $name" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull { it.isLetter() }?.toString() ?: "؟",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** پیام ثابت آموزشی/ایمنی (الزامی در اولین ورود و هر چت). */
@Composable
fun DisclaimerBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(DISCLAIMER, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun DisclaimerDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = {}, // باید پذیرفته شود
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text("پیش از شروع") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(DISCLAIMER, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "همه‌ی داده‌ها فقط روی دستگاه شما می‌ماند و چیزی به سرور فرستاده نمی‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { Button(onClick = onAccept) { Text("متوجه شدم") } },
    )
}

@Composable
fun LoadingState(text: String = "در حال بارگذاری…", modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EmptyState(title: String, hint: String = "", modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (hint.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(hint, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (action != null) { Spacer(Modifier.height(12.dp)); action() }
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (onRetry != null) { Spacer(Modifier.height(12.dp)); Button(onClick = onRetry) { Text("تلاش دوباره") } }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String = "تأیید", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

fun Context.startShare(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

val eraGroups = listOf("ANCIENT" to "باستان", "MEDIEVAL" to "میانه", "EARLY_MODERN" to "نوین آغازین", "MODERN" to "معاصر")
val roleGroups = listOf("RULER" to "فرمانروا", "THINKER" to "اندیشمند", "LEADER" to "رهبر")

fun quoteTypeLabel(type: String) = when (type) {
    "DOCUMENTED" -> "مستند"
    "ATTRIBUTED" -> "منسوب"
    else -> "بازآفرینی"
}
