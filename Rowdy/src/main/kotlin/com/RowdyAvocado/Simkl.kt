package com.RowdyAvocado

// import android.util.Log

import com.lagradost.cloudstream3.*
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
    private val apiUrl = "https://api.simkl.com"

    private fun EpisodeMetadata.toEpisode(title: String, year: Int?): Episode {
        val epData = EpisodeData(title, year, this.season, this.episode).toStringData()
        return newEpisode(epData) {
            this.season = this.season
            this.episode = this.episode
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.removeSuffix("/").substringAfterLast("/").toInt()
        val data =
                api.searchByIds(mapOf(SyncServices.Simkl to id.toString()))?.getOrNull(0)
                        ?: throw ErrorLoadingException("Unable to load data")
        val title = data.title ?: throw ErrorLoadingException("Unable to find title")
        val year = data.year
        val posterUrl = data.toSyncSearchResult()?.posterUrl
        return if (data.type.equals("movie")) {
            val dataUrl = EpisodeData(title, year, null, null).toStringData()
            newMovieLoadResponse(title, url, TvType.Movie, dataUrl) {
                this.year = year
                addId(this, id.toInt())
                this.posterUrl = posterUrl
            }
        } else {
            val epData =
                    SimklApi.getEpisodes(id, data.type, data.total_episodes, false)
                            ?: throw Exception("Unable to fetch episodes")
            val episodes = epData.map { it.toEpisode(title, year) }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.year = year
                addId(this, id)
                this.posterUrl = posterUrl
            }
        }
    }
}
