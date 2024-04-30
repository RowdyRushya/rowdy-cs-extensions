package com.RowdyAvocado

// import android.util.Log
import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.Mp4Upload
import com.lagradost.cloudstream3.extractors.Vidplay
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import java.net.URI
import org.jsoup.Jsoup

class RowdyExtractor(val type: Type, val plugin: RowdyPlugin) : ExtractorApi() {
    override val mainUrl = "https://rowdy.to"
    override val name = "Rowdy Extractor"
    override val requiresReferer = false

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val data = AppUtils.parseJson<LinkData>(url)
        Log.d("rowdy", data.toString())
        when (type) {
            Type.ANIME -> {
                plugin.storage.animeProviders.filter { it.enabled }.amap { provider ->
                    when (provider.name) {
                        "Aniwave" -> {
                            aniwaveExtractor(
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
                            cinezoneExtractor(
                                    provider.name,
                                    provider.domain,
                                    data,
                                    subtitleCallback,
                                    callback
                            )
                        }
                        "VidsrcNet" -> {
                            vidsrcNetExtractor(
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
    }

    private fun buildExtractorLink(
            serverName: String,
            link: String,
            referer: String = "",
            quality: Int = Qualities.Unknown.value
    ): ExtractorLink {
        return ExtractorLink(serverName, serverName, link, referer, quality, link.contains(".m3u8"))
    }

    // #region - Aniwave (https://aniwave.to) Extractor

    private suspend fun aniwaveExtractor(
            providerName: String?,
            url: String?,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val episode = data.episode ?: if (data.isAnime && data.type.equals("Movie")) 1 else return
        val searchPage =
                app.get(
                                "$url/filter?keyword=${data.title}&year[]=${data.year?:""}&sort=most_relevance"
                        )
                        .document
        val id =
                searchPage.selectFirst("div.poster")?.attr("data-tip")?.split("?/")?.get(0)
                        ?: throw Exception("Could not find anime id from search page")
        val seasonDataUrl = "$url/ajax/episode/list/$id?vrf=${AniwaveUtils.vrfEncrypt(id)}"
        val seasonData =
                app.get(seasonDataUrl).parsedSafe<ApiResponseHTML>()?.html
                        ?: throw Exception("Could not fetch data for $seasonDataUrl")
        val episodeIds =
                Jsoup.parse(seasonData)
                        .body()
                        .select(".episodes > ul > li > a")
                        .find { it.attr("data-num").equals(episode.toString()) }
                        ?.attr("data-ids")
                        ?: throw Exception("Could not find episode IDs in response")
        val episodeDataUrl =
                "$url/ajax/server/list/$episodeIds?vrf=${AniwaveUtils.vrfEncrypt(episodeIds)}"
        val episodeData =
                app.get(episodeDataUrl).parsedSafe<ApiResponseHTML>()?.html
                        ?: throw Exception("Could not fetch server data for $episodeDataUrl")

        Jsoup.parse(episodeData).body().select(".servers .type").amap {
            val dubType = it.attr("data-type")
            it.select("li").amap {
                val serverId = it.attr("data-sv-id")
                val dataId = it.attr("data-link-id")
                val serverResUrl = "$url/ajax/server/$dataId?vrf=${AniwaveUtils.vrfEncrypt(dataId)}"
                val serverRes = app.get(serverResUrl).parsedSafe<AniwaveResponseServer>()
                val encUrl =
                        serverRes?.result?.url
                                ?: throw Exception("Could not fetch server url for $serverResUrl")
                val decUrl = AniwaveUtils.vrfDecrypt(encUrl)
                commonLinkLoader(
                        providerName,
                        AniwaveUtils.serverName(serverId),
                        decUrl,
                        dubType,
                        subtitleCallback,
                        callback
                )
            }
        }
    }

    // #endregion - Aniwave (https://aniwave.to) Extractor

    // #region - CineZone (https://cinezone.to) Extractor

    private suspend fun cinezoneExtractor(
            providerName: String?,
            url: String?,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val searchPage =
                app.get(
                                "$url/filter?keyword=${data.title}&year[]=${data.year?:""}&sort=most_relevance"
                        )
                        .document
        val id =
                searchPage.selectFirst("div.tooltipBtn")?.attr("data-tip")?.split("?/")?.get(0)
                        ?: throw Exception("Could not find media id from search page")
        val seasonDataUrl = "$url/ajax/episode/list/$id?vrf=${CineZoneUtils.vrfEncrypt(id)}"
        val seasonData =
                app.get(seasonDataUrl).parsedSafe<ApiResponseHTML>()?.html
                        ?: throw Exception("Could not fetch data for $seasonDataUrl")
        val episodeId =
                Jsoup.parse(seasonData)
                        .body()
                        .select(".episodes")
                        .find { it.attr("data-season").equals(data.season?.toString() ?: "1") }
                        ?.select("li a")
                        ?.find { it.attr("data-num").equals(data.episode?.toString() ?: "1") }
                        ?.attr("data-id")
                        ?: throw Exception("Could not find episode IDs in response")
        val episodeDataUrl =
                "$url/ajax/server/list/$episodeId?vrf=${CineZoneUtils.vrfEncrypt(episodeId)}"
        val episodeData =
                app.get(episodeDataUrl).parsedSafe<ApiResponseHTML>()?.html
                        ?: throw Exception("Could not fetch server data for $episodeDataUrl")

        Jsoup.parse(episodeData).body().select(".server").amap {
            val serverId = it.attr("data-id")
            val dataId = it.attr("data-link-id")
            val serverResUrl = "$url/ajax/server/$dataId?vrf=${CineZoneUtils.vrfEncrypt(dataId)}"
            val serverRes = app.get(serverResUrl).parsedSafe<AniwaveResponseServer>()
            val encUrl =
                    serverRes?.result?.url
                            ?: throw Exception("Could not fetch server url for $serverResUrl")
            val decUrl = CineZoneUtils.vrfDecrypt(encUrl)
            commonLinkLoader(
                    providerName,
                    CineZoneUtils.serverName(serverId),
                    decUrl,
                    null,
                    subtitleCallback,
                    callback
            )
        }
    }

    // #endregion - CineZone (https://cinezone.to) Extractor

    // #region - VidSrc (https://vidsrc.net) Extractor

    private suspend fun vidsrcNetExtractor(
            providerName: String?,
            url: String?,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        if (data.tmdbId.isNullOrEmpty()) return
        val iFrameUrl =
                if (data.season == null) {
                    "$url/embed/movie?tmdb=${data.tmdbId}"
                } else {
                    "$url/embed/tv?tmdb=${data.tmdbId}&season=${data.season}&episode=${data.episode}"
                }
        val iframedoc = app.get(iFrameUrl).document
        val serverhash =
                iframedoc.selectFirst("div.serversList > div.server")?.attr("data-hash").toString()
        val link = Extractvidsrcnetservers(serverhash)
        val URI =
                app.get(link, referer = "https://vidsrc.net/")
                        .document
                        .selectFirst("script:containsData(Playerjs)")
                        ?.data()
                        ?.substringAfter("file:\"#9")
                        ?.substringBefore("\"")
                        ?.replace(Regex("/@#@\\S+?=?="), "")
                        ?.let { base64Decode(it) }
                        .toString()
        loadExtractor(URI, referer = "https://vidsrc.net/", subtitleCallback, callback)
    }

    suspend fun Extractvidsrcnetservers(url: String): String {
        val rcp =
                app.get(
                                "https://vidsrc.stream/rcp/$url",
                                referer = "https://vidsrc.net/",
                                headers =
                                        mapOf(
                                                "User-Agent" to
                                                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0"
                                        )
                        )
                        .document
        val link =
                rcp.selectFirst("script:containsData(player_iframe)")
                        ?.data()
                        ?.substringAfter("src: '")
                        ?.substringBefore("',")
        return "http:$link"
    }

    // #endregion - VidSrc (https://vidsrc.net/) Extractor
}

private suspend fun commonLinkLoader(
        providerName: String?,
        serverName: ServerName?,
        url: String,
        dubStatus: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
) {
    val domain = "https://" + URI(url).host
    when (serverName) {
        ServerName.Vidplay ->
                AnyVidplay(providerName, dubStatus, domain)
                        .getUrl(url, domain, subtitleCallback, callback)
        ServerName.MyCloud ->
                AnyMyCloud(providerName, dubStatus, domain)
                        .getUrl(url, domain, subtitleCallback, callback)
        ServerName.Filemoon ->
                AnyFileMoon(providerName, dubStatus, domain)
                        .getUrl(url, null, subtitleCallback, callback)
        ServerName.Mp4upload ->
                AnyMp4Upload(providerName, dubStatus, domain)
                        .getUrl(url, domain, subtitleCallback, callback)
        else -> {
            loadExtractor(url, subtitleCallback, callback)
        }
    }
}

// #region - Custom Extractors

class AnyFileMoon(provider: String?, dubType: String?, domain: String = "") : Filesim() {
    override val name =
            (if (provider != null) "$provider: " else "") +
                    "Filemoon" +
                    (if (dubType != null) ": $dubType" else "")
    override val mainUrl = domain
    override val requiresReferer = false
}

class AnyMyCloud(provider: String?, dubType: String?, domain: String = "") : Vidplay() {
    override val name =
            (if (provider != null) "$provider: " else "") +
                    "MyCloud" +
                    (if (dubType != null) ": $dubType" else "")
    override val mainUrl = domain
    override val requiresReferer = false
}

class AnyVidplay(provider: String?, dubType: String?, domain: String = "") : Vidplay() {
    override val name =
            (if (provider != null) "$provider: " else "") +
                    "Vidplay" +
                    (if (dubType != null) ": $dubType" else "")
    override val mainUrl = domain
    override val requiresReferer = false
}

class AnyMp4Upload(provider: String?, dubType: String?, domain: String = "") : Mp4Upload() {
    override var name =
            (if (provider != null) "$provider: " else "") +
                    "Mp4Upload" +
                    (if (dubType != null) ": $dubType" else "")
    override var mainUrl = domain
    override val requiresReferer = false
}

class StreamWish : Filesim() {
    override val name = "StreamWish"
    override val mainUrl = "https://awish.pro"
    override val requiresReferer = false
}

// #endregion - Custom Extractors
