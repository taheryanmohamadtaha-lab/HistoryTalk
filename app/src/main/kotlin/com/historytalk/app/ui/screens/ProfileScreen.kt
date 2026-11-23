package com.historytalk.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.historytalk.app.data.db.ConversationEntity
import com.historytalk.app.data.db.unpackList
import com.historytalk.app.ui.ProfileViewModel
import com.historytalk.app.ui.components.ConfirmDialog
import com.historytalk.app.ui.components.EmptyState
import com.historytalk.app.ui.components.FigureAvatar
import com.historytalk.app.ui.components.LoadingState
import com.historytalk.app.ui.components.quoteTypeLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenChat: (Long) -> Unit,
    vm: ProfileViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val missing by vm.missing.collectAsStateWithLifecycle()
    val convs by vm.conversations.collectAsStateWithLifecycle()
    val fav by vm.favorite.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<ConversationEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui?.figure?.name ?: "پروفایل") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
                },
                actions = {
                    IconButton(onClick = vm::toggleFavorite) {
                        Icon(
                            if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (fav) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                        )
                    }
                },
            )
        },
    ) { padding ->
        val data = ui
        when {
            missing -> EmptyState("این شخصیت پیدا نشد", modifier = Modifier.padding(padding))
            data == null -> LoadingState(modifier = Modifier.padding(padding))
            else -> {
                val f = data.figure
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            FigureAvatar(f.name, f.colorHex, size = 84.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(f.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                if (f.nameEn.isNotBlank()) Text(f.nameEn, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(f.lifespan, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Text("${f.nationality} · ${f.civilization}", style = MaterialTheme.typography.bodySmall)
                                Text("${f.role} · ${f.era}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (f.sensitive) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Text(
                                    "این شخصیت از نظر تاریخی بحث‌برانگیز است. پاسخ‌ها به‌صورت روایت تاریخی دقیق ارائه می‌شود؛ " +
                                        "جنایات و پیامدها در صورت پرسش صادقانه بیان می‌شود و ستایش یا ترویج ایدئولوژی نفرت‌محور ممکن نیست.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                    item { Text(f.summary, style = MaterialTheme.typography.bodyLarge) }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { vm.start(ConversationEntity.MODE_FREE, onOpenChat) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("شروع گفتگوی تازه") }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { vm.start(ConversationEntity.MODE_INTERVIEW, onOpenChat) },
                                    modifier = Modifier.weight(1f),
                                ) { Text("مصاحبه‌ی تاریخی") }
                                OutlinedButton(
                                    onClick = { vm.start(ConversationEntity.MODE_CHALLENGE, onOpenChat) },
                                    modifier = Modifier.weight(1f),
                                ) { Text("حالت چالش") }
                            }
                        }
                    }

                    if (convs.isNotEmpty()) {
                        item { SectionTitle("گفتگوهای قبلی") }
                        items(convs, key = { "c${it.id}" }) { c ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onOpenChat(c.id) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(c.title, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(c.updatedAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { toDelete = c }) { Icon(Icons.Default.Delete, contentDescription = "حذف گفتگو") }
                                }
                            }
                        }
                    }

                    if (data.timeline.isNotEmpty()) {
                        item { SectionTitle("خط زمانی") }
                        items(data.timeline, key = { "t${it.id}" }) { t ->
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.width(78.dp)) {
                                    Text(t.year, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    Modifier.padding(top = 6.dp).size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(t.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    val places = f.places.unpackList()
                    if (places.isNotEmpty()) {
                        item {
                            Column {
                                SectionTitle("مکان‌های مهم")
                                Spacer(Modifier.height(6.dp))
                                Text(places.joinToString(" • "), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    val relations = f.relations.unpackList()
                    if (relations.isNotEmpty()) {
                        item {
                            Column {
                                SectionTitle("روابط و شخصیت‌های مرتبط")
                                Spacer(Modifier.height(6.dp))
                                relations.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                            }
                        }
                    }

                    if (data.quotes.isNotEmpty()) {
                        item { SectionTitle("نقل‌قول‌ها") }
                        items(data.quotes, key = { "q${it.id}" }) { q ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("«${q.text}»", style = MaterialTheme.typography.bodyMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AssistChip(onClick = {}, label = { Text(quoteTypeLabel(q.type)) })
                                        if (q.source.isNotBlank()) {
                                            Text("منبع: ${q.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (data.sources.isNotEmpty()) {
                        item {
                            Column {
                                HorizontalDivider()
                                Spacer(Modifier.height(8.dp))
                                SectionTitle("منابع پایگاه دانش این شخصیت")
                                Spacer(Modifier.height(6.dp))
                                data.sources.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { c ->
        ConfirmDialog(
            title = "حذف گفتگو",
            text = "این گفتگو و همه‌ی پیام‌هایش برای همیشه پاک می‌شود.",
            confirm = "حذف",
            onConfirm = { vm.delete(c.id) },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}
