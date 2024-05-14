package com.RowdyAvocado

// import android.util.Log
import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.utils.*

class MyAnimeList(override val plugin: RowdyPlugin) : Rowdy(plugin) {
    override var name = "MyAnimeList"
    override var mainUrl = "https://myanimelist.net"
    override var supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Anilist)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val api = AccountManager.malApi
    override val type = listOf(Type.ANIME)
    override val syncId = "MAL"
    override val loginRequired = true
    private final val mediaLimit = 20
    private val auth = BuildConfig.MAL_API
    private val apiUrl = "https://api.myanimelist.net/v2"

    override val mainPage =
            mainPageOf(
                    "$apiUrl/anime/ranking?ranking_type=all&limit=$mediaLimit&offset=" to
                            "Top Anime Series",
                    "$apiUrl/anime/ranking?ranking_type=airing&limit=$mediaLimit&offset=" to
                            "Top Airing Anime",
                    "$apiUrl/anime/ranking?ranking_type=bypopularity&limit=$mediaLimit&offset=" to
                            "Popular Anime",
                    "$apiUrl/anime/ranking?ranking_type=favorite&limit=$mediaLimit&offset=" to
                            "Top Favorited Anime",
                    "$apiUrl/anime/suggestions?limit=$mediaLimit&offset=" to "Suggestions",
                    "Personal" to "Personal"
            )

    override suspend fun MainPageRequest.toSearchResponseList(
            page: Int
    ): Pair<List<SearchResponse>, Boolean> {
        Log.d("rowdy", auth.toString())
        val res =
                app.get(
                                "${this.data}${(page - 1) * mediaLimit}",
                                headers = mapOf("Authorization" to "Bearer $auth")
                        )
                        .parsedSafe<MalApiResponse>()
                        ?: throw Exception("Unable to fetch content from API")
        val data =
                res.data?.map {
                    newAnimeSearchResponse(it.node.title, "$mainUrl/${it.node.id}") {
                        this.posterUrl = it.node.picture.large
                    }
                }
                        ?: throw Exception("Unable to fetch content from API")

        return data to true
    }

    data class MalApiResponse(
            @JsonProperty("data") val data: Array<MalApiData>? = null,
    ) {
        data class MalApiData(
                @JsonProperty("node") val node: MalApiNode,
        ) {
            data class MalApiNode(
                    @JsonProperty("id") val id: Int,
                    @JsonProperty("title") val title: String,
                    @JsonProperty("main_picture") val picture: MalApiNodePicture,
            ) {
                data class MalApiNodePicture(
                        @JsonProperty("medium") val medium: String,
                        @JsonProperty("large") val large: String,
                )
            }
        }
    }
}
