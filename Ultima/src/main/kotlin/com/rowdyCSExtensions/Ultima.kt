package com.rowdyCSExtensions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.APIHolder.allProviders
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.utils.*
import com.rowdyCSExtensions.UltimaPlugin.SectionInfo

class Ultima(val plugin: UltimaPlugin) :
        MainAPI() { // all providers must be an intstance of MainAPI
    override var name = "Ultima"
    override var supportedTypes =
            setOf(TvType.Movie, TvType.TvSeries, TvType.AnimeMovie, TvType.Others)
    override var lang = "en"

    // enable this when your provider has a main page
    override val hasMainPage = true

    // this function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {
        return listOf<SearchResponse>()
    }

    val mapper = jacksonObjectMapper()

    // override val mainPage = readAllHomes()

    fun loadSections(): List<MainPageData> {
        var data: List<MainPageData> = emptyList()
        val savedSections = plugin.currentSections
        savedSections.forEach { plugin ->
            plugin.sections?.forEach { section ->
                if (section.enabled ?: false) {
                    data +=
                            mainPageOf(
                                    "${mapper.writeValueAsString(section)}" to
                                            "${section.pluginName}"
                            )
                }
            }
        }
        if (data.size.equals(0)) return mainPageOf("" to "") else return data
    }

    override val mainPage = loadSections()

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (!request.name.isNullOrEmpty()) {
            val realSection: SectionInfo = AppUtils.parseJson<SectionInfo>(request.data)
            val provider = allProviders.find { it.name == request.name }
            return provider?.getMainPage(
                    page,
                    MainPageRequest(
                            realSection.pluginName + ": " + realSection.name.toString(),
                            realSection.url.toString(),
                            request.horizontalImages
                    )
            )
        }
        throw ErrorLoadingException("Select sections from extension's settings page to show here.")
    }
}
