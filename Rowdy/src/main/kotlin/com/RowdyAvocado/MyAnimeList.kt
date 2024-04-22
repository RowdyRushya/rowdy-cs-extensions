package com.RowdyAvocado

// import android.util.Log

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.syncproviders.providers.MALApi
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class MyAnimeList(val plugin: RowdyPlugin) : MainAPI() {
    override var name = Companion.name
    override var mainUrl = Companion.mainUrl
    override var supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Anilist)
    override val hasMainPage = true
    override val hasQuickSearch = false
    private final val mediaLimit = 50
    private val api: SyncAPI = MALApi(1)

    val mapper = jacksonObjectMapper()

    companion object {
        val name = "MyAnimeList"
        var mainUrl = "https://myanimelist.net"
        val apiUrl = "https://api.myanimelist.net/v2"
    }

    override val mainPage =
            mainPageOf(
                    "$mainUrl/topanime.php?type=airing&limit=" to "Top Airing",
                    "$mainUrl/topanime.php?type=bypopularity&limit=" to "Most Popular",
                    "$mainUrl/topanime.php?type=favorite&limit=" to "Top Favorites",
                    "Personal" to "Personal"
            )

    private fun libraryItemToSearchRespose(item: SyncAPI.LibraryItem): SearchResponse {
        return newAnimeSearchResponse(item.name, item.url, item.type ?: TvType.Anime) {
            this.posterUrl = item.posterUrl
        }
    }

    private fun elementToSearchRespose(item: Element): SearchResponse {
        val name = item.select("div.detail a.hoverinfo_trigger").text()
        val url =
                item.select("div.detail a.hoverinfo_trigger").attr("href").substringBeforeLast("/")
        val posterUrl = item.select("img").attr("data-src")
        return newAnimeSearchResponse(name, url, TvType.Anime) { this.posterUrl = posterUrl }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
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
            val url = request.data + page.minus(1).times(mediaLimit)
            val res = app.get(url).document
            val data = res.select("tr.ranking-list").map { elementToSearchRespose(it) }

            return newHomePageResponse(request.name, data, true)
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfter("anime/").removeSuffix("/")
        val data: SyncAPI.SyncResult? = api.getResult(id)
        var episodes = emptyList<Episode>()
        var year = data?.startDate?.div(1000)?.div(86400)?.div(365)?.plus(1970)?.toInt()

        data?.let {
            val epCount = data.nextAiring?.episode?.minus(1) ?: data.totalEpisodes ?: 0
            for (i in 1..epCount) {
                episodes +=
                        newEpisode("") {
                            this.season = 1
                            this.episode = i
                            this.data =
                                    mapper.writeValueAsString(
                                            EpisodeData(data.title, year, this.season, i)
                                    )
                        }
            }
        }

        return newAnimeLoadResponse(data?.title ?: "", url, TvType.Anime) {
            if (Companion.name.equals("Anilist")) addAniListId(id.toInt()) else addMalId(id.toInt())
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
