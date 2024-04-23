package com.RowdyAvocado

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi
import com.lagradost.cloudstream3.utils.*

class Simkl(override val plugin: RowdyPlugin) : MainAPI2(plugin) {
    override var name = Companion.name
    override var mainUrl = Companion.mainUrl
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.AsianDrama)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Simkl)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val type = Type.ANIME
    override val api: SyncAPI = SimklApi(1)

    companion object {
        val name = "Simkl"
        var mainUrl = "https://simkl.com"
        val apiUrl = "https://api.simkl.com"
    }
}
