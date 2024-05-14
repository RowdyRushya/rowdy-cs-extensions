package com.RowdyAvocado

// import android.util.Log

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
            frag.show(context.supportFragmentManager, "")
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
