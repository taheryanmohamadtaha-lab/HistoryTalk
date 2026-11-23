package com.historytalk.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SearchHit(
    val messageId: Long,
    val conversationId: Long,
    val figureId: String,
    val title: String,
    val text: String,
    val createdAt: Long,
)

@Dao
interface FigureDao {
    @Query("SELECT * FROM figures ORDER BY name")
    fun observeAll(): Flow<List<FigureEntity>>

    @Query("SELECT * FROM figures WHERE id = :id")
    suspend fun get(id: String): FigureEntity?

    @Query("SELECT COUNT(*) FROM figures")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(figure: FigureEntity)

    @Query("SELECT figureId FROM favorites")
    fun observeFavorites(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(f: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE figureId = :id")
    suspend fun removeFavorite(id: String)
}

@Dao
interface ContentDao {
    @Query("SELECT * FROM timeline WHERE figureId = :id ORDER BY position")
    suspend fun timeline(id: String): List<TimelineEntity>

    @Query("SELECT * FROM knowledge WHERE figureId = :id")
    suspend fun knowledge(id: String): List<KnowledgeEntity>

    @Query("SELECT DISTINCT source FROM knowledge WHERE figureId = :id AND source != '' ORDER BY source")
    suspend fun sourcesOf(id: String): List<String>

    @Query("SELECT * FROM knowledge WHERE id IN (:ids)")
    suspend fun knowledgeByIds(ids: List<String>): List<KnowledgeEntity>

    @Query("SELECT * FROM quotes WHERE figureId = :id ORDER BY id")
    suspend fun quotes(id: String): List<QuoteEntity>

    @Query("SELECT * FROM challenges WHERE figureId = :id ORDER BY position")
    suspend fun challenges(id: String): List<ChallengeEntity>

    @Insert suspend fun insertTimeline(items: List<TimelineEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertKnowledge(items: List<KnowledgeEntity>)
    @Insert suspend fun insertQuotes(items: List<QuoteEntity>)
    @Insert suspend fun insertChallenges(items: List<ChallengeEntity>)

    @Query("DELETE FROM timeline WHERE figureId = :id") suspend fun deleteTimeline(id: String)
    @Query("DELETE FROM knowledge WHERE figureId = :id") suspend fun deleteKnowledge(id: String)
    @Query("DELETE FROM quotes WHERE figureId = :id") suspend fun deleteQuotes(id: String)
    @Query("DELETE FROM challenges WHERE figureId = :id") suspend fun deleteChallenges(id: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE figureId = :figureId ORDER BY updatedAt DESC")
    fun observeForFigure(figureId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeConversation(id: Long): Flow<ConversationEntity?>

    @Insert suspend fun insertConversation(c: ConversationEntity): Long
    @Update suspend fun updateConversation(c: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id") suspend fun deleteConversation(id: Long)
    @Query("DELETE FROM messages WHERE conversationId = :id") suspend fun deleteMessagesOf(id: Long)
    @Query("DELETE FROM messages") suspend fun deleteAllMessages()
    @Query("DELETE FROM conversations") suspend fun deleteAllConversations()

    @Query("SELECT * FROM messages WHERE conversationId = :id ORDER BY id")
    fun observeMessages(id: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :id ORDER BY id")
    suspend fun messages(id: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessage(id: Long): MessageEntity?

    @Insert suspend fun insertMessage(m: MessageEntity): Long

    @Query("UPDATE messages SET text = :text, sourceIds = :sourceIds, engine = :engine WHERE id = :id")
    suspend fun updateMessage(id: Long, text: String, sourceIds: String, engine: String)

    @Query("""
        SELECT m.id AS messageId, m.conversationId AS conversationId, c.figureId AS figureId,
               c.title AS title, m.text AS text, m.createdAt AS createdAt
        FROM messages m JOIN conversations c ON c.id = m.conversationId
        WHERE m.text LIKE '%' || :q || '%' AND m.engine != 'notice'
        ORDER BY m.createdAt DESC LIMIT 100
    """)
    suspend fun search(q: String): List<SearchHit>
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings")
    fun observeAll(): Flow<List<SettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(s: SettingEntity)

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun remove(key: String)
}
