package xyz.untan.gyazauto

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    val TAG: String = BootCompletedReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        Log.d(TAG, "onReceive ACTION_BOOT_COMPLETED")

        val autoUploadEnabled = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(SettingsActivity.KEY_AUTO_UPLOAD, false)
        val readStorageGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (autoUploadEnabled && readStorageGranted) {
            // start ScreenshotObserverService
            val serviceIntent = Intent(context, ScreenshotObserverService::class.java)
            serviceIntent.action = Intent.ACTION_BOOT_COMPLETED
            context.startService(serviceIntent)
        }
    }
}
