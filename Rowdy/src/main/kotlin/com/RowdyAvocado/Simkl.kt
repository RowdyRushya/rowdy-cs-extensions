package com.RowdyAvocado

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.utils.*

class Simkl(val plugin: RowdyPlugin) : MainAPI() {
    override var name = "Simkl"
    override var supportedTypes = TvType.values().toSet()
    override var lang = "en"
    override val hasMainPage = true
    override val hasQuickSearch = false

    val mapper = jacksonObjectMapper()

    override val mainPage = mainPageOf("" to "")

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val providers = plugin.mediaProviders.map { if (it.enabled) it.name else null }
        throw ErrorLoadingException(
                "Welcome to Simkl. You have enabled following providers. ${providers.toString()}"
        )
    }

    override suspend fun load(url: String): LoadResponse {
        return newMovieLoadResponse("Welcome to Simkl", "", TvType.Others, "")
    }
}
