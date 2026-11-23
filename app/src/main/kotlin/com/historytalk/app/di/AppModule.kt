package com.historytalk.app.di

import android.content.Context
import androidx.room.Room
import com.historytalk.app.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class AppScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "historytalk.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun figureDao(db: AppDatabase) = db.figureDao()
    @Provides fun contentDao(db: AppDatabase) = db.contentDao()
    @Provides fun chatDao(db: AppDatabase) = db.chatDao()
    @Provides fun settingDao(db: AppDatabase) = db.settingDao()

    @Provides @Singleton @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
