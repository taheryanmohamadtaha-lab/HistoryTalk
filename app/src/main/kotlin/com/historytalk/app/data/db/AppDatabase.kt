package com.historytalk.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FigureEntity::class, TimelineEntity::class, KnowledgeEntity::class, QuoteEntity::class,
        ChallengeEntity::class, FavoriteEntity::class, ConversationEntity::class,
        MessageEntity::class, SettingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun figureDao(): FigureDao
    abstract fun contentDao(): ContentDao
    abstract fun chatDao(): ChatDao
    abstract fun settingDao(): SettingDao
}

const val LIST_SEP = "\u001F"
fun List<String>.packList(): String = joinToString(LIST_SEP)
fun String.unpackList(): List<String> = if (isEmpty()) emptyList() else split(LIST_SEP)
