package com.rowdyCSExtensions

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType

class MyFlixer(val plugin: MyFlixerPlugin) :
        MainAPI() { // all providers must be an intstance of MainAPI
    override var mainUrl = "https://example.com/"
    override var name = "MyFlixer"
    override val supportedTypes = setOf(TvType.NSFW)

    override var lang = "en"

    // enable this when your provider has a main page
    override val hasMainPage = true

    // this function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {
        return listOf<SearchResponse>()
    }
}
