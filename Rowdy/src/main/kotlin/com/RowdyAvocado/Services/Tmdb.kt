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
    private val type = listOf(Type.MEDIA, Type.ANIME)
    override val supportedSyncNames = setOf(SyncIdName.Simkl)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val useMetaLoadResponse = true
    private val apiUrl = "https://api.themoviedb.org"

    protected fun Any.toStringData(): String {
        return mapper.writeValueAsString(this)
    }

    private fun TmdbLink.toLinkData(): LinkData {
        return LinkData(
                imdbId = imdbID,
                tmdbId = tmdbID,
                title = movieName,
                season = season,
                episode = episode
        )
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val mediaData = AppUtils.parseJson<TmdbLink>(data).toLinkData()
        type
                .filter {
                    (mediaData.isAnime && it == Type.ANIME) ||
                            (!mediaData.isAnime && it == Type.MEDIA)
                }
                .amap { t ->
                    RowdyExtractor(t, plugin)
                            .getUrl(mediaData.toStringData(), null, subtitleCallback, callback)
                }
        return true
    }
}
