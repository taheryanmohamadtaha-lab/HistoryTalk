package com.historytalk.app.data.repo

import com.historytalk.app.data.db.*
import com.historytalk.app.data.model.Cat
import com.historytalk.app.engine.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,
    private val figures: FigureRepository,
    private val runtime: ModelRuntime,
    private val resolver: EngineResolver,
    private val models: ModelManager,
) {
    private val retriever = KnowledgeRetriever()
    private val genMutex = Mutex()

    fun observeConversations(): Flow<List<ConversationEntity>> = chatDao.observeConversations()
    fun observeForFigure(id: String): Flow<List<ConversationEntity>> = chatDao.observeForFigure(id)
    fun observeConversation(id: Long): Flow<ConversationEntity?> = chatDao.observeConversation(id)
    fun observeMessages(id: Long): Flow<List<MessageEntity>> = chatDao.observeMessages(id)
    suspend fun conversation(id: Long) = chatDao.getConversation(id)
    suspend fun messages(id: Long) = chatDao.messages(id)
    suspend fun search(q: String): List<SearchHit> = if (q.trim().length < 2) emptyList() else chatDao.search(q.trim())

    suspend fun createConversation(figureId: String, mode: String = ConversationEntity.MODE_FREE): Long {
        val fig = figures.figure(figureId)
        val now = System.currentTimeMillis()
        val id = chatDao.insertConversation(
            ConversationEntity(figureId = figureId, title = "گفتگو با ${fig?.name ?: ""}", createdAt = now, updatedAt = now),
        )
        if (mode != ConversationEntity.MODE_FREE) setMode(id, mode)
        return id
    }

    suspend fun deleteConversation(id: Long) {
        chatDao.deleteMessagesOf(id)
        chatDao.deleteConversation(id)
    }

    suspend fun deleteAllHistory() {
        chatDao.deleteAllMessages()
        chatDao.deleteAllConversations()
    }

    suspend fun sources(message: MessageEntity): List<KnowledgeEntity> =
        figures.knowledgeByIds(message.sourceIds.split(',').filter { it.isNotBlank() })

    // ───────────── حالت‌ها ─────────────

    suspend fun setMode(convId: Long, mode: String) {
        val conv = chatDao.getConversation(convId) ?: return
        val fig = figures.profile(conv.figureId) ?: return
        when (mode) {
            ConversationEntity.MODE_CHALLENGE -> {
                val chs = figures.challenges(fig.id)
                if (chs.isEmpty()) {
                    notice(convId, "برای این شخصیت هنوز چالشی ثبت نشده است.")
                    chatDao.updateConversation(conv.copy(mode = ConversationEntity.MODE_FREE))
                    return
                }
                chatDao.updateConversation(conv.copy(mode = mode, qIndex = 0, score = 0, updatedAt = now()))
                assistant(convId, "چالش تاریخی آغاز شد! من از تو می‌پرسم؛ اگر پاسخ را نمی‌دانی بنویس «نمی‌دانم».\n\nسؤال ۱ از ${chs.size}: ${chs[0].question}")
            }
            ConversationEntity.MODE_INTERVIEW -> {
                chatDao.updateConversation(conv.copy(mode = mode, qIndex = 0, updatedAt = now()))
                notice(convId, "حالت مصاحبه‌ی تاریخی: سؤال‌های ساختاریافته را به ترتیب با دکمه‌ی «سؤال بعدی» بپرسید یا آزادانه بنویسید.")
            }
            else -> chatDao.updateConversation(conv.copy(mode = ConversationEntity.MODE_FREE, updatedAt = now()))
        }
    }

    suspend fun askNextInterview(convId: Long) {
        val conv = chatDao.getConversation(convId) ?: return
        val fig = figures.profile(conv.figureId) ?: return
        val q = fig.suggestions.getOrNull(conv.qIndex)
        if (q == null) {
            notice(convId, "مصاحبه به پایان رسید. می‌توانید آزادانه ادامه دهید.")
            chatDao.updateConversation(conv.copy(mode = ConversationEntity.MODE_FREE))
            return
        }
        chatDao.updateConversation(conv.copy(qIndex = conv.qIndex + 1))
        send(convId, q)
    }

    // ───────────── ارسال پیام ─────────────

    suspend fun send(convId: Long, userText: String) {
        val text = userText.trim().take(1000)
        if (text.isEmpty()) return
        val conv = chatDao.getConversation(convId) ?: return
        val fig = figures.profile(conv.figureId) ?: return

        val history = chatDao.messages(convId)
            .filter { it.engine != MessageEntity.ENGINE_NOTICE && !it.refusal }
            .takeLast(6).map { HistoryTurn(it.role == MessageEntity.USER, it.text) }

        chatDao.insertMessage(MessageEntity(conversationId = convId, role = MessageEntity.USER, text = text, createdAt = now()))
        val isFirst = history.isEmpty()
        chatDao.updateConversation((chatDao.getConversation(convId) ?: conv).copy(
            updatedAt = now(),
            title = if (isFirst) text.take(40) else conv.title,
        ))

        // ۱) فیلتر ورودی
        val verdict = SafetyFilter.checkInput(text)
        if (verdict is SafetyVerdict.Blocked) {
            chatDao.insertMessage(
                MessageEntity(
                    conversationId = convId, role = MessageEntity.ASSISTANT, text = SafetyFilter.refusalMessage(verdict.reason),
                    createdAt = now(), engine = MessageEntity.ENGINE_SAFETY, refusal = true,
                ),
            )
            return
        }

        // ۲) چالش: ورودی کاربر پاسخ سؤال است
        if (conv.mode == ConversationEntity.MODE_CHALLENGE) {
            handleChallengeAnswer(convId, fig, text)
            return
        }

        genMutex.withLock { respond(convId, fig, text, history) }
    }

    private suspend fun respond(convId: Long, fig: FigureProfile, text: String, history: List<HistoryTurn>) {
        val category = QuestionClassifier.classify(text)
        val ranked = if (category == Cat.GREETING || category == Cat.INTRO) emptyList()
        else retriever.rank(text, figures.docs(fig.id), category, topK = 3)
        val base = ResponseComposer.compose(fig, text, category, ranked)

        var status = resolver.current()
        val smalltalk = category == Cat.GREETING || category == Cat.INTRO
        val tryModel = status.wantsModel && !smalltalk && ranked.isNotEmpty()

        val placeholderEngine = if (tryModel) MessageEntity.ENGINE_MODEL else MessageEntity.ENGINE_BASE
        val msgId = chatDao.insertMessage(
            MessageEntity(conversationId = convId, role = MessageEntity.ASSISTANT, text = "", createdAt = now(), engine = placeholderEngine),
        )
        val srcIds = base.sourceIds.joinToString(",")

        if (tryModel) {
            val ready = runtime.awaitReady(timeoutMs = 25_000)
            status = resolver.current()
            if (ready && status.useModel) {
                val out = generateWithModel(msgId, fig, ranked, history, text, srcIds)
                if (out != null) return
            } else {
                notice(convId, "مدل آماده نشد (${status.detail}). این پاسخ با حالت پایه ساخته شد.")
            }
        }
        streamBase(msgId, base.text, srcIds)
    }

    /** true یعنی پاسخ مدل با موفقیت ثبت شد؛ null یعنی باید به حالت پایه برگشت. */
    private suspend fun generateWithModel(
        msgId: Long, fig: FigureProfile, ranked: List<ScoredDoc>, history: List<HistoryTurn>, userText: String, srcIds: String,
    ): Boolean? {
        val template = PromptTemplate.forModelFile(models.installed.value ?: "")
        val prompt = PromptBuilder.build(template, PromptBuilder.systemPrompt(fig, ranked.map { it.doc }), history, userText)
        val sb = StringBuilder()
        var lastFlush = 0L
        try {
            runtime.generate(prompt).collect { piece ->
                sb.append(piece)
                val t = System.currentTimeMillis()
                if (t - lastFlush > 90) {
                    chatDao.updateMessage(msgId, sb.toString(), srcIds, MessageEntity.ENGINE_MODEL)
                    lastFlush = t
                }
            }
        } catch (ce: CancellationException) {
            runtime.stop()
            withContext(NonCancellable) {
                chatDao.updateMessage(msgId, sb.toString().trim().ifEmpty { "(متوقف شد)" }, srcIds, MessageEntity.ENGINE_MODEL)
            }
            throw ce
        } catch (t: Throwable) {
            runtime.markFailed(t.message ?: t.javaClass.simpleName)
            return null
        }
        val out = sb.toString().trim()
        if (out.isEmpty()) return null
        // فیلتر خروجی
        if (SafetyFilter.checkOutput(out) is SafetyVerdict.Blocked) {
            chatDao.updateMessage(msgId, "", "", MessageEntity.ENGINE_BASE)
            return null
        }
        chatDao.updateMessage(msgId, out, srcIds, MessageEntity.ENGINE_MODEL)
        return true
    }

    private suspend fun streamBase(msgId: Long, fullText: String, srcIds: String) {
        // فیلتر خروجی روی الگوها هم اعمال می‌شود
        val text = if (SafetyFilter.checkOutput(fullText) is SafetyVerdict.Blocked)
            "این پاسخ توسط فیلتر ایمنی خروجی رد شد. لطفاً پرسش را به شکل دیگری مطرح کنید." else fullText
        val sb = StringBuilder()
        chatDao.updateMessage(msgId, "", srcIds, MessageEntity.ENGINE_BASE)
        for (chunk in ResponseComposer.chunks(text)) {
            sb.append(chunk)
            chatDao.updateMessage(msgId, sb.toString(), srcIds, MessageEntity.ENGINE_BASE)
            delay(12)
        }
        chatDao.updateMessage(msgId, text, srcIds, MessageEntity.ENGINE_BASE)
    }

    private suspend fun handleChallengeAnswer(convId: Long, fig: FigureProfile, text: String) {
        val conv = chatDao.getConversation(convId) ?: return
        val chs = figures.challenges(fig.id)
        val cur = chs.getOrNull(conv.qIndex)
        if (cur == null) {
            chatDao.updateConversation(conv.copy(mode = ConversationEntity.MODE_FREE))
            notice(convId, "چالش پایان یافته است.")
            return
        }
        val norm = PersianText.normalize(text)
        val skip = listOf("نمی دانم", "نمیدانم", "نمی دونم", "پاس", "نمیدونم").any { norm.contains(PersianText.normalize(it)) }
        val correct = !skip && cur.keywords.unpackList().any { norm.contains(PersianText.normalize(it)) }
        val score = conv.score + if (correct) 1 else 0
        val head = when { correct -> "درست است! "; skip -> "اشکالی ندارد. "; else -> "این پاسخ دقیق نیست. " }
        val next = conv.qIndex + 1
        val msg = buildString {
            append(head).append(cur.explanation)
            if (next < chs.size) append("\n\nسؤال ${next + 1} از ${chs.size}: ").append(chs[next].question)
            else append("\n\nچالش تمام شد. امتیاز تو: $score از ${chs.size}. برای ادامه می‌توانی آزادانه سؤال بپرسی.")
        }
        chatDao.updateConversation(
            if (next < chs.size) conv.copy(qIndex = next, score = score, updatedAt = now())
            else conv.copy(mode = ConversationEntity.MODE_FREE, qIndex = 0, score = score, updatedAt = now()),
        )
        assistant(convId, msg)
    }

    private suspend fun assistant(convId: Long, text: String) {
        chatDao.insertMessage(
            MessageEntity(conversationId = convId, role = MessageEntity.ASSISTANT, text = text, createdAt = now(), engine = MessageEntity.ENGINE_BASE),
        )
    }

    private suspend fun notice(convId: Long, text: String) {
        chatDao.insertMessage(
            MessageEntity(conversationId = convId, role = MessageEntity.ASSISTANT, text = text, createdAt = now(), engine = MessageEntity.ENGINE_NOTICE),
        )
    }

    private fun now() = System.currentTimeMillis()
}
