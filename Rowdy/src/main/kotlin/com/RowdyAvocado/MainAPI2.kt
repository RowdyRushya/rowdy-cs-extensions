package com.RowdyAvocado

// import android.util.Log

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.LoadResponse.Companion.addSimklId
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi
import com.lagradost.cloudstream3.utils.*

open class MainAPI2(open val plugin: RowdyPlugin) : MainAPI() {
    open override var lang = "en"
    open override val hasMainPage = true
    open val api: SyncAPI = AniListApi(1)
    open val type = Type.NONE

    open val mapper = jacksonObjectMapper()

    open override val mainPage = mainPageOf("Personal" to "Personal")

    private fun libraryItemToSearchRespose(item: SyncAPI.LibraryItem): SearchResponse {
        return newAnimeSearchResponse(item.name, item.url, item.type ?: TvType.Anime) {
            this.posterUrl = item.posterUrl
        }
    }

    open suspend fun buildSearchResposeList(
            page: Int,
            request: MainPageRequest
    ): Pair<List<SearchResponse>, Boolean> {
        return emptyList<SearchResponse>() to false
    }

    open fun addId(res: LoadResponse, id: Int) {
        when {
            this is Anilist -> res.addAniListId(id)
            this is MyAnimeList -> res.addMalId(id)
            this is Simkl -> res.addSimklId(id)
            else -> {}
        }
    }

    open override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (request.name.equals("Personal")) {
            var homePageList = emptyList<HomePageList>()
            api.getPersonalLibrary()?.allLibraryLists?.forEach {
                if (it.items.isNotEmpty()) {
                    val items = it.items.mapNotNull { libraryItemToSearchRespose(it) }
                    homePageList +=
                            HomePageList(
                                    "${request.name}: ${it.name.asString(plugin.activity!!)}",
                                    items
                            )
                }
            }
            return newHomePageResponse(homePageList, false)
        } else {
            val data = buildSearchResposeList(page, request)
            return newHomePageResponse(request.name, data.first, data.second)
        }
    }

    open override suspend fun load(url: String): LoadResponse {
        val id = url.removeSuffix("/").substringAfterLast("/")
        val data: SyncAPI.SyncResult? = api.getResult(id)
        var episodes = emptyList<Episode>()
        var year = data?.startDate?.div(1000)?.div(86400)?.div(365)?.plus(1970)?.toInt()

        data?.let {
            val epCount = data.nextAiring?.episode?.minus(1) ?: data.totalEpisodes ?: 0
            episodes =
                    (1..epCount).map { i ->
                        newEpisode(mapper.writeValueAsString(EpisodeData(data.title, year, 1, i))) {
                            this.season = 1
                            this.episode = i
                        }
                    }
        }

        return newAnimeLoadResponse(data?.title ?: "", url, TvType.Anime) {
            addId(this, id.toInt())
            addEpisodes(DubStatus.Subbed, episodes)
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
        providers.toList().amap {
            if (it.enabled) {
                RowdyExtractor(type)
                        .getUrl(data, mapper.writeValueAsString(it), subtitleCallback, callback)
            }
        }
        return true
    }
}

enum class Type {
    ANIME,
    MEDIA,
    NONE
}
