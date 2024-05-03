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
    private val type = listOf(Type.MEDIA, Type.ANIME)
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

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val mediaData = AppUtils.parseJson<LinkData>(data)
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
