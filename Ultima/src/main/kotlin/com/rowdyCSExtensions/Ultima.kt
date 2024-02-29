package com.KillerDogeEmpire

import com.KillerDogeEmpire.UltimaPlugin.SectionInfo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.APIHolder.allProviders
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.utils.*
import kotlin.collections.forEach

class Ultima(val plugin: UltimaPlugin) : MainAPI() {
    override var name = "Ultima"
    override var supportedTypes = TvType.values().toSet()
    override var lang = "en"
    override val hasMainPage = true

    override suspend fun search(query: String): List<SearchResponse> {
        return listOf<SearchResponse>()
    }

    val mapper = jacksonObjectMapper()

    // override val mainPage = readAllHomes()

    fun loadSections(): List<MainPageData> {
        var data: List<MainPageData> = emptyList()
        var savedSections: List<SectionInfo> = emptyList()
        val savedPlugins = plugin.currentSections
        savedPlugins.forEach { plugin ->
            plugin.sections?.forEach { section -> savedSections += section }
        }
        savedSections.sortedByDescending { it.priority }.forEach { section ->
            if (section.enabled) {
                data +=
                        mainPageOf(
                                "${mapper.writeValueAsString(section)}" to "${section.pluginName}"
                        )
            }
        }
        if (data.size.equals(0)) return mainPageOf("" to "") else return data
    }

    override val mainPage = loadSections()

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (!request.name.isNullOrEmpty()) {
            try {
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
            } catch (e: Throwable) {
                return null
            }
        } else
                throw ErrorLoadingException(
                        "Select sections from extension's settings page to show here."
                )
    }

    override suspend fun load(url: String): LoadResponse {
        val enabledPlugins = mainPage.map { it.name }
        val provider = allProviders.filter { it.name in enabledPlugins }
        for (i in 0 until (provider.size)) {
            try {
                return provider.get(i).load(url)!!
            } catch (e: Throwable) {}
        }
        return newMovieLoadResponse("Welcome to Ultima", "", TvType.Others, "")
    }
}
