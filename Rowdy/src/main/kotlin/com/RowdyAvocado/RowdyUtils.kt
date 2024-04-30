package com.RowdyAvocado

import com.fasterxml.jackson.annotation.JsonProperty

enum class ServerName {
    MyCloud,
    Mp4upload,
    Streamtape,
    Vidplay,
    Filemoon,
    NONE
}

enum class Type {
    ANIME,
    MEDIA,
    NONE
}

data class Provider(
        @JsonProperty("name") var name: String? = null,
        @JsonProperty("domain") var domain: String? = null,
        @JsonProperty("enabled") var enabled: Boolean = true,
        @JsonProperty("userModified") var userModified: Boolean = false
)

data class LinkData(
        val simklId: Int? = null,
        val traktId: Int? = null,
        val imdbId: String? = null,
        val tmdbId: String? = null,
        val tvdbId: Int? = null,
        val type: String? = null,
        val season: Int? = null,
        val episode: Int? = null,
        val aniId: String? = null,
        val animeId: String? = null,
        val title: String? = null,
        val year: Int? = null,
        val orgTitle: String? = null,
        val isAnime: Boolean = false,
        val airedYear: Int? = null,
        val lastSeason: Int? = null,
        val epsTitle: String? = null,
        val jpTitle: String? = null,
        val date: String? = null,
        val airedDate: String? = null,
        val isAsian: Boolean = false,
        val isBollywood: Boolean = false,
        val isCartoon: Boolean = false,
)
