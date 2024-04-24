package com.RowdyAvocado

// import android.util.Log

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Encode
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.Mp4Upload
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import java.net.URI
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.jsoup.Jsoup

class RowdyExtractor(type: Type) : ExtractorApi() {
    override val mainUrl = Anilist.mainUrl
    override val name = Anilist.name
    override val requiresReferer = false
    private val type = type

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val data = AppUtils.parseJson<EpisodeData>(url)
        val provider = AppUtils.parseJson<Provider>(referer ?: "")
        when (type) {
            Type.ANIME -> {
                when (provider.name) {
                    "Aniwave" -> {
                        aniwaveExtractor(provider.domain, referer, data, subtitleCallback, callback)
                    }
                    else -> {}
                }
            }
            Type.MEDIA -> {
                when (provider.name) {
                    "CineZone" -> {
                        cinezoneExtractor(
                                provider.domain,
                                referer,
                                data,
                                subtitleCallback,
                                callback
                        )
                    }
                    else -> {}
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
            url: String?,
            referer: String?,
            data: EpisodeData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val searchPage =
                app.get(
                                "$url/filter?keyword=${data.name}&year[]=${data.seasonYear?:""}&sort=most_relevance"
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
                        .find { it.attr("data-num").equals(data.epNum) }
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
                        CineZoneUtils.serverName(serverId),
                        decUrl,
                        dubType,
                        referer,
                        subtitleCallback,
                        callback
                )
            }
        }
    }

    private suspend fun cinezoneExtractor(
            url: String?,
            referer: String?,
            data: EpisodeData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val searchPage =
                app.get(
                                "$url/filter?keyword=${data.name}&year[]=${data.seasonYear?:""}&sort=most_relevance"
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
                        .find { it.attr("data-season").equals(data.sNum ?: "1") }
                        ?.select("li a")
                        ?.find { it.attr("data-num").equals(data.epNum ?: "1") }
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
                    CineZoneUtils.serverName(serverId),
                    decUrl,
                    null,
                    referer,
                    subtitleCallback,
                    callback
            )
        }
    }
}

private suspend fun commonLinkLoader(
        serverName: ServerName,
        url: String,
        dubStatus: String?,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
) {
    val domain = "https://" + URI(url).host
    when (serverName) {
        ServerName.Vidplay ->
                AnyVidplay(dubStatus, domain).getUrl(url, referer, subtitleCallback, callback)
        ServerName.MyCloud -> loadExtractor(url, subtitleCallback, callback)
        ServerName.Filemoon ->
                AnyFileMoon(dubStatus, domain).getUrl(url, null, subtitleCallback, callback)
        ServerName.Mp4upload ->
                AnyMp4Upload(dubStatus, domain).getUrl(url, referer, subtitleCallback, callback)
        else -> {
            loadExtractor(url, subtitleCallback, callback)
        }
    }
}

class AnyFileMoon(dubType: String? = null, domain: String = "") : Filesim() {
    override val name = "Filemoon" + if (dubType != null) ": $dubType" else ""
    override val mainUrl = domain
    override val requiresReferer = false
}

class AnyVidplay(dubType: String? = null, domain: String = "") : ExtractorApi() {
    override val name = "Vidplay" + if (dubType != null) ": $dubType" else ""
    override val mainUrl = domain
    override val requiresReferer = false

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val id = url.substringBefore("?").substringAfterLast("/")
        val encodeId = encodeId(id, listOf("mpYdXXCWOdCmQxsx", "i0HTHEA9gTstnw1w"))
        val mediaUrl = callFutoken(encodeId, url)
        val res =
                app.get(
                                "$mediaUrl",
                                headers =
                                        mapOf(
                                                "Accept" to
                                                        "application/json, text/javascript, */*; q=0.01",
                                                "X-Requested-With" to "XMLHttpRequest",
                                        ),
                                referer = url
                        )
                        .parsedSafe<Response>()
                        ?.result

        res?.sources?.map {
            M3u8Helper.generateM3u8(this.name, it.file ?: return@map, "$mainUrl/").forEach(callback)
        }

        res?.tracks?.filter { it.kind == "captions" }?.map {
            subtitleCallback.invoke(SubtitleFile(it.label ?: return@map, it.file ?: return@map))
        }
    }

    private suspend fun callFutoken(id: String, url: String): String? {
        val script = app.get("$mainUrl/futoken", referer = url).text
        val k = "k='(\\S+)'".toRegex().find(script)?.groupValues?.get(1) ?: return null
        val a = mutableListOf(k)
        for (i in id.indices) {
            a.add((k[i % k.length].code + id[i].code).toString())
        }
        return "$mainUrl/mediainfo/${a.joinToString(",")}?${url.substringAfter("?")}"
    }

    private fun encodeId(id: String, keyList: List<String>): String {
        val cipher1 = Cipher.getInstance("RC4")
        val cipher2 = Cipher.getInstance("RC4")
        cipher1.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyList[0].toByteArray(), "RC4"),
                cipher1.parameters
        )
        cipher2.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyList[1].toByteArray(), "RC4"),
                cipher2.parameters
        )
        var input = id.toByteArray()
        input = cipher1.doFinal(input)
        input = cipher2.doFinal(input)
        return base64Encode(input).replace("/", "_")
    }

    data class Tracks(
            @JsonProperty("file") val file: String? = null,
            @JsonProperty("label") val label: String? = null,
            @JsonProperty("kind") val kind: String? = null,
    )

    data class Sources(
            @JsonProperty("file") val file: String? = null,
    )

    data class Result(
            @JsonProperty("sources") val sources: ArrayList<Sources>? = arrayListOf(),
            @JsonProperty("tracks") val tracks: ArrayList<Tracks>? = arrayListOf(),
    )

    data class Response(
            @JsonProperty("result") val result: Result? = null,
    )
}

class AnyMp4Upload(dubType: String? = null, domain: String = "") : Mp4Upload() {
    override var name = "Mp4Upload" + if (dubType != null) ": $dubType" else ""
    override var mainUrl = domain
    override val requiresReferer = false
}

class StreamWish : Filesim() {
    override val name = "StreamWish"
    override val mainUrl = "https://awish.pro"
    override val requiresReferer = false
}
