package com.historytalk.app.data.repo

import android.content.Context
import androidx.room.withTransaction
import com.historytalk.app.data.db.*
import com.historytalk.app.data.model.FigurePack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** وارد کردن پک‌های JSON (داخلی در assets و خارجی در filesDir/packs) به Room. */
@Singleton
class DataPackLoader @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mutex = Mutex()
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val idRegex = Regex("^[a-z0-9_]{2,40}$")

    suspend fun ensureLoaded() {
      mutex.withLock {
        if (_ready.value) return@withLock
        withContext(Dispatchers.IO) {
            val installed = settings.raw(SettingKeys.PACK_VERSION)
            if (installed != DATA_VERSION.toString() || db.figureDao().count() == 0) {
                ctx.assets.list("figures")?.filter { it.endsWith(".json") }?.sorted()?.forEach { name ->
                    runCatching {
                        val text = ctx.assets.open("figures/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }
                        insert(json.decodeFromString<FigurePack>(text))
                    }
                }
                settings.set(SettingKeys.PACK_VERSION, DATA_VERSION.toString())
            }
            File(ctx.filesDir, "packs").listFiles { f -> f.extension == "json" }?.forEach { f ->
                runCatching { insert(json.decodeFromString<FigurePack>(f.readText(Charsets.UTF_8))) }
            }
        }
        _ready.value = true
      }
    }

    /** افزودن شخصیت جدید از متن JSON (از تنظیمات). نتیجه: نام شخصیت یا پیام خطا. */
    suspend fun importPack(text: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val p = json.decodeFromString<FigurePack>(text)
            require(idRegex.matches(p.id)) { "شناسه باید فقط شامل حروف کوچک انگلیسی، عدد و _ باشد (۲ تا ۴۰ نویسه)" }
            require(p.name.isNotBlank() && p.summary.isNotBlank()) { "نام و خلاصه الزامی است" }
            require(p.knowledge.isNotEmpty()) { "حداقل یک آیتم دانش لازم است" }
            insert(p)
            val dir = File(ctx.filesDir, "packs").apply { mkdirs() }
            File(dir, "${p.id}.json").writeText(text, Charsets.UTF_8)
            p.name
        }
    }

    private suspend fun insert(p: FigurePack) {
        db.withTransaction {
            val c = db.contentDao()
            c.deleteTimeline(p.id); c.deleteKnowledge(p.id); c.deleteQuotes(p.id); c.deleteChallenges(p.id)
            db.figureDao().upsert(
                FigureEntity(
                    id = p.id, name = p.name, nameEn = p.nameEn, lifespan = p.lifespan, era = p.era, eraGroup = p.eraGroup,
                    civilization = p.civilization, role = p.role, roleGroup = p.roleGroup, nationality = p.nationality,
                    sensitive = p.sensitive, colorHex = p.colorHex, summary = p.summary, voice = p.voice,
                    openers = p.openers.packList(), closers = p.closers.packList(), vocabulary = p.vocabulary.packList(),
                    places = p.places.packList(), relations = p.relations.packList(), suggestions = p.suggestions.packList(),
                ),
            )
            c.insertTimeline(p.timeline.mapIndexed { i, t -> TimelineEntity(figureId = p.id, position = i, year = t.year, text = t.text) })
            c.insertKnowledge(p.knowledge.mapIndexed { i, k ->
                KnowledgeEntity("${p.id}#$i", p.id, k.category, k.keywords.packList(), k.text, k.source)
            })
            c.insertQuotes(p.quotes.map { QuoteEntity(figureId = p.id, text = it.text, source = it.source, type = it.type) })
            c.insertChallenges(p.challenges.mapIndexed { i, ch ->
                ChallengeEntity(figureId = p.id, position = i, question = ch.question, keywords = ch.keywords.packList(), explanation = ch.explanation)
            })
        }
    }

    companion object { const val DATA_VERSION = 1 }
}
