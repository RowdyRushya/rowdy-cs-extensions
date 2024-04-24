package com.RowdyAvocado

import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey
import com.lagradost.cloudstream3.MainActivity.Companion.afterPluginsLoadedEvent
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.PluginManager

@CloudstreamPlugin
class RowdyPlugin : Plugin() {
    var activity: AppCompatActivity? = null

    // #region - custom data variables
    var isAnimeSync: Boolean
        get() = getKey("ROWDY_IS_ANIME_SYNC") ?: false
        set(value) {
            setKey("ROWDY_IS_ANIME_SYNC", value)
        }

    var isMediaSync: Boolean
        get() = getKey("ROWDY_IS_MEDIA_SYNC") ?: false
        set(value) {
            setKey("ROWDY_IS_MEDIA_SYNC", value)
        }

    var mediaSyncService: String
        get() = getKey("MEDIA_SYNC_SERVICE") ?: "Simkl"
        set(value) {
            setKey("MEDIA_SYNC_SERVICE", value)
        }

    var animeSyncService: String
        get() = getKey("ANIME_SYNC_SERVICE") ?: "Anilist"
        set(value) {
            setKey("ANIME_SYNC_SERVICE", value)
        }

    var mediaProviders: Array<Provider>
        get() = buildProvidersList(mediaPrdList, "MEDIA_PROVIDERS")
        set(value) {
            setKey("MEDIA_PROVIDERS", value)
        }

    var animeProviders: Array<Provider>
        get() = buildProvidersList(animePrdList, "ANIME_PROVIDERS")
        set(value) {
            setKey("ANIME_PROVIDERS", value)
        }
    // #endregion - custom data variables

    private val mediaPrdList =
            listOf(
                    "CineZone" to "https://cinezone.to",
                    "Superstream" to "https://www.superstream.to",
                    "VidSrcTo" to "https://www.vidsrc.to"
            )

    private val animePrdList =
            listOf(
                    "Aniwave" to "https://aniwave.to",
                    "Anitaku" to "https://www.anitaku.com",
                    "HiAnime" to "https://www.hianime.com"
            )

    companion object {
        inline fun Handler.postFunction(crossinline function: () -> Unit) {
            this.post(
                    object : Runnable {
                        override fun run() {
                            function()
                        }
                    }
            )
        }
    }

    override fun load(context: Context) {
        activity = context as AppCompatActivity
        // All providers should be added in this manner
        if (isMediaSync)
                when (mediaSyncService) {
                    "Simkl" -> registerMainAPI(Simkl(this))
                    "Tmdb" -> registerMainAPI(Tmdb(this))
                    "Trakt" -> registerMainAPI(Trakt(this))
                }
        if (isAnimeSync)
                when (animeSyncService) {
                    "Anilist" -> registerMainAPI(Anilist(this))
                    "MyAnimeList" -> registerMainAPI(MyAnimeList(this))
                }
        openSettings = {
            val frag = RowdySettings(this)
            frag.show(activity!!.supportFragmentManager, "")
        }
    }

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
                        "ANIME_PROVIDERS"
                )
                .forEach { setKey(it, null) }
    }

    fun reload(context: Context?) {
        val pluginData = PluginManager.getPluginsOnline().find { it.internalName.contains("Rowdy") }
        if (pluginData == null) {
            PluginManager.hotReloadAllLocalPlugins(context as AppCompatActivity)
        } else {
            PluginManager.unloadPlugin(pluginData.filePath)
            PluginManager.loadAllOnlinePlugins(context!!)
            afterPluginsLoadedEvent.invoke(true)
        }
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
                    newProviderList += Provider(it.first, it.second, false, false)
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

// RowdyRushya — Today at 4:17 AM
// i am totally aware of it and I will not register the MainAPI class in plugin if it the user is
// not logged in
// brother of yam — Today at 4:17 AM
// no
// like it is very wrong because it hardcodes the index
// and on top of that it does not call init
// use open val api: SyncApi get()= AccountManager.aniListApi
// that should be better
// RowdyRushya — Today at 4:18 AM
// oh okay, thanks :Prayge~1: :Prayge~1:
// brother of yam — Today at 4:18 AM
// do remember that SyncApi throws
// use SyncRepo for the safe API
