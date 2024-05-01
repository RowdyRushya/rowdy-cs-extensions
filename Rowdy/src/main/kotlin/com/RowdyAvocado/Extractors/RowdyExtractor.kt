package com.RowdyAvocado

// import android.util.Log
import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink

class RowdyExtractor(val type: Type, val plugin: RowdyPlugin) : ExtractorApi() {
    override val name = "RowdyExtractor"
    override val mainUrl = "https://rowdy.pro"
    override val requiresReferer = false

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val data = AppUtils.parseJson<LinkData>(url)
        try {
            when (type) {
                Type.ANIME -> {
                    plugin.storage.animeProviders.filter { it.enabled }.amap { provider ->
                        when (provider.name) {
                            "Aniwave" -> {
                                RowdyContentExtractors.aniwaveExtractor(
                                        provider.name,
                                        provider.domain,
                                        data,
                                        subtitleCallback,
                                        callback
                                )
                            }
                            else -> {}
                        }
                    }
                }
                Type.MEDIA -> {
                    plugin.storage.mediaProviders.filter { it.enabled }.amap { provider ->
                        when (provider.name) {
                            "CineZone" -> {
                                RowdyContentExtractors.cinezoneExtractor(
                                        provider.name,
                                        provider.domain,
                                        data,
                                        subtitleCallback,
                                        callback
                                )
                            }
                            "VidsrcNet" -> {
                                RowdyContentExtractors.vidsrcNetExtractor(
                                        provider.name,
                                        provider.domain,
                                        data,
                                        subtitleCallback,
                                        callback
                                )
                            }
                            "VidsrcTo" -> {
                                RowdyContentExtractors.vidsrcToExtractor(
                                        provider.name,
                                        provider.domain,
                                        data,
                                        subtitleCallback,
                                        callback
                                )
                            }
                            "Moflix" -> {
                                RowdyContentExtractors.moflixExtractor(
                                        provider.name,
                                        provider.domain,
                                        data,
                                        subtitleCallback,
                                        callback
                                )
                            }
                            else -> {}
                        }
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            Log.d("ROWDY", e.message.toString())
        }
    }
}
