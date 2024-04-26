package com.RowdyAvocado

// import android.util.Log

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addSimklId
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi.Companion.EpisodeMetadata
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi.Companion.SyncServices
import com.lagradost.cloudstream3.utils.*

class Simkl(override val plugin: RowdyPlugin) : MainAPI2(plugin) {
    override var name = "Simkl"
    override var mainUrl = "https://simkl.com"
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.AsianDrama)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Simkl)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val type = Type.MEDIA
    override val api: SimklApi = SimklApi(1)
    override val syncId = "simkl"
    private val apiUrl = "https://api.simkl.com"

    private fun EpisodeMetadata.toEpisode(title: String, year: Int?): Episode {
        val epData = EpisodeData(title, year, this.season, this.episode).toStringData()
        return newEpisode(epData) {
            this.season = this.season
            this.episode = this.episode
        }
    }

    override val mainPage = mainPageOf("""{ "wltime": "month", "sort": ["watched", true], "langs": "EN", "boxes": { "subtype": { "t_documentary": false, "t_youtube": false, "t_entertainment": false } }, "sliders": {"budget":[1,500]}, "limit": 50, "title_lang": 0, "type": "movies", "section": "best", "page": 2 }""" to "Most Watched Movies" ,"" to "Most Watched TV" ,"Personal" to "Personal")

    override suspend fun load(url: String): LoadResponse {
        val id = url.removeSuffix("/").substringAfterLast("/")
        val data =
                api.searchByIds(mapOf(SyncServices.Simkl to id))?.getOrNull(0)
                        ?: throw ErrorLoadingException("Unable to load data")
        val title = data.title ?: throw ErrorLoadingException("Unable to find title")
        val year = data.year
        val posterUrl = data.toSyncSearchResult()?.posterUrl
        return if (data.type.equals("movie")) {
            val dataUrl = EpisodeData(title, year, null, null).toStringData()
            newMovieLoadResponse(title, url, TvType.Movie, dataUrl) {
                this.syncData = mutableMapOf(id to syncId)
                this.year = year
                this.posterUrl = posterUrl
            }
        } else {
            val epData =
                    SimklApi.getEpisodes(id.toInt(), data.type, data.total_episodes, false)
                            ?: throw Exception("Unable to fetch episodes")
            val episodes = epData.map { it.toEpisode(title, year) }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                // this.syncData = mutableMapOf(id to syncId)
                this.year = year
                addSimklId(id.toInt())
                this.posterUrl = posterUrl
            }
        }
    }
}
