package com.RowdyAvocado

// import android.util.Log

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi.LikePageInfo
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi.Media
import com.lagradost.cloudstream3.utils.*

class Anilist(val plugin: RowdyPlugin) : MainAPI() {
    override var name = Anilist.name
    override var supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Anilist)
    override val hasMainPage = true
    override val hasQuickSearch = false
    private val api = AniListApi(1)

    val mapper = jacksonObjectMapper()

    companion object {
        val name = "Anilist"
        var mainUrl = "https://anilist.co"
        val apiUrl = "https://graphql.anilist.co"
    }

    override val mainPage =
            mainPageOf(
                    "Trending Now" to "Trending Now",
                    "Popular This Season" to "Popular This Season",
                    "All Time Popular" to "All Time Popular",
                    "Top 100 Anime" to "Top 100 Anime",
                    "Personal" to "Personal"
            )

    private fun queries(section: String, page: Int): String {
        return when (section) {
            "Trending Now" ->
                    "query (\$page: Int = $page, \$sort: [MediaSort] = [TRENDING_DESC, POPULARITY_DESC], \$isAdult: Boolean = false) { Page(page: \$page, perPage: 20) { pageInfo { total perPage currentPage lastPage hasNextPage } media(sort: \$sort, isAdult: \$isAdult, type: ANIME) { id idMal season seasonYear format episodes chapters title { english romaji } coverImage { extraLarge large medium } synonyms nextAiringEpisode { timeUntilAiring episode } } } }"
            "Popular This Season" ->
                    "query (\$page: Int = $page, \$seasonYear: Int = 2024, \$sort: [MediaSort] = [TRENDING_DESC, POPULARITY_DESC], \$isAdult: Boolean = false) { Page(page: \$page, perPage: 20) { pageInfo { total perPage currentPage lastPage hasNextPage } media(sort: \$sort, seasonYear: \$seasonYear, season: SPRING, isAdult: \$isAdult, type: ANIME) { id idMal season seasonYear format episodes chapters title { english romaji } coverImage { extraLarge large medium } synonyms nextAiringEpisode { timeUntilAiring episode } } } }"
            "All Time Popular" ->
                    "query (\$page: Int = $page, \$sort: [MediaSort] = [POPULARITY_DESC], \$isAdult: Boolean = false) { Page(page: \$page, perPage: 20) { pageInfo { total perPage currentPage lastPage hasNextPage } media(sort: \$sort, isAdult: \$isAdult, type: ANIME) { id idMal season seasonYear format episodes chapters title { english romaji } coverImage { extraLarge large medium } synonyms nextAiringEpisode { timeUntilAiring episode } } } }"
            "Top 100 Anime" ->
                    "query (\$page: Int = $page, \$sort: [MediaSort] = [SCORE_DESC], \$isAdult: Boolean = false) { Page(page: \$page, perPage: 20) { pageInfo { total perPage currentPage lastPage hasNextPage } media(sort: \$sort, isAdult: \$isAdult, type: ANIME) { id idMal season seasonYear format episodes chapters title { english romaji } coverImage { extraLarge large medium } synonyms nextAiringEpisode { timeUntilAiring episode } } } }"
            else -> ""
        }
    }

    private fun libraryItemToSearchRespose(item: SyncAPI.LibraryItem): SearchResponse {
        return newAnimeSearchResponse(item.name, item.url, item.type ?: TvType.Anime) {
            this.posterUrl = item.posterUrl
        }
    }

    private fun mediaToSearchRespose(item: Media): SearchResponse {

        return newAnimeSearchResponse(
                item.title.english ?: "",
                "https://anilist.co/anime/${item.id}/",
                TvType.Anime
        ) { this.posterUrl = item.coverImage.large }
    }

    private suspend fun anilistAPICall(query: String): AnilistData? {
        val url = Anilist.apiUrl
        val res =
                app.post(
                        url,
                        headers =
                                mapOf(
                                        "Accept" to "application/json",
                                        "Content-Type" to "application/json",
                                ),
                        data =
                                mapOf(
                                        "query" to query,
                                )
                )
        val parsed = res.parsedSafe<AnilistAPIResponse>()
        return parsed?.data
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (request.name.equals("Personal")) {
            var homePageList = emptyList<HomePageList>()
            api.getPersonalLibrary().allLibraryLists.forEach {
                if (it.items.isNotEmpty()) {
                    val items = it.items.mapNotNull { libraryItemToSearchRespose(it) }
                    homePageList +=
                            HomePageList(
                                    "${request.name}: ${it.name.asString(plugin.activity!!)}",
                                    items
                            )
                }
            }
            return newHomePageResponse(homePageList)
        } else {
            val res = anilistAPICall(queries(request.data, page))
            val data =
                    res?.page?.media?.map { mediaToSearchRespose(it) }
                            ?: throw Exception("Unable to convert api response to search response")
            return newHomePageResponse(request.name, data, res.page.pageInfo?.hasNextPage)
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val id = api.getIdFromUrl(url)
        val data = api.getResult(id)

        val seasonData = api.getAllSeasons(id.toInt())
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

    data class AnilistAPIResponse(
            @JsonProperty("data") val data: AnilistData? = null,
    )

    data class AnilistData(
            @JsonProperty("Page") val page: AnilistPage? = null,
    )

    data class AnilistPage(
            @JsonProperty("pageInfo") val pageInfo: LikePageInfo? = null,
            @JsonProperty("media") val media: List<Media>? = null,
    )
}
