package xyz.untan.gyazauto

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val KEY_AUTO_UPLOAD = "auto_upload_enabled"
        const val KEY_COPY_URL = "copy_url"
        const val PERMISSIONS_REQUEST_CODE = 1
    }
    val TAG: String = SettingsActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate")
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()

        // check storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            Log.d(TAG, "onCreate: storage access DENIED, show request dialog")
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode != PERMISSIONS_REQUEST_CODE) {
            return
        }
        val granted = grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted) {
            Log.d(TAG, "onRequestPermissionsResult: request GRANTED")
            startService(Intent(this, ScreenshotObserverService::class.java))
        } else {
            Log.d(TAG, "onRequestPermissionsResult: request DENIED")
        }
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String) {
        if (key == KEY_AUTO_UPLOAD) {
            val enabled = pref.getBoolean(key, false)
            if (enabled) {
                startService(Intent(this, ScreenshotObserverService::class.java))
            } else {
                stopService(Intent(this, ScreenshotObserverService::class.java))
            }
        }
    }

    class SettingsFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_main)
        }

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences
                    .registerOnSharedPreferenceChangeListener(activity as SettingsActivity)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences
                    .unregisterOnSharedPreferenceChangeListener(activity as SettingsActivity)
        }
    }
}
