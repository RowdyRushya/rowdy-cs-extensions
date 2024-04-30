package com.RowdyAvocado

// import android.util.Log
import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
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
                plugin.animeProviders.filter { it.enabled }.amap { provider ->
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
                plugin.mediaProviders.filter { it.enabled }.amap { provider ->
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
                        ?: throw Error("Could not find anime id from search page")
        val seasonDataUrl = "$url/ajax/episode/list/$id?vrf=${AniwaveUtils.vrfEncrypt(id)}"
        val seasonData =
                app.get(seasonDataUrl).parsedSafe<ApiResponseHTML>()?.html
                        ?: throw Error("Could not fetch data for $seasonDataUrl")
        val episodeIds =
                Jsoup.parse(seasonData)
                        .body()
                        .select(".episodes > ul > li > a")
                        .find { it.attr("data-num").equals(episode.toString()) }
                        ?.attr("data-ids")
                        ?: throw Error("Could not find episode IDs in response")
        val episodeDataUrl =
                "$url/ajax/server/list/$episodeIds?vrf=${AniwaveUtils.vrfEncrypt(episodeIds)}"
        val episodeData =
                app.get(episodeDataUrl).parsedSafe<ApiResponseHTML>()?.html
                        ?: throw Error("Could not fetch server data for $episodeDataUrl")

        Jsoup.parse(episodeData).body().select(".servers .type").amap {
            val dubType = it.attr("data-type")
            it.select("li").amap {
                val serverId = it.attr("data-sv-id")
                val dataId = it.attr("data-link-id")
                val serverResUrl = "$url/ajax/server/$dataId?vrf=${AniwaveUtils.vrfEncrypt(dataId)}"
                val serverRes = app.get(serverResUrl).parsedSafe<AniwaveResponseServer>()
                val encUrl =
                        serverRes?.result?.url
                                ?: throw Error("Could not fetch server url for $serverResUrl")
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
                        ?: throw Error("Could not find media id from search page")
        val seasonDataUrl = "$url/ajax/episode/list/$id?vrf=${CineZoneUtils.vrfEncrypt(id)}"
        val seasonData =
                app.get(seasonDataUrl).parsedSafe<ApiResponseHTML>()?.html
                        ?: throw Error("Could not fetch data for $seasonDataUrl")
        val episodeId =
                Jsoup.parse(seasonData)
                        .body()
                        .select(".episodes")
                        .find { it.attr("data-season").equals(data.season?.toString() ?: "1") }
                        ?.select("li a")
                        ?.find { it.attr("data-num").equals(data.episode?.toString() ?: "1") }
                        ?.attr("data-id")
                        ?: throw Error("Could not find episode IDs in response")
        val episodeDataUrl =
                "$url/ajax/server/list/$episodeId?vrf=${CineZoneUtils.vrfEncrypt(episodeId)}"
        val episodeData =
                app.get(episodeDataUrl).parsedSafe<ApiResponseHTML>()?.html
                        ?: throw Error("Could not fetch server data for $episodeDataUrl")

        Jsoup.parse(episodeData).body().select(".server").amap {
            val serverId = it.attr("data-id")
            val dataId = it.attr("data-link-id")
            val serverResUrl = "$url/ajax/server/$dataId?vrf=${CineZoneUtils.vrfEncrypt(dataId)}"
            val serverRes = app.get(serverResUrl).parsedSafe<AniwaveResponseServer>()
            val encUrl =
                    serverRes?.result?.url
                            ?: throw Error("Could not fetch server url for $serverResUrl")
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
}

private suspend fun commonLinkLoader(
        providerName: String?,
        serverName: ServerName,
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
