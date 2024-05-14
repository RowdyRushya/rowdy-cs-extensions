package com.RowdyAvocado

// import android.util.Log
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.utils.AppUtils.setDefaultFocus

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class RowdySettings(val plugin: RowdyPlugin) : BottomSheetDialogFragment() {
    private var param1: String? = null
    private var param2: String? = null

    private val res: Resources = plugin.resources ?: throw Exception("Unable to read resources")

    private var mediaProviders: Array<Provider> = plugin.storage.mediaProviders
    private var animeProviders: Array<Provider> = plugin.storage.animeProviders

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    // #region - necessary functions
    private fun getLayout(name: String, inflater: LayoutInflater, container: ViewGroup?): View {
        val id = res.getIdentifier(name, "layout", BuildConfig.LIBRARY_PACKAGE_NAME)
        val layout = res.getLayout(id)
        return inflater.inflate(layout, container, false)
    }

    private fun getDrawable(name: String): Drawable {
        val id = res.getIdentifier(name, "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
        return res.getDrawable(id, null) ?: throw Exception("Unable to find drawable $name")
    }

    private fun getString(name: String): String {
        val id = res.getIdentifier(name, "string", BuildConfig.LIBRARY_PACKAGE_NAME)
        return res.getString(id)
    }

    private fun <T : View> View.findView(name: String): T {
        val id = plugin.resources!!.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
        return this.findViewById(id)
    }

    private fun View.makeTvCompatible() {
        val outlineId = res.getIdentifier("outline", "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
        this.background = res.getDrawable(outlineId, null)
    }
    // #endregion - necessary functions

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val settings = getLayout("settings", inflater, container)

        // #region - building save button and its click listener
        val saveBtn = settings.findView<ImageView>("save")
        saveBtn.setImageDrawable(getDrawable("save_icon"))
        saveBtn.makeTvCompatible()
        saveBtn.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.reload(context)
                        showToast("Saved")
                        dismiss()
                    }
                }
        )
        // #endregion - building save button and its click listener

        // #region - building delete button and its alert as well as its click listener
        val deleteBtn = settings.findView<ImageView>("delete")
        deleteBtn.setImageDrawable(getDrawable("delete_icon"))
        deleteBtn.makeTvCompatible()
        deleteBtn.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        AlertDialog.Builder(
                                        context ?: throw Exception("Unable to build alert dialog")
                                )
                                .setTitle("Reset Rowdy")
                                .setMessage("This will reset all settings.")
                                .setPositiveButton(
                                        "Reset",
                                        object : DialogInterface.OnClickListener {
                                            override fun onClick(p0: DialogInterface, p1: Int) {
                                                plugin.storage.deleteAllData()
                                                plugin.reload(context)
                                                showToast("Reset completed")
                                                dismiss()
                                            }
                                        }
                                )
                                .setNegativeButton("Cancel", null)
                                .show()
                                .setDefaultFocus()
                    }
                }
        )
        // #endregion - building detete button and its alert as well as its click listener

        // #region - building Anime Meta Switch with its click listener
        val mediaMetaSwitch = settings.findView<Switch>("media_meta")
        mediaMetaSwitch.makeTvCompatible()
        val mediaPrdConfig = settings.findView<LinearLayout>("media_providers_config")
        val mediaMetaRadioGroup = settings.findView<RadioGroup>("media_meta_group")

        mediaPrdConfig.visibility = if (plugin.storage.isMediaService) View.VISIBLE else View.GONE
        mediaMetaRadioGroup.visibility =
                if (plugin.storage.isMediaService) View.VISIBLE else View.GONE
        mediaMetaSwitch.isChecked = plugin.storage.isMediaService

        mediaMetaSwitch.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.storage.isMediaService = mediaMetaSwitch.isChecked
                        mediaPrdConfig.visibility =
                                if (mediaMetaSwitch.isChecked) View.VISIBLE else View.GONE
                        mediaMetaRadioGroup.visibility =
                                if (mediaMetaSwitch.isChecked) View.VISIBLE else View.GONE
                    }
                }
        )
        // #endregion - building Anime Meta Switch with its click listener

        // #region - building Media Meta Services List with its click listener
        val simklRadio = settings.findView<RadioButton>("simkl")
        simklRadio.makeTvCompatible()
        val tmdbRadio = settings.findView<RadioButton>("tmdb")
        tmdbRadio.makeTvCompatible()
        val traktRadio = settings.findView<RadioButton>("trakt")
        traktRadio.makeTvCompatible()
        simklRadio.isChecked = plugin.storage.mediaService.equals("Simkl")
        tmdbRadio.isChecked = plugin.storage.mediaService.equals("Tmdb")
        traktRadio.isChecked = plugin.storage.mediaService.equals("Trakt")

        simklRadio.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.storage.mediaService = "Simkl"
                    }
                }
        )
        tmdbRadio.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.storage.mediaService = "Tmdb"
                    }
                }
        )
        traktRadio.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.storage.mediaService = "Trakt"
                    }
                }
        )
        // #endregion - building Media Meta Services List with its click listener

        // #region - building Anime Meta Switch with its click listener
        val animeMetaSwitch = settings.findView<Switch>("anime_meta")
        animeMetaSwitch.makeTvCompatible()
        val animePrdConfig = settings.findView<LinearLayout>("anime_providers_config")
        val animeMetaRadioGroup = settings.findView<RadioGroup>("anime_meta_group")
        animePrdConfig.visibility = if (plugin.storage.isAnimeService) View.VISIBLE else View.GONE
        animeMetaRadioGroup.visibility =
                if (plugin.storage.isAnimeService) View.VISIBLE else View.GONE
        animeMetaSwitch.isChecked = plugin.storage.isAnimeService
        animeMetaSwitch.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.storage.isAnimeService = animeMetaSwitch.isChecked
                        animePrdConfig.visibility =
                                if (animeMetaSwitch.isChecked) View.VISIBLE else View.GONE
                        animeMetaRadioGroup.visibility =
                                if (animeMetaSwitch.isChecked) View.VISIBLE else View.GONE
                    }
                }
        )
        // #endregion - building Anime Meta Switch with its click listener

        // #region - building Anime Meta Services List with its click listener
        val anilistRadio = settings.findView<RadioButton>("anilist")
        anilistRadio.makeTvCompatible()
        val malRadio = settings.findView<RadioButton>("mal")
        malRadio.makeTvCompatible()
        anilistRadio.isChecked = plugin.storage.animeService.equals("Anilist")
        malRadio.isChecked = plugin.storage.animeService.equals("MyAnimeList")

        anilistRadio.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.storage.animeService = "Anilist"
                    }
                }
        )
        malRadio.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.storage.animeService = "MyAnimeList"
                    }
                }
        )
        // #endregion - building Anime Meta Services List with its click listener

        // #region - building Media Providers List with its click listener
        val mediaProvidersListTitle = settings.findView<TextView>("media_providers_list_title")
        mediaProvidersListTitle.makeTvCompatible()
        val mediaProvidersList = settings.findView<LinearLayout>("media_providers_list")

        mediaProvidersListTitle.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        if (mediaProvidersList.visibility == View.GONE) {
                            mediaProvidersListTitle.text = "▼ Media Providers"
                            mediaProvidersList.visibility = View.VISIBLE
                        } else {
                            mediaProvidersListTitle.text = "▶ Media Providers"
                            mediaProvidersList.visibility = View.GONE
                        }
                    }
                }
        )
        mediaProviders.forEach { provider ->
            mediaProvidersList.addView(buildProviderView(provider, inflater, container))
        }
        // #endregion - building Media Providers List with its click listener

        // #region - building Anime Providers List with its click listener
        val animeProvidersListTitle = settings.findView<TextView>("anime_providers_list_title")
        animeProvidersListTitle.makeTvCompatible()
        val animeProvidersList = settings.findView<LinearLayout>("anime_providers_list")
        animeProvidersListTitle.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        if (animeProvidersList.visibility == View.GONE) {
                            animeProvidersListTitle.text = "▼ Anime Providers"
                            animeProvidersList.visibility = View.VISIBLE
                        } else {
                            animeProvidersListTitle.text = "▶ Anime Providers"
                            animeProvidersList.visibility = View.GONE
                        }
                    }
                }
        )
        animeProviders.forEach { provider ->
            animeProvidersList.addView(buildProviderView(provider, inflater, container))
        }
        // #endregion - building Media Providers List with its click listener

        return settings
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {}

    fun buildProviderView(
            provider: Provider,
            inflater: LayoutInflater,
            container: ViewGroup?
    ): View {

        // #region - collecting required resources and setting necessary details
        val providerView = getLayout("provider", inflater, container)
        val providerCheckBox = providerView.findView<CheckBox>("provider")
        providerCheckBox.makeTvCompatible()

        val domainEdit = providerView.findView<ImageView>("domain_edit")
        domainEdit.setImageDrawable(getDrawable("edit_icon"))
        domainEdit.makeTvCompatible()
        // #endregion - collecting required resources and setting necessary details

        providerCheckBox.text = provider.name + if (provider.userModified) "*" else ""
        providerCheckBox.isChecked = provider.enabled
        providerCheckBox.setOnCheckedChangeListener(
                object : OnCheckedChangeListener {
                    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                        provider.enabled = isChecked
                        plugin.storage.mediaProviders = mediaProviders
                        plugin.storage.animeProviders = animeProviders
                    }
                }
        )

        // #region - Set domain and its edit + reset buttons with listeners
        val domain = providerView.findView<TextView>("domain")

        domain.text = provider.domain
        domainEdit.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        val editText = EditText(context)
                        editText.setText(provider.domain)

                        editText.setInputType(InputType.TYPE_CLASS_TEXT)
                        AlertDialog.Builder(
                                        context ?: throw Exception("Unable to build alert dialog")
                                )
                                .setTitle("Update Domain")
                                .setView(editText)
                                .setPositiveButton(
                                        "Save",
                                        object : DialogInterface.OnClickListener {
                                            override fun onClick(p0: DialogInterface, p1: Int) {
                                                provider.domain = editText.text.toString()
                                                provider.userModified = true
                                                plugin.storage.mediaProviders = mediaProviders
                                                plugin.storage.animeProviders = animeProviders
                                                plugin.reload(context)
                                                showToast("Domain changed")
                                                dismiss()
                                            }
                                        }
                                )
                                .setNegativeButton(
                                        "Reset",
                                        object : DialogInterface.OnClickListener {
                                            override fun onClick(p0: DialogInterface, p1: Int) {
                                                provider.userModified = false
                                                plugin.storage.mediaProviders = mediaProviders
                                                plugin.storage.animeProviders = animeProviders
                                                plugin.reload(context)
                                                showToast("Domain reset complete.")
                                                dismiss()
                                            }
                                        }
                                )
                                .show()
                                .setDefaultFocus()
                    }
                }
        )
        // #endregion - Set domain and its edit + reset buttons with listeners

        return providerView
    }
}
