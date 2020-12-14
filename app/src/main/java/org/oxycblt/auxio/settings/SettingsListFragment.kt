package org.oxycblt.auxio.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import org.oxycblt.auxio.BuildConfig
import org.oxycblt.auxio.R
import org.oxycblt.auxio.logD
import org.oxycblt.auxio.playback.PlaybackViewModel
import org.oxycblt.auxio.recycler.DisplayMode
import org.oxycblt.auxio.settings.adapters.AccentAdapter
import org.oxycblt.auxio.ui.ACCENTS
import org.oxycblt.auxio.ui.accent
import org.oxycblt.auxio.ui.createToast
import org.oxycblt.auxio.ui.getDetailedAccentSummary

@Suppress("UNUSED")
class SettingsListFragment : PreferenceFragmentCompat() {
    private val playbackModel: PlaybackViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceScreen.children.forEach {
            recursivelyHandleChildren(it)
        }

        logD("Fragment created.")
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs_main, rootKey)
    }

    private fun recursivelyHandleChildren(pref: Preference) {
        if (pref is PreferenceCategory) {
            if (pref.title == getString(R.string.debug_title) && BuildConfig.DEBUG) {
                logD("Showing debug category.")

                pref.isVisible = true
            }

            pref.children.forEach {
                recursivelyHandleChildren(it)
            }
        } else {
            handlePreference(pref)
        }
    }

    private fun handlePreference(it: Preference) {
        it.apply {
            when (it.key) {
                SettingsManager.Keys.KEY_THEME -> {
                    setIcon(AppCompatDelegate.getDefaultNightMode().toThemeIcon())

                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, value ->
                        AppCompatDelegate.setDefaultNightMode((value as String).toThemeInt())

                        setIcon(AppCompatDelegate.getDefaultNightMode().toThemeIcon())

                        true
                    }
                }

                SettingsManager.Keys.KEY_ACCENT -> {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        showAccentDialog()
                        true
                    }

                    summary = getDetailedAccentSummary(requireActivity(), accent)
                }

                SettingsManager.Keys.KEY_EDGE_TO_EDGE -> {
                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                        requireActivity().recreate()

                        true
                    }
                }

                SettingsManager.Keys.KEY_LIBRARY_DISPLAY_MODE -> {
                    setIcon(SettingsManager.getInstance().libraryDisplayMode.iconRes)

                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, value ->
                        setIcon(DisplayMode.valueOfOrFallback(value as String).iconRes)

                        true
                    }
                }

                SettingsManager.Keys.KEY_DEBUG_SAVE -> {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        playbackModel.save(requireContext())
                        getString(R.string.debug_state_saved).createToast(requireContext())

                        true
                    }
                }
            }
        }
    }

    private fun showAccentDialog() {
        MaterialDialog(requireActivity()).show {
            title(R.string.setting_accent)

            // Roll my own RecyclerView since [To no surprise whatsoever] Material Dialogs
            // has a bug where ugly dividers will show with the RecyclerView even if you disable them.
            // This is why I hate using third party libraries.
            val recycler = RecyclerView(requireContext()).apply {
                adapter = AccentAdapter {
                    if (it.first != accent.first) {
                        SettingsManager.getInstance().accent = it

                        requireActivity().recreate()
                    }

                    this@show.dismiss()
                }

                post {
                    // Combine the width of the recyclerview with the width of an item in order
                    // to center the currently selected accent.
                    val childWidth = getChildAt(0).width / 2

                    (layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(
                            ACCENTS.indexOf(accent),
                            (width / 2) - childWidth
                        )
                }

                layoutManager = LinearLayoutManager(
                    requireContext()
                ).also { it.orientation = LinearLayoutManager.HORIZONTAL }
            }

            customView(view = recycler)

            view.invalidateDividers(showTop = false, showBottom = false)

            negativeButton(android.R.string.cancel)

            show()
        }
    }
}
