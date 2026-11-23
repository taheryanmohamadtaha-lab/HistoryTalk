package com.historytalk.app.data.repo

import com.historytalk.app.data.db.*
import com.historytalk.app.engine.FigureProfile
import com.historytalk.app.engine.RetrievalDoc
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FigureRepository @Inject constructor(
    private val figureDao: FigureDao,
    private val contentDao: ContentDao,
) {
    fun observeFigures(): Flow<List<FigureEntity>> = figureDao.observeAll()
    fun observeFavorites(): Flow<List<String>> = figureDao.observeFavorites()

    suspend fun setFavorite(id: String, fav: Boolean) =
        if (fav) figureDao.addFavorite(FavoriteEntity(id)) else figureDao.removeFavorite(id)

    suspend fun figure(id: String): FigureEntity? = figureDao.get(id)
    suspend fun timeline(id: String) = contentDao.timeline(id)
    suspend fun quotes(id: String) = contentDao.quotes(id)
    suspend fun challenges(id: String) = contentDao.challenges(id)
    suspend fun sources(id: String) = contentDao.sourcesOf(id)
    suspend fun knowledgeByIds(ids: List<String>) = if (ids.isEmpty()) emptyList() else contentDao.knowledgeByIds(ids)

    suspend fun docs(id: String): List<RetrievalDoc> = contentDao.knowledge(id).map {
        RetrievalDoc(it.id, it.category, it.keywords.unpackList(), it.text, it.source)
    }

    suspend fun profile(id: String): FigureProfile? = figureDao.get(id)?.toProfile()
}

fun FigureEntity.toProfile() = FigureProfile(
    id = id, name = name, lifespan = lifespan, summary = summary, sensitive = sensitive, voice = voice,
    openers = openers.unpackList(), closers = closers.unpackList(), vocabulary = vocabulary.unpackList(),
    suggestions = suggestions.unpackList(),
)
