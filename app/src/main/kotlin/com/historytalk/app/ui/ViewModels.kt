package com.historytalk.app.ui

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.historytalk.app.data.db.ConversationEntity
import com.historytalk.app.data.db.FigureEntity
import com.historytalk.app.data.db.KnowledgeEntity
import com.historytalk.app.data.db.MessageEntity
import com.historytalk.app.data.db.QuoteEntity
import com.historytalk.app.data.db.SearchHit
import com.historytalk.app.data.db.TimelineEntity
import com.historytalk.app.data.repo.AppSettings
import com.historytalk.app.data.repo.ChatRepository
import com.historytalk.app.data.repo.DataPackLoader
import com.historytalk.app.data.repo.FigureRepository
import com.historytalk.app.data.repo.SettingKeys
import com.historytalk.app.data.repo.SettingsRepository
import com.historytalk.app.engine.DownloadState
import com.historytalk.app.engine.EngineResolver
import com.historytalk.app.engine.EngineStatus
import com.historytalk.app.engine.ModelManager
import com.historytalk.app.engine.ModelRuntime
import com.historytalk.app.engine.PersianText
import com.historytalk.app.engine.RuntimeState
import com.historytalk.app.export.ChatExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private val whileSubscribed = SharingStarted.WhileSubscribed(5_000)

// ───────────────────────── ریشه ─────────────────────────

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val loader: DataPackLoader,
) : ViewModel() {
    val settingsState: StateFlow<AppSettings?> =
        settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val dataReady: StateFlow<Boolean> = loader.ready

    init { viewModelScope.launch { runCatching { loader.ensureLoaded() } } }

    fun acceptDisclaimer() { viewModelScope.launch { settings.set(SettingKeys.DISCLAIMER, "1") } }
}

// ───────────────────────── فهرست شخصیت‌ها ─────────────────────────

data class FigureCard(val figure: FigureEntity, val favorite: Boolean)
data class ListFilter(val query: String = "", val era: String? = null, val role: String? = null, val favoritesOnly: Boolean = false)

@HiltViewModel
class FigureListViewModel @Inject constructor(
    private val repo: FigureRepository,
    resolver: EngineResolver,
) : ViewModel() {
    private val _filter = MutableStateFlow(ListFilter())
    val filter: StateFlow<ListFilter> = _filter.asStateFlow()
    val engine: StateFlow<EngineStatus?> = resolver.status.stateIn(viewModelScope, whileSubscribed, null)

    /** null یعنی هنوز در حال بارگذاری. */
    val figures: StateFlow<List<FigureCard>?> = combine(repo.observeFigures(), repo.observeFavorites(), _filter) { all, favs, f ->
        val favSet = favs.toSet()
        val q = PersianText.normalize(f.query)
        all.filter { e ->
            (f.era == null || e.eraGroup == f.era) &&
                (f.role == null || e.roleGroup == f.role) &&
                (!f.favoritesOnly || e.id in favSet) &&
                (q.isEmpty() || PersianText.normalize("${e.name} ${e.nameEn} ${e.role} ${e.civilization} ${e.era} ${e.nationality}").contains(q))
        }.map { FigureCard(it, it.id in favSet) }
    }.stateIn(viewModelScope, whileSubscribed, null)

    fun setQuery(q: String) = _filter.update { it.copy(query = q) }
    fun toggleEra(g: String) = _filter.update { it.copy(era = if (it.era == g) null else g) }
    fun toggleRole(g: String) = _filter.update { it.copy(role = if (it.role == g) null else g) }
    fun toggleFavoritesOnly() = _filter.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    fun clearFilters() { _filter.value = ListFilter() }
    fun toggleFavorite(card: FigureCard) { viewModelScope.launch { repo.setFavorite(card.figure.id, !card.favorite) } }
}

// ───────────────────────── پروفایل ─────────────────────────

data class ProfileUi(
    val figure: FigureEntity,
    val timeline: List<TimelineEntity>,
    val quotes: List<QuoteEntity>,
    val sources: List<String>,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val figures: FigureRepository,
    private val chats: ChatRepository,
) : ViewModel() {
    val figureId: String = savedStateHandle.get<String>("id").orEmpty()

    private val _ui = MutableStateFlow<ProfileUi?>(null)
    val ui: StateFlow<ProfileUi?> = _ui.asStateFlow()
    private val _missing = MutableStateFlow(false)
    val missing: StateFlow<Boolean> = _missing.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> =
        chats.observeForFigure(figureId).stateIn(viewModelScope, whileSubscribed, emptyList())
    val favorite: StateFlow<Boolean> =
        figures.observeFavorites().combine(MutableStateFlow(figureId)) { favs, id -> id in favs }
            .stateIn(viewModelScope, whileSubscribed, false)

    init {
        viewModelScope.launch {
            val f = figures.figure(figureId)
            if (f == null) _missing.value = true
            else _ui.value = ProfileUi(f, figures.timeline(figureId), figures.quotes(figureId), figures.sources(figureId))
        }
    }

    fun toggleFavorite() { viewModelScope.launch { figures.setFavorite(figureId, !favorite.value) } }

    fun start(mode: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(chats.createConversation(figureId, mode)) }
    }

    fun delete(id: Long) { viewModelScope.launch { chats.deleteConversation(id) } }
}

// ───────────────────────── چت ─────────────────────────

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chats: ChatRepository,
    private val figures: FigureRepository,
    resolver: EngineResolver,
    private val exporter: ChatExporter,
) : ViewModel() {
    val convId: Long = savedStateHandle.get<Long>("convId") ?: -1L

    val conversation: StateFlow<ConversationEntity?> =
        chats.observeConversation(convId).stateIn(viewModelScope, whileSubscribed, null)
    val messages: StateFlow<List<MessageEntity>> =
        chats.observeMessages(convId).stateIn(viewModelScope, whileSubscribed, emptyList())
    val engine: StateFlow<EngineStatus?> = resolver.status.stateIn(viewModelScope, whileSubscribed, null)

    private val _figure = MutableStateFlow<FigureEntity?>(null)
    val figure: StateFlow<FigureEntity?> = _figure.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _sources = MutableStateFlow<List<KnowledgeEntity>?>(null)
    val sources: StateFlow<List<KnowledgeEntity>?> = _sources.asStateFlow()
    private val _sourceMsg = MutableStateFlow<MessageEntity?>(null)
    val sourceMessage: StateFlow<MessageEntity?> = _sourceMsg.asStateFlow()
    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch { chats.conversation(convId)?.let { _figure.value = figures.figure(it.figureId) } }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        job = viewModelScope.launch {
            try { block() } finally { _busy.value = false }
        }
    }

    fun send(text: String) { if (text.isNotBlank()) launchBusy { chats.send(convId, text) } }
    fun askNextInterview() = launchBusy { chats.askNextInterview(convId) }

    /** توقف تولید پاسخ؛ متن تا اینجا ذخیره می‌شود. */
    fun stop() { job?.cancel() }

    fun setMode(mode: String) { viewModelScope.launch { chats.setMode(convId, mode) } }

    fun showSources(m: MessageEntity) {
        viewModelScope.launch { _sources.value = chats.sources(m); _sourceMsg.value = m }
    }
    fun dismissSources() { _sources.value = null; _sourceMsg.value = null }

    fun export(pdf: Boolean, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            try {
                val conv = chats.conversation(convId) ?: return@launch
                val msgs = chats.messages(convId)
                val f = _figure.value
                val file = if (pdf) exporter.exportPdf(f, conv, msgs) else exporter.exportTxt(f, conv, msgs)
                onReady(exporter.shareIntent(file, if (pdf) "application/pdf" else "text/plain"))
            } catch (t: Throwable) {
                _exportError.value = "خروجی گرفتن ناموفق بود: ${t.message ?: t.javaClass.simpleName}"
            }
        }
    }
    fun clearExportError() { _exportError.value = null }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch { chats.deleteConversation(convId); onDone() }
    }

    override fun onCleared() { job?.cancel() }
}

// ───────────────────────── تاریخچه ─────────────────────────

data class HistoryRow(val conv: ConversationEntity, val figureName: String)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val chats: ChatRepository,
    figures: FigureRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _hits = MutableStateFlow<List<SearchHit>>(emptyList())
    val hits: StateFlow<List<SearchHit>> = _hits.asStateFlow()

    val rows: StateFlow<List<HistoryRow>> = combine(chats.observeConversations(), figures.observeFigures()) { convs, figs ->
        val names = figs.associate { it.id to it.name }
        convs.map { HistoryRow(it, names[it.figureId] ?: it.figureId) }
    }.stateIn(viewModelScope, whileSubscribed, emptyList())

    private var searchJob: Job? = null
    fun setQuery(q: String) {
        _query.value = q
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _hits.value = if (q.trim().length < 2) emptyList() else chats.search(q)
        }
    }

    fun delete(id: Long) { viewModelScope.launch { chats.deleteConversation(id); setQuery(_query.value) } }
}

// ───────────────────────── تنظیمات ─────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val loader: DataPackLoader,
    private val models: ModelManager,
    private val runtime: ModelRuntime,
    private val chats: ChatRepository,
    private val exporter: ChatExporter,
    resolver: EngineResolver,
) : ViewModel() {
    val settingsState: StateFlow<AppSettings> =
        settings.settings.stateIn(viewModelScope, whileSubscribed, AppSettings())
    val engine: StateFlow<EngineStatus?> = resolver.status.stateIn(viewModelScope, whileSubscribed, null)
    val device = resolver.device
    val download: StateFlow<DownloadState> = models.state
    val installed: StateFlow<String?> = models.installed
    val runtimeState: StateFlow<RuntimeState> = runtime.state

    private val _info = MutableStateFlow<String?>(null)
    val info: StateFlow<String?> = _info.asStateFlow()
    private val _probe = MutableStateFlow<String?>(null)
    val probe: StateFlow<String?> = _probe.asStateFlow()

    fun installedSizeMb(): Long = models.installedSizeBytes() / 1_048_576
    fun partialMb(): Long = models.partialBytes() / 1_048_576

    fun setTheme(v: String) { viewModelScope.launch { settings.set(SettingKeys.THEME, v) } }
    fun setFont(v: Float) { viewModelScope.launch { settings.set(SettingKeys.FONT, v.toString()) } }
    fun setEngine(mode: String) {
        viewModelScope.launch {
            settings.set(SettingKeys.ENGINE, mode)
            if (mode == "base") runtime.unload() // آزادسازی حافظه
        }
    }
    fun setModelUrl(url: String) { viewModelScope.launch { settings.set(SettingKeys.MODEL_URL, url.trim()) } }

    fun probeUrl(url: String) {
        _probe.value = "در حال بررسی…"
        viewModelScope.launch {
            models.probe(url.trim()).fold(
                onSuccess = { _probe.value = "حجم فایل: ${it / 1_048_576} مگابایت" },
                onFailure = { _probe.value = "خطا: ${it.message ?: "نامشخص"}" },
            )
        }
    }

    fun startDownload(url: String) {
        viewModelScope.launch { settings.set(SettingKeys.MODEL_URL, url.trim()) }
        models.start(url.trim())
    }
    fun pauseDownload() = models.pause()
    fun retryModel() = runtime.retry()

    fun deleteModel() {
        viewModelScope.launch { runtime.unload(); models.deleteAll(); _info.value = "مدل حذف شد." }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chats.deleteAllHistory()
            withContext(Dispatchers.IO) { exporter.clearCache() }
            _info.value = "تاریخچه‌ی گفتگوها پاک شد."
        }
    }

    fun importPack(text: String) {
        viewModelScope.launch {
            loader.importPack(text).fold(
                onSuccess = { _info.value = "شخصیت «$it» افزوده شد." },
                onFailure = { _info.value = "افزودن ناموفق بود: ${it.message ?: "JSON نامعتبر"}" },
            )
        }
    }

    fun clearInfo() { _info.value = null }
}
