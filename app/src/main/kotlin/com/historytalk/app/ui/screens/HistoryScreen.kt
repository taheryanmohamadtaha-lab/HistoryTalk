package com.historytalk.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.historytalk.app.ui.HistoryViewModel
import com.historytalk.app.ui.components.ConfirmDialog
import com.historytalk.app.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** تاریخچه‌ی همه‌ی گفتگوها + جست‌وجو در متن پیام‌ها. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, onOpenChat: (Long) -> Unit, vm: HistoryViewModel = hiltViewModel()) {
    val query by vm.query.collectAsStateWithLifecycle()
    val hits by vm.hits.collectAsStateWithLifecycle()
    val rows by vm.rows.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<Long?>(null) }
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }
    val names = remember(rows) { rows.associate { it.conv.figureId to it.figureName } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تاریخچه‌ی گفتگوها") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("جست‌وجو در متن گفتگوها…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Default.Close, contentDescription = "پاک کردن") }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            val searching = query.trim().length >= 2
            when {
                searching && hits.isEmpty() -> EmptyState("نتیجه‌ای پیدا نشد", "عبارت دیگری را امتحان کنید.")
                searching -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(hits, key = { it.messageId }) { h ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenChat(h.conversationId) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(names[h.figureId] ?: h.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(h.text, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                Text(fmt.format(Date(h.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                rows.isEmpty() -> EmptyState("هنوز گفتگویی ندارید", "یک شخصیت را انتخاب کنید و گفتگو را شروع کنید.")
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rows, key = { it.conv.id }) { r ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenChat(r.conv.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(r.figureName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    Text(r.conv.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                                    Text(fmt.format(Date(r.conv.updatedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { toDelete = r.conv.id }) { Icon(Icons.Default.Delete, contentDescription = "حذف گفتگو") }
                            }
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { id ->
        ConfirmDialog(
            title = "حذف گفتگو",
            text = "این گفتگو و همه‌ی پیام‌هایش برای همیشه پاک می‌شود.",
            confirm = "حذف",
            onConfirm = { vm.delete(id) },
            onDismiss = { toDelete = null },
        )
    }
}
