package com.RowdyAvocado

// import android.util.Log

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi.Companion.SyncServices
import com.lagradost.cloudstream3.utils.*

class Simkl(override val plugin: RowdyPlugin) : MainAPI2(plugin) {
    override var name = Companion.name
    override var mainUrl = Companion.mainUrl
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.AsianDrama)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Simkl)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val type = Type.MEDIA
    override val api: SimklApi = SimklApi(1)

    companion object {
        val name = "Simkl"
        var mainUrl = "https://simkl.com"
        val apiUrl = "https://api.simkl.com"
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.removeSuffix("/").substringAfterLast("/")
        val data = api.searchByIds(mapOf(SyncServices.Simkl to id))?.get(0)
        val title = data?.title ?: ""
        val year = data?.year
        val posterUrl = data?.toSyncSearchResult()?.posterUrl
        if (data?.type.equals("movie")) {
            return newMovieLoadResponse(
                    title,
                    url,
                    TvType.Movie,
                    mapper.writeValueAsString(EpisodeData(title, year, null, null))
            ) {
                this.year = year
                addId(this, id.toInt())
                this.posterUrl = posterUrl
            }
        } else {
            var episodes = emptyList<Episode>()
            data?.let {
                val eps = SimklApi.getEpisodes(id.toInt(), it.type, it.total_episodes, false)
                episodes =
                        eps?.mapNotNull { ep ->
                            newEpisode(
                                    mapper.writeValueAsString(
                                            EpisodeData(title, year, ep.season, ep.episode)
                                    )
                            ) {
                                this.season = ep.season
                                this.episode = ep.episode
                            }
                        }
                                ?: throw Exception("Unable to build episodes")
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.year = year
                addId(this, id.toInt())
                this.posterUrl = posterUrl
            }
        }
    }
}
