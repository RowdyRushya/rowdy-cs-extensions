package com.rowdyCSExtensions

import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.APIHolder.allProviders
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class UltimaPlugin : Plugin() {
    var activity: AppCompatActivity? = null

    var currentSections: Array<PluginInfo>
        get() = getKey("ULTIMA_PROVIDER_LIST") ?: arrayOf(PluginInfo())
        set(value) {
            setKey("ULTIMA_PROVIDER_LIST", value)
        }

    companion object {
        /**
         * Used to make Runnables work properly on Android 21 Otherwise you get: ERROR:D8:
         * Invoke-customs are only supported starting with Android O (--min-api 26)
         */
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
        registerMainAPI(Ultima(this))

        openSettings = { ctx ->
            val frag = UltimaFragment(this)
            frag.show(activity!!.supportFragmentManager, "")
        }

        // currentSections = getSections()
    }

    // fun fetchSections(): Array<PluginInfo> {
    //     synchronized(allProviders) {
    //         var providerList = emptyArray<PluginInfo>()
    //         allProviders.forEach { provider ->
    //             if (!provider.name.equals("Ultima")) {
    //                 var mainPageList = emptyArray<SectionInfo>()
    //                 provider.mainPage.forEach { section ->
    //                     var sectionData =
    //                             SectionInfo(section.name, section.data, provider.name, false)
    //                     mainPageList += sectionData
    //                 }
    //                 var providerData = PluginInfo(provider.name, mainPageList)
    //                 providerList += providerData
    //             }
    //         }
    //         return providerList
    //     }
    // }

    fun getSections(): Array<PluginInfo> {
        synchronized(allProviders) {
            var providers = allProviders
            var newProviderList = emptyArray<PluginInfo>()

            providers.forEach { provider ->
                if (!provider.name.equals("Ultima")) {
                    val doesProviderExist =
                            getKey<Array<PluginInfo>>("ULTIMA_PROVIDER_LIST")?.find {
                                it.name == provider.name
                            }
                    if (doesProviderExist == null) {
                        var mainPageList = emptyArray<SectionInfo>()
                        provider.mainPage.forEach { section ->
                            var sectionData =
                                    SectionInfo(section.name, section.data, provider.name, false)
                            mainPageList += sectionData
                        }
                        var providerData = PluginInfo(provider.name, mainPageList)
                        newProviderList += providerData
                    } else {
                        newProviderList += doesProviderExist
                    }
                }
            }

            if (newProviderList.size == providers.size) {
                return newProviderList
            } else {
                return newProviderList
                        .filter { new -> providers.find { new.name == it.name } != null }
                        .toTypedArray()
            }
        }
    }

    fun setSections(data: Array<PluginInfo>) {
        setKey("ULTIMA_PROVIDER_LIST", data)
    }

    data class SectionInfo(
            @JsonProperty("name") var name: String? = null,
            @JsonProperty("url") var url: String? = null,
            @JsonProperty("pluginName") var pluginName: String? = null,
            @JsonProperty("enabled") var enabled: Boolean? = false
    )

    data class PluginInfo(
            @JsonProperty("name") var name: String? = null,
            @JsonProperty("sections") var sections: Array<SectionInfo>? = null
    )
}
