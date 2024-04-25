package com.RowdyAvocado

// import android.util.Log

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.metaproviders.TraktProvider
import com.lagradost.cloudstream3.metaproviders.TraktProvider.LinkData
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.utils.*

class Trakt(val plugin: RowdyPlugin) : TraktProvider() {
    override var name = Companion.name
    override var mainUrl = Companion.mainUrl
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.AsianDrama)
    override var lang = "en"
    private val type = Type.MEDIA
    override val supportedSyncNames = setOf(SyncIdName.Simkl)
    override val hasMainPage = true
    override val hasQuickSearch = false

    protected fun Any.toStringData(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        val name = "Trakt"
        var mainUrl = "https://trakt.tv"
        val apiUrl = "https://api.trakt.tv"
    }

    private fun LinkData.toEpisodeData(): EpisodeData {
        return EpisodeData(this.title, this.year, this.season, this.episode)
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val mediaData = AppUtils.parseJson<LinkData>(data).toEpisodeData().toStringData()
        val providers =
                if (type.equals(Type.ANIME)) plugin.animeProviders else plugin.mediaProviders
        providers.toList().filter { it.enabled }.amap {
            RowdyExtractor(type).getUrl(mediaData, it.toStringData(), subtitleCallback, callback)
        }
        return true
    }
}
