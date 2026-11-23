package com.historytalk.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "figures")
data class FigureEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameEn: String,
    val lifespan: String,
    val era: String,
    val eraGroup: String,
    val civilization: String,
    val role: String,
    val roleGroup: String,
    val nationality: String,
    val sensitive: Boolean,
    val colorHex: String,
    val summary: String,
    val voice: String,
    val openers: String,       // با جداکننده \u001F
    val closers: String,
    val vocabulary: String,
    val places: String,
    val relations: String,
    val suggestions: String,
)

@Entity(tableName = "timeline")
data class TimelineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val figureId: String,
    val position: Int,
    val year: String,
    val text: String,
)

@Entity(tableName = "knowledge")
data class KnowledgeEntity(
    @PrimaryKey val id: String,   // figureId#n
    val figureId: String,
    val category: String,
    val keywords: String,         // با جداکننده \u001F
    val text: String,
    val source: String,
)

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val figureId: String,
    val text: String,
    val source: String,
    val type: String,
)

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val figureId: String,
    val position: Int,
    val question: String,
    val keywords: String,
    val explanation: String,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val figureId: String)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val figureId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val mode: String = MODE_FREE,   // free | interview | challenge
    val qIndex: Int = 0,            // اندیس سؤال بعدی (مصاحبه) یا سؤال جاری (چالش)
    val score: Int = 0,
) {
    companion object {
        const val MODE_FREE = "free"
        const val MODE_INTERVIEW = "interview"
        const val MODE_CHALLENGE = "challenge"
    }
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String,               // user | assistant
    val text: String,
    val createdAt: Long,
    val sourceIds: String = "",     // شناسه‌های دانش مبنا، با ویرگول
    val engine: String = "",        // base | model | safety | notice
    val refusal: Boolean = false,
) {
    companion object {
        const val USER = "user"
        const val ASSISTANT = "assistant"
        const val ENGINE_BASE = "base"
        const val ENGINE_MODEL = "model"
        const val ENGINE_SAFETY = "safety"
        const val ENGINE_NOTICE = "notice"
    }
}

@Entity(tableName = "settings")
data class SettingEntity(@PrimaryKey val key: String, val value: String)
