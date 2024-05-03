package com.RowdyAvocado

// import android.util.Log
import android.util.Base64
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.base64Encode
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.Mp4Upload
import com.lagradost.cloudstream3.extractors.Vidplay
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getAndUnpack
import com.lagradost.cloudstream3.utils.loadExtractor
import java.net.URI
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.jsoup.Jsoup

object RowdyContentExtractors {

    suspend fun commonLinkLoader(
            providerName: String?,
            serverName: ServerName?,
            url: String,
            dubStatus: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val domain = CommonUtils.getBaseUrl(url)
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

    // #region - Aniwave (https://aniwave.to) Extractor

    suspend fun aniwaveExtractor(
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
                        ?: return
        val seasonDataUrl = "$url/ajax/episode/list/$id?vrf=${AniwaveUtils.vrfEncrypt(id)}"
        val seasonData = app.get(seasonDataUrl).parsedSafe<ApiResponseHTML>()?.html ?: return
        val episodeIds =
                Jsoup.parse(seasonData)
                        .body()
                        .select(".episodes > ul > li > a")
                        .find { it.attr("data-num").equals(episode.toString()) }
                        ?.attr("data-ids")
                        ?: return
        val episodeDataUrl =
                "$url/ajax/server/list/$episodeIds?vrf=${AniwaveUtils.vrfEncrypt(episodeIds)}"
        val episodeData = app.get(episodeDataUrl).parsedSafe<ApiResponseHTML>()?.html ?: return

        Jsoup.parse(episodeData).body().select(".servers .type").amap {
            val dubType = it.attr("data-type")
            it.select("li").amap LinkLoader@{
                val serverId = it.attr("data-sv-id")
                val dataId = it.attr("data-link-id")
                val serverResUrl = "$url/ajax/server/$dataId?vrf=${AniwaveUtils.vrfEncrypt(dataId)}"
                val serverRes = app.get(serverResUrl).parsedSafe<AniwaveResponseServer>()
                val encUrl = serverRes?.result?.url ?: return@LinkLoader
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

    suspend fun cinezoneExtractor(
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
                        ?: return
        val seasonDataUrl = "$url/ajax/episode/list/$id?vrf=${CineZoneUtils.vrfEncrypt(id)}"
        val seasonData = app.get(seasonDataUrl).parsedSafe<ApiResponseHTML>()?.html ?: return
        val episodeId =
                Jsoup.parse(seasonData)
                        .body()
                        .select(".episodes")
                        .find { it.attr("data-season").equals(data.season?.toString() ?: "1") }
                        ?.select("li a")
                        ?.find { it.attr("data-num").equals(data.episode?.toString() ?: "1") }
                        ?.attr("data-id")
                        ?: return
        val episodeDataUrl =
                "$url/ajax/server/list/$episodeId?vrf=${CineZoneUtils.vrfEncrypt(episodeId)}"
        val episodeData = app.get(episodeDataUrl).parsedSafe<ApiResponseHTML>()?.html ?: return

        Jsoup.parse(episodeData).body().select(".server").amap {
            val serverId = it.attr("data-id")
            val dataId = it.attr("data-link-id")
            val serverResUrl = "$url/ajax/server/$dataId?vrf=${CineZoneUtils.vrfEncrypt(dataId)}"
            val serverRes = app.get(serverResUrl).parsedSafe<AniwaveResponseServer>()
            val encUrl = serverRes?.result?.url ?: return@amap
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

    // #region - VidSrcNet (https://vidsrc.net) Extractor

    suspend fun vidsrcNetExtractor(
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
                iframedoc.selectFirst("div.serversList > div.server")?.attr("data-hash") ?: return
        val link = Extractvidsrcnetservers(serverhash) ?: return
        val URI =
                app.get(link, referer = "https://vidsrc.net/")
                        .document
                        .selectFirst("script:containsData(Playerjs)")
                        ?.data()
                        ?.substringAfter("file:\"#9")
                        ?.substringBefore("\"")
                        ?.replace(Regex("/@#@\\S+?=?="), "")
                        ?.let { base64Decode(it) }
                        ?: return
        loadExtractor(URI, referer = "https://vidsrc.net/", subtitleCallback, callback)
    }

    suspend fun Extractvidsrcnetservers(url: String): String? {
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
                        ?: return null
        return "http:$link"
    }

    // #endregion - VidSrcNet (https://vidsrc.net/) Extractor

    // #region - VidSrcTo (https://vidsrc.net) Extractor

    suspend fun vidsrcToExtractor(
            providerName: String?,
            url: String?,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        if (data.isAnime) return // right now, anime is not supported from VidsrcTo API
        val id = data.tmdbId ?: data.imdbId ?: return
        val iFrameUrl =
                if (data.season == null) {
                    "$url/embed/movie/$id"
                } else {
                    "$url/embed/tv/$id/${data.season}/${data.episode}"
                }
        val mediaId = app.get(iFrameUrl).document.selectFirst("ul.episodes li a")?.attr("data-id")
        val res =
                app.get("$url/ajax/embed/episode/$mediaId/sources")
                        .parsedSafe<VidsrctoEpisodeSources>()
        if (res?.status == 200) {
            res.result?.apmap { source ->
                val embedRes =
                        app.get("$url/ajax/embed/source/${source.id}")
                                .parsedSafe<VidsrctoEmbedSource>()
                val finalUrl = DecryptUrl(embedRes?.result?.encUrl ?: "")
                commonLinkLoader(
                        providerName,
                        VidsrcToUtils.serverName(source.title),
                        finalUrl,
                        null,
                        subtitleCallback,
                        callback
                )
            }
        }
    }

    private fun DecryptUrl(encUrl: String): String {
        var data = encUrl.toByteArray()
        data = Base64.decode(data, Base64.URL_SAFE)
        val rc4Key = SecretKeySpec("WXrUARXb1aDLaZjI".toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        data = cipher.doFinal(data)
        return URLDecoder.decode(data.toString(Charsets.UTF_8), "utf-8")
    }
    data class VidsrctoEpisodeSources(
            @JsonProperty("status") val status: Int,
            @JsonProperty("result") val result: List<VidsrctoResult>?
    )

    data class VidsrctoResult(
            @JsonProperty("id") val id: String,
            @JsonProperty("title") val title: String
    )

    data class VidsrctoEmbedSource(
            @JsonProperty("status") val status: Int,
            @JsonProperty("result") val result: VidsrctoUrl
    )

    data class VidsrctoUrl(@JsonProperty("url") val encUrl: String)

    // #endregion - VidSrcTo (https://vidsrc.net) Extractor

    // #region - Moflix (https://myfilestorage.xyz) Extractor

    suspend fun moflixExtractor(
            providerName: String?,
            url: String?,
            data: LinkData,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        if (data.tmdbId.isNullOrEmpty()) return
        val id =
                (if (data.season == null) {
                            "tmdb|movie|${data.tmdbId}"
                        } else {
                            "tmdb|series|${data.tmdbId}"
                        })
                        .let { base64Encode(it.toByteArray()) }

        val loaderUrl = "$url/api/v1/titles/$id?loader=titlePage"
        val url2 =
                if (data.season == null) {
                    loaderUrl
                } else {
                    val mediaId =
                            app.get(loaderUrl, referer = "$url/")
                                    .parsedSafe<MoflixResponse>()
                                    ?.title
                                    ?.id
                    "$url/api/v1/titles/$mediaId/seasons/${data.season}/episodes/${data.episode}?loader=episodePage"
                }

        val res = app.get(url2, referer = "$url/").parsedSafe<MoflixResponse>()
        (res?.episode ?: res?.title)?.videos?.filter { it.category.equals("full", true) }?.apmap {
                iframe ->
            val response = app.get(iframe.src ?: return@apmap, referer = "$url/")
            val host = CommonUtils.getBaseUrl(iframe.src)
            val doc = response.document.selectFirst("script:containsData(sources:)")?.data()
            val script =
                    if (doc.isNullOrEmpty()) {
                        getAndUnpack(response.text)
                    } else {
                        doc
                    }
            val m3u8 = Regex("file:\\s*\"(.*?m3u8.*?)\"").find(script)?.groupValues?.getOrNull(1)
            // not sure why this line messes with loading
            // if (CommonUtils.haveDub(m3u8 ?: return@apmap, "$host/") == false) return@apmap
            callback.invoke(
                    ExtractorLink(
                            providerName ?: "",
                            "$providerName:${iframe.name}",
                            m3u8 ?: return@apmap,
                            "$host/",
                            iframe.quality?.filter { it.isDigit() }?.toIntOrNull()
                                    ?: Qualities.Unknown.value,
                            INFER_TYPE
                    )
            )
        }
    }

    data class MoflixResponse(
            @JsonProperty("title") val title: Episode? = null,
            @JsonProperty("episode") val episode: Episode? = null,
    ) {
        data class Episode(
                @JsonProperty("id") val id: Int? = null,
                @JsonProperty("videos") val videos: ArrayList<Videos>? = arrayListOf(),
        ) {
            data class Videos(
                    @JsonProperty("name") val name: String? = null,
                    @JsonProperty("category") val category: String? = null,
                    @JsonProperty("src") val src: String? = null,
                    @JsonProperty("quality") val quality: String? = null,
            )
        }
    }

    // #endregion - Moflix (https://myfilestorage.xyz) Extractor
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
