package com.RowdyAvocado

import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.MainActivity.Companion.afterPluginsLoadedEvent
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.PluginManager

@CloudstreamPlugin
class RowdyPlugin : Plugin() {
    var activity: AppCompatActivity? = null

    // #region - custom data variables
    // var isAnimeService: Boolean
    //     get() = getKey("ROWDY_IS_ANIME_SYNC") ?: false
    //     set(value) {
    //         setKey("ROWDY_IS_ANIME_SYNC", value)
    //     }

    // var isMediaService: Boolean
    //     get() = getKey("ROWDY_IS_MEDIA_SYNC") ?: false
    //     set(value) {
    //         setKey("ROWDY_IS_MEDIA_SYNC", value)
    //     }

    // var mediaService: String
    //     get() = getKey("MEDIA_SYNC_SERVICE") ?: "Simkl"
    //     set(value) {
    //         setKey("MEDIA_SYNC_SERVICE", value)
    //     }

    // var animeService: String
    //     get() = getKey("ANIME_SYNC_SERVICE") ?: "Anilist"
    //     set(value) {
    //         setKey("ANIME_SYNC_SERVICE", value)
    //     }

    // var mediaProviders: Array<Provider>
    //     get() = buildProvidersList(mediaPrdList, "MEDIA_PROVIDERS")
    //     set(value) {
    //         setKey("MEDIA_PROVIDERS", value)
    //     }

    // var animeProviders: Array<Provider>
    //     get() = buildProvidersList(animePrdList, "ANIME_PROVIDERS")
    //     set(value) {
    //         setKey("ANIME_PROVIDERS", value)
    //     }
    // #endregion - custom data variables

    // private val mediaPrdList =
    //         listOf(
    //                 "CineZone" to "https://cinezone.to",
    //                 "VidsrcNet" to "https://vidsrc.net",
    //                 "VidsrcTo" to "https://vidsrc.to",
    //         )

    // private val animePrdList =
    //         listOf(
    //                 "Aniwave" to "https://aniwave.to",
    //                 "Anitaku" to "https://www.anitaku.com",
    //                 "HiAnime" to "https://www.hianime.com"
    //         )

    val providers = RowdyExtractorUtil
    val storage = StorageManager

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
        if (storage.isMediaService)
                when (storage.mediaService) {
                    "Simkl" -> registerMainAPI(Simkl(this))
                    "Tmdb" -> registerMainAPI(Tmdb(this))
                    "Trakt" -> registerMainAPI(Trakt(this))
                }
        if (storage.isAnimeService)
                when (storage.animeService) {
                    "Anilist" -> registerMainAPI(Anilist(this))
                    "MyAnimeList" -> registerMainAPI(MyAnimeList(this))
                }
        openSettings = {
            val frag = RowdySettings(this)
            frag.show(activity!!.supportFragmentManager, "")
        }
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
