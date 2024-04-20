package com.RowdyAvocado

// import android.util.Log

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi
import com.lagradost.cloudstream3.utils.*

class Anilist(val plugin: RowdyPlugin) : MainAPI() {
    override var name = Anilist.name
    override var supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Anilist)
    override val hasMainPage = true
    override val hasQuickSearch = false

    val mapper = jacksonObjectMapper()

    companion object {
        val name = "Anilist"
        var mainUrl = "https://anilist.co"
    }

    override val mainPage =
            mainPageOf(
                    "Watching" to "Watching",
                    "Completed" to "Completed",
                    "On-Hold" to "On-Hold",
                    "Dropped" to "Dropped",
                    "Plan to Watch" to "Plan to Watch",
                    "Rewatching" to "Rewatching"
            )

    private fun convertToSearchRespose(item: SyncAPI.LibraryItem): SearchResponse {
        return newAnimeSearchResponse(item.name, item.url, item.type ?: TvType.Anime) {
            this.posterUrl = item.posterUrl
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        var data = emptyList<SearchResponse>()
        AniListApi(1)
                .getPersonalLibrary()
                .allLibraryLists
                .find { it.name.asString(plugin.activity!!).equals(request.data) }
                ?.let { data = it.items.mapNotNull { convertToSearchRespose(it) } }
        return newHomePageResponse(request.name, data, false)
    }

    override suspend fun load(url: String): LoadResponse {
        val id = AniListApi(1).getIdFromUrl(url)
        val data = AniListApi(1).getResult(id)
        val seasonData = AniListApi(1).getAllSeasons(id.toInt())
        var episodes = emptyList<Episode>()
        var sCounter = 0
        seasonData.forEach { season ->
            season?.let {
                sCounter += 1
                val nextAiringEpisode = season.data.Media.nextAiringEpisode
                val epCount =
                        if (nextAiringEpisode == null) season.data.Media.episodes ?: 0
                        else (nextAiringEpisode.episode ?: 1) - 1
                for (i in 1..epCount) {
                    val name = season.data.Media.title?.english ?: season.data.Media.title?.romaji
                    episodes +=
                            newEpisode("") {
                                this.season = sCounter
                                this.episode = i
                                this.data =
                                        mapper.writeValueAsString(
                                                EpisodeData(name, null, sCounter, i)
                                        )
                            }
                }
            }
        }
        return newAnimeLoadResponse(data.title ?: "", url, TvType.Anime) {
            addAniListId(id.toInt())
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        plugin.animeProviders.toList().amap {
            if (it.enabled) {
                AnimeExtractors()
                        .getUrl(data, mapper.writeValueAsString(it), subtitleCallback, callback)
            }
        }
        return true
    }
}
