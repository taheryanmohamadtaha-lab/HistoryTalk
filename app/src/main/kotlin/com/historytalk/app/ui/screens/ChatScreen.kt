package com.historytalk.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.historytalk.app.data.db.ConversationEntity
import com.historytalk.app.data.db.MessageEntity
import com.historytalk.app.data.db.unpackList
import com.historytalk.app.data.model.Cat
import com.historytalk.app.ui.ChatViewModel
import com.historytalk.app.ui.components.ConfirmDialog
import com.historytalk.app.ui.components.DisclaimerBanner
import com.historytalk.app.ui.components.startShare

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack: () -> Unit, vm: ChatViewModel = hiltViewModel()) {
    val conv by vm.conversation.collectAsStateWithLifecycle()
    val figure by vm.figure.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val engine by vm.engine.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    val sourceMsg by vm.sourceMessage.collectAsStateWithLifecycle()
    val exportError by vm.exportError.collectAsStateWithLifecycle()

    var input by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }

    val last = messages.lastOrNull()
    // آیتم ۰ = بنر ثابت؛ پیام‌ها از ایندکس ۱
    LaunchedEffect(messages.size, last?.text?.length) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size)
    }
    LaunchedEffect(exportError) {
        exportError?.let { snackbar.showSnackbar(it); vm.clearExportError() }
    }

    val mode = conv?.mode ?: ConversationEntity.MODE_FREE
    val hasUserMessage = messages.any { it.role == MessageEntity.USER }
    val suggestions = remember(figure) { figure?.suggestions?.unpackList().orEmpty() }

    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(figure?.name ?: "گفتگو", maxLines = 1, style = MaterialTheme.typography.titleMedium)
                        Text(
                            figure?.lifespan.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
                },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "گزینه‌های گفتگو") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("حالت آزاد") }, onClick = { menu = false; vm.setMode(ConversationEntity.MODE_FREE) })
                        DropdownMenuItem(text = { Text("مصاحبه‌ی تاریخی") }, onClick = { menu = false; vm.setMode(ConversationEntity.MODE_INTERVIEW) })
                        DropdownMenuItem(text = { Text("چالش (شخصیت از شما می‌پرسد)") }, onClick = { menu = false; vm.setMode(ConversationEntity.MODE_CHALLENGE) })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("خروجی TXT") }, onClick = { menu = false; vm.export(false) { ctx.startShare(it) } })
                        DropdownMenuItem(text = { Text("خروجی PDF") }, onClick = { menu = false; vm.export(true) { ctx.startShare(it) } })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("حذف این گفتگو") }, onClick = { menu = false; confirmDelete = true })
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                Column {
                    if (mode == ConversationEntity.MODE_INTERVIEW && !busy) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(onClick = { vm.askNextInterview() }, label = { Text("سؤال بعدی مصاحبه") })
                        }
                    } else if (!hasUserMessage && suggestions.isNotEmpty() && !busy) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(suggestions) { s -> SuggestionChip(onClick = { vm.send(s) }, label = { Text(s) }) }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it.take(1000) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(if (mode == ConversationEntity.MODE_CHALLENGE) "پاسخ خود را بنویسید…" else "پیام خود را بنویسید…") },
                            maxLines = 4,
                            shape = RoundedCornerShape(20.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (!busy && input.isNotBlank()) { vm.send(input); input = "" }
                            }),
                        )
                        if (busy) {
                            Button(
                                onClick = vm::stop,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                                modifier = Modifier.semantics { contentDescription = "توقف تولید پاسخ" },
                            ) { Text("■ توقف") }
                        } else {
                            FilledIconButton(
                                onClick = { vm.send(input); input = "" },
                                enabled = input.isNotBlank(),
                                modifier = Modifier.size(52.dp),
                            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "ارسال") }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            engine?.let { e ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "حالت فعال: ${e.label} — ${e.detail}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f),
                        )
                        if (mode == ConversationEntity.MODE_CHALLENGE) {
                            Text("چالش · امتیاز ${conv?.score ?: 0}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "banner") { DisclaimerBanner() }
                items(messages, key = { it.id }) { m ->
                    MessageItem(
                        m = m,
                        typing = busy && m.id == last?.id && m.role == MessageEntity.ASSISTANT && m.text.isEmpty(),
                        onSources = { vm.showSources(m) },
                    )
                }
            }
        }
    }

    sources?.let { list ->
        AlertDialog(
            onDismissRequest = vm::dismissSources,
            title = { Text("حقیقت‌سنجی تاریخی") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text(
                            if (list.isEmpty()) "این پیام به بخش مشخصی از پایگاه دانش وابسته نیست."
                            else "این پاسخ بر پایه‌ی بخش‌های زیر از پایگاه دانش ساخته شده است" +
                                if (sourceMsg?.engine == MessageEntity.ENGINE_MODEL) " (مدل ممکن است جمله‌بندی را تغییر داده باشد؛ مرجع همین بخش‌هاست):" else ":",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    items(list, key = { it.id }) { k ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(Cat.labels[k.category] ?: k.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(k.text, style = MaterialTheme.typography.bodyMedium)
                            if (k.source.isNotBlank()) Text("منبع: ${k.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = vm::dismissSources) { Text("بستن") } },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "حذف گفتگو",
            text = "این گفتگو و همه‌ی پیام‌هایش برای همیشه پاک می‌شود.",
            confirm = "حذف",
            onConfirm = { vm.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun MessageItem(m: MessageEntity, typing: Boolean, onSources: () -> Unit) {
    val mine = m.role == MessageEntity.USER
    if (m.engine == MessageEntity.ENGINE_NOTICE) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(m.text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
        return
    }
    val bubbleColor = when {
        mine -> MaterialTheme.colorScheme.primaryContainer
        m.refusal -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        mine -> MaterialTheme.colorScheme.onPrimaryContainer
        m.refusal -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    // در RTL «ابتدا» سمت راست است؛ پیام کاربر ابتدا، پیام شخصیت انتها
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = bubbleColor,
            contentColor = textColor,
            tonalElevation = if (mine) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(0.88f),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when {
                    typing -> TypingDots()
                    m.text.isEmpty() -> Text("(پاسخ متوقف شد)", style = MaterialTheme.typography.bodySmall)
                    else -> Text(m.text, style = MaterialTheme.typography.bodyLarge)
                }
                if (!mine && !typing && m.text.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val tag = when (m.engine) {
                            MessageEntity.ENGINE_MODEL -> "مدل"
                            MessageEntity.ENGINE_SAFETY -> "فیلتر ایمنی"
                            else -> "حالت پایه"
                        }
                        Text(tag, style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.7f))
                        if (!m.refusal) {
                            TextButton(onClick = onSources, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                Text("حقیقت‌سنجی تاریخی", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingDots() {
    val t = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.semantics { contentDescription = "در حال نوشتن پاسخ" }) {
        repeat(3) { i ->
            val a by t.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse),
                label = "dot$i",
            )
            Box(Modifier.size(8.dp).alpha(a).background(MaterialTheme.colorScheme.onSurface, CircleShape))
        }
    }
}
