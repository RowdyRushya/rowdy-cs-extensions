package com.RowdyAvocado

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.utils.*

class MyAnimeList(val plugin: RowdyPlugin) : MainAPI() {
    override var name = "MyAnimeList"
    override var supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)
    override var lang = "en"
    override val hasMainPage = true
    override val hasQuickSearch = false

    val mapper = jacksonObjectMapper()

    override val mainPage = mainPageOf("" to "")

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val providers = plugin.animeProviders.map { if (it.enabled) it.name else null }
        throw ErrorLoadingException(
                "Welcome to MyAnimeList. You have enabled following anime providers. ${providers.toString()}"
        )
    }

    override suspend fun load(url: String): LoadResponse {
        return newMovieLoadResponse("Welcome to Simkl", "", TvType.Others, "")
    }
}
