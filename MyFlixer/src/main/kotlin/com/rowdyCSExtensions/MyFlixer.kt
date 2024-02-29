package com.rowdyCSExtensions

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document

class MyFlixer(val plugin: MyFlixerPlugin) : MainAPI() {
    override var mainUrl = "https://myflixerz.to"
    override var name = "MyFlixer"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "en"
    override val hasMainPage = true

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search/${query.replace(" ", "-")}"
        val response = app.get(url)
        if (response.code == 200) return searchResponseBuilder(response.document)
        else return listOf<SearchResponse>()
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override val mainPage =
            mainPageOf(
                    "$mainUrl/movie?page=" to "Popular Movies",
                    "$mainUrl/tv-show?page=" to "Popular TV Shows",
                    "$mainUrl/coming-soon?page=" to "Coming Soon",
                    "$mainUrl/top-imdb?page=" to "Top IMDB Rating",
            )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data + page
        val response = app.get(url)
        if (response.code == 200)
                return newHomePageResponse(
                        request.name,
                        searchResponseBuilder(response.document),
                        true
                )
        else throw ErrorLoadingException("Could not load data")
    }

    override suspend fun load(url: String): LoadResponse {
        val res = app.get(url)
        if (res.code != 200) throw ErrorLoadingException("Could not load data" + url)

        val type = url.replace("https://", "").split("/")[1]
        val contentId = res.document.select("div.detail_page-watch").attr("data-id")
        val details = res.document.select("div.detail_page-infor")
        val name = details.select("h2.heading-name > a").text()
        if (type.contains("movie")) {
            return newMovieLoadResponse(name, url, TvType.Movie, "list/" + contentId) {
                this.posterUrl = details.select("div.film-poster > img").attr("src")
                this.plot = details.select("div.description").text()
                this.rating =
                        details.select("button.btn-imdb")
                                .text()
                                .replace("N/A", "")
                                .replace("IMDB: ", "")
                                .toIntOrNull()
                addTrailer(res.document.select("iframe#iframe-trailer").attr("data-src"))
            }
        } else {
            val episodes = ArrayList<Episode>()
            val seasonsRes =
                    app.get("$mainUrl/ajax/season/list/$contentId").document.select("a.ss-item")

            seasonsRes.forEach { season ->
                val seasonId = season.attr("data-id")
                val seasonNum = season.text().replace("Season ", "")
                app.get("$mainUrl/ajax/season/episodes/$seasonId")
                        .document
                        .select("a.eps-item")
                        .forEach { episode ->
                            val epId = episode.attr("data-id")
                            val (epNum, epName) =
                                    Regex("Eps (\\d+): (.+)").find(episode.attr("title"))!!
                                            .destructured

                            episodes.add(
                                    newEpisode(epId) {
                                        this.name = epName
                                        this.episode = epNum.toInt()
                                        this.season = seasonNum.toInt()
                                        this.data = "servers/" + epId
                                    }
                            )
                        }
            }
            return newTvSeriesLoadResponse(name, "", TvType.TvSeries, episodes) {
                this.posterUrl = details.select("div.film-poster > img").attr("src")
                this.plot = details.select("div.description").text()
                this.rating =
                        details.select("button.btn-imdb")
                                .text()
                                .replace("N/A", "")
                                .replace("IMDB: ", "")
                                .toIntOrNull()
                addTrailer(res.document.select("iframe#iframe-trailer").attr("data-src"))
            }
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val serversRes = app.get("$mainUrl/ajax/episode/$data").document.select("a.link-item")
        serversRes.forEach { server ->
            val linkId =
                    if (server.attr("data-linkid").isNullOrEmpty()) server.attr("data-id")
                    else server.attr("data-linkid")
            val source = app.get("$mainUrl/ajax/episode/sources/$linkId").parsedSafe<Source>()
            if (source?.link.toString().contains("megacloud.tv/embed-1"))
                    Megacloud2().getUrl(source?.link.toString(), null, subtitleCallback, callback)
            else loadExtractor(source?.link.toString(), subtitleCallback, callback)
        }
        return true
    }

    data class Source(
            @JsonProperty("type") var type: String,
            @JsonProperty("link") var link: String
    )

    private fun searchResponseBuilder(webDocument: Document): List<SearchResponse> {
        val searchCollection =
                webDocument.select("div.flw-item").mapNotNull { element ->
                    val title =
                            element.selectFirst("h2.film-name > a")?.attr("title")
                                    ?: return@mapNotNull null
                    val link =
                            element.selectFirst("h2.film-name > a")?.attr("href")
                                    ?: return@mapNotNull null
                    val poster =
                            element.selectFirst("img.film-poster-img")?.attr("data-src")
                                    ?: return@mapNotNull null

                    newMovieSearchResponse(title, link) { this.posterUrl = poster }
                }
        return searchCollection
    }
}
