package com.logicam.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.logicam.BuildConfig
import com.logicam.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings fragment displaying app preferences
 */
class SettingsFragment : PreferenceFragmentCompat() {
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        // Video quality preference with summary
        findPreference<ListPreference>("video_quality")?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        }
        
        // Auto upload preference
        findPreference<Preference>("auto_upload")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                // Enable/disable WiFi-only option based on auto upload
                findPreference<Preference>("wifi_only_upload")?.isEnabled = enabled
                true
            }
        }
        
        // Clear cache button
        findPreference<Preference>("clear_cache")?.setOnPreferenceClickListener {
            clearCache()
            true
        }
        
        // About section with app version
        findPreference<Preference>("about")?.apply {
            summary = getString(R.string.version_format, BuildConfig.VERSION_NAME)
        }
    }
    
    /**
     * Clear app cache directory
     */
    private fun clearCache() {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    context?.cacheDir?.deleteRecursively() ?: false
                } catch (e: Exception) {
                    false
                }
            }
            
            if (success) {
                Toast.makeText(context, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.cache_clear_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
