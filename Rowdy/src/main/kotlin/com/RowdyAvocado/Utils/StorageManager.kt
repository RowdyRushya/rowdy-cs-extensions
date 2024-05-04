package com.RowdyAvocado

import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey

object StorageManager {

    // #region - custom data variables
    var isAnimeService: Boolean
        get() = getKey("ROWDY_IS_ANIME_SERVICE") ?: false
        set(value) {
            setKey("ROWDY_IS_ANIME_SERVICE", value)
        }

    var isMediaService: Boolean
        get() = getKey("ROWDY_IS_MEDIA_SERVICE") ?: false
        set(value) {
            setKey("ROWDY_IS_MEDIA_SERVICE", value)
        }

    var mediaService: String
        get() = getKey("ROWDY_MEDIA_SERVICE") ?: "Simkl"
        set(value) {
            setKey("ROWDY_MEDIA_SERVICE", value)
        }

    var animeService: String
        get() = getKey("ROWDY_ANIME_SERVICE") ?: "Anilist"
        set(value) {
            setKey("ROWDY_ANIME_SERVICE", value)
        }

    var mediaProviders: Array<Provider>
        get() = buildProvidersList(RowdyExtractorUtil.mediaPrdList, "ROWDY_MEDIA_PROVIDERS")
        set(value) {
            setKey("ROWDY_MEDIA_PROVIDERS", value)
        }

    var animeProviders: Array<Provider>
        get() = buildProvidersList(RowdyExtractorUtil.animePrdList, "ROWDY_ANIME_PROVIDERS")
        set(value) {
            setKey("ROWDY_ANIME_PROVIDERS", value)
        }
    // #endregion - custom data variables

    fun deleteAllData() {
        listOf(
                        "ROWDY_ANIME_SYNC",
                        "ROWDY_IS_ANIME_PROD",
                        "ROWDY_IS_MEDIA_SYNC",
                        "ROWDY_IS_ANIME_SYNC",
                        "MEDIA_PROVIDER",
                        "ANIME_PROVIDER",
                        "MEDIA_SYNC_SERVICE",
                        "ANIME_SYNC_SERVICE",
                        "MEDIA_PROVIDERS",
                        "ANIME_PROVIDERS",
                        "ROWDY_IS_MEDIA_SERVICE",
                        "ROWDY_IS_ANIME_SERVICE",
                        "ROWDY_MEDIA_SERVICE",
                        "ROWDY_ANIME_SERVICE",
                        "ROWDY_MEDIA_PROVIDERS",
                        "ROWDY_ANIME_PROVIDERS",
                )
                .forEach { setKey(it, null) }
    }

    private fun buildProvidersList(
            providers: List<Pair<String, String>>,
            key: String
    ): Array<Provider> {
        var storedProviders = getKey<Array<Provider>>(key)
        if (storedProviders != null) {
            var newProviderList = emptyArray<Provider>()
            providers.forEach {
                val oldProvider = storedProviders.find { p -> p.name.equals(it.first) }
                if (oldProvider == null) {
                    newProviderList += Provider(it.first, it.second, true, false)
                } else {
                    val domain = if (oldProvider.userModified) oldProvider.domain else it.second
                    newProviderList +=
                            Provider(
                                    it.first,
                                    domain,
                                    oldProvider.enabled,
                                    oldProvider.userModified
                            )
                }
            }
            return newProviderList
        } else {
            var data = emptyArray<Provider>()
            providers.forEach { data += Provider(it.first, it.second, true, false) }
            return data
        }
    }
}
