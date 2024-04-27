package com.RowdyAvocado

// import android.util.Log

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.utils.*

abstract class MainAPI2(open val plugin: RowdyPlugin) : MainAPI() {
    open override var lang = "en"
    open override val hasMainPage = true
    abstract val api: SyncAPI
    abstract val type: Type
    abstract val syncId: String
    abstract val loginRequired: Boolean

    protected fun Any.toStringData(): String {
        return mapper.writeValueAsString(this)
    }

    open override val mainPage = mainPageOf("Personal" to "Personal")

    open suspend fun MainPageRequest.toSearchResponseList(
            page: Int
    ): Pair<List<SearchResponse>, Boolean> {
        return emptyList<SearchResponse>() to false
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        return api.search(query)
    }

    open override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (request.name.equals("Personal")) {
            var homePageList =
                    api.getPersonalLibrary()?.allLibraryLists?.mapNotNull {
                        if (it.items.isEmpty()) return@mapNotNull null
                        val libraryName =
                                it.name.asString(plugin.activity ?: return@mapNotNull null)
                        HomePageList("${request.name}: $libraryName", it.items)
                    }
                            ?: return null
            return newHomePageResponse(homePageList, false)
        } else {
            val data = request.toSearchResponseList(page)
            return newHomePageResponse(request.name, data.first, data.second)
        }
    }

    open override suspend fun load(url: String): LoadResponse {
        val id = url.removeSuffix("/").substringAfterLast("/")
        val data = api.getResult(id) ?: throw NotImplementedError("Unable to fetch show details")
        var year = data.startDate?.div(1000)?.div(86400)?.div(365)?.plus(1970)?.toInt()
        val epCount = data.nextAiring?.episode?.minus(1) ?: data.totalEpisodes ?: 0
        val episodes =
                (1..epCount).map { i ->
                    val dataUrl = EpisodeData(data.title, year, 1, i).toStringData()
                    Episode(dataUrl, season = 1, episode = i)
                }
        return newAnimeLoadResponse(data.title ?: "", url, TvType.Anime) {
            this.syncData = mutableMapOf(syncId to id)
            addEpisodes(DubStatus.Subbed, episodes)
            this.recommendations = data.recommendations
        }
    }

    open override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val providers =
                if (type.equals(Type.ANIME)) plugin.animeProviders else plugin.mediaProviders
        providers.toList().filter { it.enabled }.amap {
            RowdyExtractor(type).getUrl(data, it.toStringData(), subtitleCallback, callback)
        }
        return true
    }
}

enum class Type {
    ANIME,
    MEDIA,
    NONE
}
