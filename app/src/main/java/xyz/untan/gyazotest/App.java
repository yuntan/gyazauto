package xyz.untan.gyazotest;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.os.operando.garum.Configuration;
import com.os.operando.garum.Garum;


public class App extends Application {
    static final String TAG = App.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");

        PreferenceManager.setDefaultValues(this, R.xml.pref_main, false);

//        Garum.initialize(getApplicationContext()); // Why is this method not works?
        Configuration.Builder builder = new Configuration.Builder(getApplicationContext());
        builder.setModelClasses(AppStatus.class);
        Garum.initialize(builder.create(), true);

        boolean autoUploadEnabled = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(SettingsActivity.KEY_AUTO_UPLOAD, false),
                readStorageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;

        if (autoUploadEnabled && readStorageGranted) {
            startService(new Intent(this, ScreenshotObserverService.class));
        }
    }
}
