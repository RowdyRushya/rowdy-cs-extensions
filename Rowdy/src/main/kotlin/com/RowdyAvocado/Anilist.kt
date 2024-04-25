package com.RowdyAvocado

// import android.util.Log

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi.LikePageInfo
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi.Media
import com.lagradost.cloudstream3.utils.*

class Anilist(override val plugin: RowdyPlugin) : MainAPI2(plugin) {
    override var name = "Anilist"
    override var mainUrl = "https://anilist.co"
    override var supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Anilist)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val type = Type.ANIME
    override val api: SyncAPI = AniListApi(1)
    private val apiUrl = "https://graphql.anilist.co"
    private val headerJSON =
            mapOf("Accept" to "application/json", "Content-Type" to "application/json")

    override val mainPage =
            mainPageOf(
                    "query (\$page: Int = ###, \$sort: [MediaSort] = [TRENDING_DESC, POPULARITY_DESC], \$isAdult: Boolean = false) { Page(page: \$page, perPage: 20) { pageInfo { total perPage currentPage lastPage hasNextPage } media(sort: \$sort, isAdult: \$isAdult, type: ANIME) { id idMal season seasonYear format episodes chapters title { english romaji } coverImage { extraLarge large medium } synonyms nextAiringEpisode { timeUntilAiring episode } } } }" to
                            "Trending Now",
                    "query (\$page: Int = ###, \$seasonYear: Int = 2024, \$sort: [MediaSort] = [TRENDING_DESC, POPULARITY_DESC], \$isAdult: Boolean = false) { Page(page: \$page, perPage: 20) { pageInfo { total perPage currentPage lastPage hasNextPage } media(sort: \$sort, seasonYear: \$seasonYear, season: SPRING, isAdult: \$isAdult, type: ANIME) { id idMal season seasonYear format episodes chapters title { english romaji } coverImage { extraLarge large medium } synonyms nextAiringEpisode { timeUntilAiring episode } } } }" to
                            "Popular This Season",
                    "query (\$page: Int = ###, \$sort: [MediaSort] = [POPULARITY_DESC], \$isAdult: Boolean = false) { Page(page: \$page, perPage: 20) { pageInfo { total perPage currentPage lastPage hasNextPage } media(sort: \$sort, isAdult: \$isAdult, type: ANIME) { id idMal season seasonYear format episodes chapters title { english romaji } coverImage { extraLarge large medium } synonyms nextAiringEpisode { timeUntilAiring episode } } } }" to
                            "All Time Popular",
                    "query (\$page: Int = ###, \$sort: [MediaSort] = [SCORE_DESC], \$isAdult: Boolean = false) { Page(page: \$page, perPage: 20) { pageInfo { total perPage currentPage lastPage hasNextPage } media(sort: \$sort, isAdult: \$isAdult, type: ANIME) { id idMal season seasonYear format episodes chapters title { english romaji } coverImage { extraLarge large medium } synonyms nextAiringEpisode { timeUntilAiring episode } } } }" to
                            "Top 100 Anime",
                    "Personal" to "Personal"
            )

    private suspend fun anilistAPICall(query: String): AnilistData {
        val data = mapOf("query" to query)
        val res =
                app.post(apiUrl, headers = headerJSON, data = data).parsedSafe<AnilistAPIResponse>()
                        ?: throw Exception("Unable to fecth or parse Anilist api response")
        return res.data
    }

    override suspend fun buildSearchResposeList(
            page: Int,
            request: MainPageRequest
    ): Pair<List<SearchResponse>, Boolean> {
        val res = anilistAPICall(request.data.replace("###", "$page"))
        val data =
                res.page.media.map {
                    val title = it.title.english ?: it.title.romaji ?: ""
                    val url = "$mainUrl/anime/${it.id}"
                    newAnimeSearchResponse(title, url, TvType.Anime) {
                        this.posterUrl = it.coverImage.large
                    }
                }
        val hasNextPage = res.page.pageInfo.hasNextPage ?: false
        return data to hasNextPage
    }

    data class AnilistAPIResponse(
            @JsonProperty("data") val data: AnilistData,
    )

    data class AnilistData(
            @JsonProperty("Page") val page: AnilistPage,
    )

    data class AnilistPage(
            @JsonProperty("pageInfo") val pageInfo: LikePageInfo,
            @JsonProperty("media") val media: List<Media>,
    )
}
