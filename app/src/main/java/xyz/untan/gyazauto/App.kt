package xyz.untan.gyazauto

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.util.Log

import com.os.operando.garum.Configuration
import com.os.operando.garum.Garum

class App : Application() {
    val TAG: String = App::class.java.simpleName

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "onCreate")

        PreferenceManager.setDefaultValues(this, R.xml.pref_main, false)

        //        Garum.initialize(getApplicationContext()); // Why is this method not works?
        val builder = Configuration.Builder(applicationContext)
        builder.setModelClasses(AppStatus::class.java)
        Garum.initialize(builder.create(), true)

        val autoUploadEnabled = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(SettingsActivity.KEY_AUTO_UPLOAD, false)
        val readStorageGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (autoUploadEnabled && readStorageGranted) {
            startService(Intent(this, ScreenshotObserverService::class.java))
        }
    }
}
