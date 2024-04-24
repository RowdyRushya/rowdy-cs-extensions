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
    override var name = Companion.name
    override var mainUrl = Companion.mainUrl
    override var supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Anilist)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val type = Type.ANIME
    override val api: SyncAPI = AniListApi(1)

    // val LoadResponse.addId2: (id: Int?): Unit -> Unit = ::addAniListId

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

    private suspend fun anilistAPICall(query: String): AnilistData? {
        val url = Companion.apiUrl
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

    override suspend fun buildSearchResposeList(
            page: Int,
            request: MainPageRequest
    ): Pair<List<SearchResponse>, Boolean> {
        val res = anilistAPICall(queries(request.data, page))
        val data =
                res?.page?.media?.map {
                    newAnimeSearchResponse(
                            it.title.english ?: "",
                            "https://anilist.co/anime/${it.id}",
                            TvType.Anime
                    ) { this.posterUrl = it.coverImage.large }
                }
                        ?: throw Exception("Unable to convert api response to search response")
        return data to false
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
