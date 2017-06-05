package xyz.untan.gyazauto;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = BootCompletedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }
        Log.d(TAG, "onReceive ACTION_BOOT_COMPLETED");

        boolean autoUploadEnabled = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(SettingsActivity.KEY_AUTO_UPLOAD, false),
                readStorageGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;

        if (autoUploadEnabled && readStorageGranted) {
            // start ScreenshotObserverService
            Intent serviceIntent = new Intent(context, ScreenshotObserverService.class);
            serviceIntent.setAction(Intent.ACTION_BOOT_COMPLETED);
            context.startService(serviceIntent);
        }
    }
}
