package com.RowdyAvocado

// import android.util.Log

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.metaproviders.TmdbLink
import com.lagradost.cloudstream3.metaproviders.TmdbProvider
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.utils.*

class Tmdb(val plugin: RowdyPlugin) : TmdbProvider() {
    override var name = "TMDB"
    override var mainUrl = "https://www.themoviedb.org"
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.AsianDrama)
    override var lang = "en"
    private val type = Type.MEDIA
    override val supportedSyncNames = setOf(SyncIdName.Simkl)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val useMetaLoadResponse = true
    private val apiUrl = "https://api.themoviedb.org"

    protected fun Any.toStringData(): String {
        return mapper.writeValueAsString(this)
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
        val mediaData = AppUtils.parseJson<TmdbLink>(data).toEpisodeData().toStringData()
        val providers =
                if (type.equals(Type.ANIME)) plugin.animeProviders else plugin.mediaProviders
        providers.toList().filter { it.enabled }.amap {
            RowdyExtractor(type).getUrl(mediaData, it.toStringData(), subtitleCallback, callback)
        }
        return true
    }
}
