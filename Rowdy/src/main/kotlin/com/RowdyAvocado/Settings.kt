package com.RowdyAvocado

import android.content.DialogInterface
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
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.utils.AppUtils.setDefaultFocus

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class RowdySettings(val plugin: RowdyPlugin) : BottomSheetDialogFragment() {
    private var param1: String? = null
    private var param2: String? = null

    private var mediaProviders: Array<Provider> = plugin.mediaProviders
    private var animeProviders: Array<Provider> = plugin.animeProviders

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    // #region - necessary functions
    private fun getDrawable(name: String): Drawable? {
        val id =
                plugin.resources!!.getIdentifier(name, "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
        return ResourcesCompat.getDrawable(plugin.resources!!, id, null)
    }

    private fun getString(name: String): String? {
        val id = plugin.resources!!.getIdentifier(name, "string", BuildConfig.LIBRARY_PACKAGE_NAME)
        return plugin.resources!!.getString(id)
    }

    private fun <T : View> View.findView(name: String): T {
        val id = plugin.resources!!.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
        return this.findViewById(id)
    }
    // #endregion - necessary functions

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        // #region - collecting required resources
        val settingsLayoutId =
                plugin.resources!!.getIdentifier(
                        "settings",
                        "layout",
                        BuildConfig.LIBRARY_PACKAGE_NAME
                )
        val settingsLayout = plugin.resources!!.getLayout(settingsLayoutId)
        val settings = inflater.inflate(settingsLayout, container, false)
        val outlineId =
                plugin.resources!!.getIdentifier(
                        "outline",
                        "drawable",
                        BuildConfig.LIBRARY_PACKAGE_NAME
                )
        val providerLayoutId =
                plugin.resources!!.getIdentifier(
                        "provider",
                        "layout",
                        BuildConfig.LIBRARY_PACKAGE_NAME
                )
        val saveIconId =
                plugin.resources!!.getIdentifier(
                        "save_icon",
                        "drawable",
                        BuildConfig.LIBRARY_PACKAGE_NAME
                )
        val deleteIconId =
                plugin.resources!!.getIdentifier(
                        "delete_icon",
                        "drawable",
                        BuildConfig.LIBRARY_PACKAGE_NAME
                )
        val editIconId =
                plugin.resources!!.getIdentifier(
                        "edit_icon",
                        "drawable",
                        BuildConfig.LIBRARY_PACKAGE_NAME
                )
        val resetIconId =
                plugin.resources!!.getIdentifier(
                        "reset_icon",
                        "drawable",
                        BuildConfig.LIBRARY_PACKAGE_NAME
                )
        // #endregion - collecting required resources

        // #region - building save button and its click listener
        val saveBtn = settings.findView<ImageView>("save")
        saveBtn.setImageDrawable(plugin.resources!!.getDrawable(saveIconId, null))
        saveBtn.background = plugin.resources!!.getDrawable(outlineId, null)
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
        val alertBuilder = AlertDialog.Builder(context!!)
        val dialogClickListener =
                DialogInterface.OnClickListener { _, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            plugin.deleteAllData()
                            plugin.reload(context)
                            showToast("Reset completed")
                            dismiss()
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {}
                    }
                }
        val deleteBtn = settings.findView<ImageView>("delete")
        deleteBtn.setImageDrawable(plugin.resources!!.getDrawable(deleteIconId, null))
        deleteBtn.background = plugin.resources!!.getDrawable(outlineId, null)
        deleteBtn.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        alertBuilder
                                .setTitle("Reset Rowdy")
                                .setMessage("This will reset all settings.")
                                .setPositiveButton("Reset", dialogClickListener)
                                .setNegativeButton("Cancel", dialogClickListener)
                                .show()
                                .setDefaultFocus()
                    }
                }
        )
        // #endregion - building detete button and its alert as well as its click listener

        // #region - building Anime Sync Switch with its click listener
        val mediaSyncSwitch = settings.findView<Switch>("media_sync")
        mediaSyncSwitch.background = plugin.resources!!.getDrawable(outlineId, null)
        val mediaPrdConfig = settings.findView<LinearLayout>("media_providers_config")
        val mediaSyncRadioGroup = settings.findView<RadioGroup>("media_sync_group")

        mediaPrdConfig.visibility = if (plugin.isMediaSync) View.VISIBLE else View.GONE
        mediaSyncRadioGroup.visibility = if (plugin.isMediaSync) View.VISIBLE else View.GONE
        mediaSyncSwitch.isChecked = plugin.isMediaSync

        mediaSyncSwitch.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.isMediaSync = mediaSyncSwitch.isChecked
                        mediaPrdConfig.visibility =
                                if (mediaSyncSwitch.isChecked) View.VISIBLE else View.GONE
                        mediaSyncRadioGroup.visibility =
                                if (mediaSyncSwitch.isChecked) View.VISIBLE else View.GONE
                    }
                }
        )
        // #endregion - building Anime Sync Switch with its click listener

        // #region - building Media Sync Services List with its click listener
        val simklRadio = settings.findView<RadioButton>("simkl")
        simklRadio.background = plugin.resources!!.getDrawable(outlineId, null)
        val tmdbRadio = settings.findView<RadioButton>("tmdb")
        tmdbRadio.background = plugin.resources!!.getDrawable(outlineId, null)
        val traktRadio = settings.findView<RadioButton>("trakt")
        traktRadio.background = plugin.resources!!.getDrawable(outlineId, null)
        simklRadio.isChecked = plugin.mediaSyncService.equals("Simkl")
        tmdbRadio.isChecked = plugin.mediaSyncService.equals("Tmdb")
        traktRadio.isChecked = plugin.mediaSyncService.equals("Trakt")

        simklRadio.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.mediaSyncService = "Simkl"
                    }
                }
        )
        tmdbRadio.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.mediaSyncService = "Tmdb"
                    }
                }
        )
        traktRadio.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.mediaSyncService = "Trakt"
                    }
                }
        )
        // #endregion - building Media Sync Services List with its click listener

        // #region - building Anime Sync Switch with its click listener
        val animeSyncSwitch = settings.findView<Switch>("anime_sync")
        animeSyncSwitch.background = plugin.resources!!.getDrawable(outlineId, null)
        val animePrdConfig = settings.findView<LinearLayout>("anime_providers_config")
        val animeSyncRadioGroup = settings.findView<RadioGroup>("anime_sync_group")
        animePrdConfig.visibility = if (plugin.isAnimeSync) View.VISIBLE else View.GONE
        animeSyncRadioGroup.visibility = if (plugin.isAnimeSync) View.VISIBLE else View.GONE
        animeSyncSwitch.isChecked = plugin.isAnimeSync
        animeSyncSwitch.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.isAnimeSync = animeSyncSwitch.isChecked
                        animePrdConfig.visibility =
                                if (animeSyncSwitch.isChecked) View.VISIBLE else View.GONE
                        animeSyncRadioGroup.visibility =
                                if (animeSyncSwitch.isChecked) View.VISIBLE else View.GONE
                    }
                }
        )
        // #endregion - building Anime Sync Switch with its click listener

        // #region - building Anime Sync Services List with its click listener
        val anilistRadio = settings.findView<RadioButton>("anilist")
        anilistRadio.background = plugin.resources!!.getDrawable(outlineId, null)
        val malRadio = settings.findView<RadioButton>("mal")
        malRadio.background = plugin.resources!!.getDrawable(outlineId, null)
        anilistRadio.isChecked = plugin.animeSyncService.equals("Anilist")
        malRadio.isChecked = plugin.animeSyncService.equals("MyAnimeList")

        anilistRadio.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.animeSyncService = "Anilist"
                    }
                }
        )
        malRadio.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.animeSyncService = "MyAnimeList"
                    }
                }
        )
        // #endregion - building Anime Sync Services List with its click listener

        // #region - building Media Providers List with its click listener
        val mediaProvidersListTitle = settings.findView<TextView>("media_providers_list_title")
        mediaProvidersListTitle.background = plugin.resources!!.getDrawable(outlineId, null)
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
            mediaProvidersList.addView(
                    buildProviderView(
                            provider,
                            providerLayoutId,
                            outlineId,
                            editIconId,
                            resetIconId,
                            inflater,
                            container,
                            alertBuilder
                    )
            )
        }
        // #endregion - building Media Providers List with its click listener

        // #region - building Anime Providers List with its click listener
        val animeProvidersListTitle = settings.findView<TextView>("anime_providers_list_title")
        animeProvidersListTitle.background = plugin.resources!!.getDrawable(outlineId, null)
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
            animeProvidersList.addView(
                    buildProviderView(
                            provider,
                            providerLayoutId,
                            outlineId,
                            editIconId,
                            resetIconId,
                            inflater,
                            container,
                            alertBuilder
                    )
            )
        }
        // #endregion - building Media Providers List with its click listener

        return settings
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {}

    fun buildProviderView(
            provider: Provider,
            providerLayoutId: Int,
            outlineId: Int,
            editIconId: Int,
            resetIconId: Int,
            inflater: LayoutInflater,
            container: ViewGroup?,
            alertBuilder: AlertDialog.Builder
    ): View {

        // #region - collecting required resources and setting necessary details
        val providerLayout = plugin.resources!!.getLayout(providerLayoutId)
        val providerView = inflater.inflate(providerLayout, container, false)
        val providerCheckBox = providerView.findView<CheckBox>("provider")
        providerCheckBox.background = plugin.resources!!.getDrawable(outlineId, null)

        val domainEdit = providerView.findView<ImageView>("domain_edit")
        domainEdit.setImageDrawable(plugin.resources!!.getDrawable(editIconId, null))
        domainEdit.background = plugin.resources!!.getDrawable(outlineId, null)
        val domainReset = providerView.findView<ImageView>("domain_reset")
        domainReset.setImageDrawable(plugin.resources!!.getDrawable(resetIconId, null))
        domainReset.background = plugin.resources!!.getDrawable(outlineId, null)
        // #endregion - collecting required resources and setting necessary details

        providerCheckBox.text = provider.name + if (provider.userModified) "*" else ""
        providerCheckBox.isChecked = provider.enabled
        providerCheckBox.setOnCheckedChangeListener(
                object : OnCheckedChangeListener {
                    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                        provider.enabled = isChecked
                        plugin.mediaProviders = mediaProviders
                        plugin.animeProviders = animeProviders
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

                        val domainEditClickListener =
                                DialogInterface.OnClickListener { _, which ->
                                    when (which) {
                                        DialogInterface.BUTTON_POSITIVE -> {
                                            provider.domain = editText.text.toString()
                                            provider.userModified = true
                                            plugin.mediaProviders = mediaProviders
                                            plugin.animeProviders = animeProviders
                                            plugin.reload(context)
                                            showToast("Domain changed")
                                            dismiss()
                                        }
                                        DialogInterface.BUTTON_NEGATIVE -> {}
                                    }
                                }

                        editText.setInputType(InputType.TYPE_CLASS_TEXT)
                        alertBuilder
                                .setTitle("Update Domain")
                                .setView(editText)
                                .setPositiveButton("Save", domainEditClickListener)
                                .setNegativeButton("Cancel", domainEditClickListener)
                                .show()
                                .setDefaultFocus()
                    }
                }
        )

        domainReset.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        provider.userModified = false
                        plugin.mediaProviders = mediaProviders
                        plugin.animeProviders = animeProviders
                        plugin.reload(context)
                        showToast("Domain reset complete.")
                        dismiss()
                    }
                }
        )
        // #endregion - Set domain and its edit + reset buttons with listeners

        return providerView
    }
}
