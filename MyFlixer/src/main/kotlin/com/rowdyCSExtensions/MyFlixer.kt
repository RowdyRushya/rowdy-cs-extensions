package com.rowdyCSExtensions

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType

class MyFlixer(val plugin: MyFlixerPlugin) :
        MainAPI() { // all providers must be an intstance of MainAPI
    override var mainUrl = "https://myflixerz.to"
    override var name = "MyFlixer"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override var lang = "en"

    // enable this when your provider has a main page
    override val hasMainPage = true

    // this function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {
        return listOf<SearchResponse>()
    }

    override val mainPage =
            mainPageOf(
                    "$mainUrl/movie?page=" to "Popular Movies",
                    "$mainUrl/tv-show?page=" to "Popular TV Shows",
                    "$mainUrl/coming-soon?page=" to "Coming Soon",
                    "$mainUrl/top-imdb?page=" to "Top IMDB Rating",
            )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data + page
        val res = app.get(url)
        if (res.code == 200) {
            val home =
                    res.document.select("div.flw-item").mapNotNull { element ->
                        val title =
                                element.selectFirst("h2.film-name > a")?.attr("title")
                                        ?: return@mapNotNull null
                        val link =
                                element.selectFirst("h2.film-name > a")?.attr("href")
                                        ?: return@mapNotNull null
                        val poster =
                                element.selectFirst("img.film-poster-img")?.attr("data-src")
                                        ?: return@mapNotNull null
                        val type =
                                element.selectFirst("div.fd-infor span.fdi-type")?.text()
                                        ?: return@mapNotNull null

                        newMovieSearchResponse(title, link) { this.posterUrl = poster }
                    }

            return newHomePageResponse(request.name, home, true)
        } else {
            throw ErrorLoadingException("Could not load data")
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val res = app.get(url)
        if (res.code != 200) throw ErrorLoadingException("Could not load data" + url)

        val type = url.replace("https://", "").split("/")[1]
        val contentId = res.document.select("div.detail_page-watch").attr("data-id")
        val details = res.document.select("div.detail_page-infor")
        val name = details.select("h2.heading-name > a").text()
        if (type.contains("movie")) {
            return newMovieLoadResponse(name, url, TvType.Movie, url) {
                this.posterUrl = details.select("div.film-poster > img").attr("src")
                this.plot = details.select("div.description").text()
                this.rating =
                        details.select("button.btn-imdb")
                                .text()
                                .replace("N/A", "")
                                .replace("IMDB: ", "")
                                .toIntOrNull()
                // this.backgroundPosterUrl = bgPoster
                // this.year = year
                // this.duration = res.runtime
                // this.tags = keywords.takeIf { !it.isNullOrEmpty() } ?: genres
                // this.recommendations = recommendations
                // this.actors = actors
                addTrailer(res.document.select("iframe#iframe-trailer").attr("data-src"))
                // addImdbId("")
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
                                    }
                            )
                        }
            }
            return newTvSeriesLoadResponse(name, url, TvType.TvSeries, episodes) {
                this.posterUrl = details.select("div.film-poster > img").attr("src")
                this.plot = details.select("div.description").text()
                this.rating =
                        details.select("button.btn-imdb")
                                .text()
                                .replace("N/A", "")
                                .replace("IMDB: ", "")
                                .toIntOrNull()
                // this.backgroundPosterUrl = bgPoster
                // this.year = year
                // this.duration = res.runtime
                // this.tags = keywords.takeIf { !it.isNullOrEmpty() } ?: genres
                // this.recommendations = recommendations
                // this.actors = actors
                addTrailer(res.document.select("iframe#iframe-trailer").attr("data-src"))
                // addImdbId("")
            }
        }
    }
}
