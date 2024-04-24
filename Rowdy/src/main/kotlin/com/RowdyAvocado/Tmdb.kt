package com.RowdyAvocado

// import android.util.Log

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.metaproviders.TmdbLink
import com.lagradost.cloudstream3.metaproviders.TmdbProvider
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.utils.*

class Tmdb(val plugin: RowdyPlugin) : TmdbProvider() {
    override var name = Companion.name
    override var mainUrl = Companion.mainUrl
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.AsianDrama)
    override var lang = "en"
    private val type = Type.MEDIA
    override val supportedSyncNames = setOf(SyncIdName.Simkl)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val useMetaLoadResponse = true

    companion object {
        val name = "TMDB"
        var mainUrl = "https://www.themoviedb.org"
        val apiUrl = "https://api.themoviedb.org"
    }

    private fun TmdbLink.toEpisodeData(): EpisodeData {
        return EpisodeData(this.movieName, null, this.season, this.episode)
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val mediaData = AppUtils.parseJson<TmdbLink>(data).toEpisodeData()
        val providers =
                if (type.equals(Type.ANIME)) plugin.animeProviders else plugin.mediaProviders
        providers.toList().amap {
            if (it.enabled) {
                RowdyExtractor(type)
                        .getUrl(
                                mapper.writeValueAsString(mediaData),
                                mapper.writeValueAsString(it),
                                subtitleCallback,
                                callback
                        )
            }
        }
        return true
    }
}
