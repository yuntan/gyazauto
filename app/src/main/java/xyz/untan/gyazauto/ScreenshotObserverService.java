package xyz.untan.gyazauto;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class ScreenshotObserverService extends Service {
    static final String TAG = ScreenshotObserverService.class.getSimpleName();
    //    private FileObserver _observer; // prevent GC
    private ContentObserver _observer;
    static final int NOTIFY_ID = 1;


    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");

        initializeObserver();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
            Log.d(TAG, "onStartCommand: action: " + action);
        }

        Log.d(TAG, "onStartCommand: start watching");
//        _observer.startWatching();
        getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, _observer);

        startForeground(NOTIFY_ID, buildNotification());

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: stop watching");
//        _observer.stopWatching();
        getContentResolver().unregisterContentObserver(_observer);
    }

    // not works on api 23, 24
    // https://issuetracker.google.com/issues/37065227
    /*
    private void initializeObserver() {
        final String screenshotsDirName = "Screenshots";

        File picturesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        Log.d(TAG, "initializeObserver: pictures dir: " + picturesDir.getAbsolutePath());
        File screenshotsDir = new File(picturesDir, screenshotsDirName);
        String canonicalPath = null;
        try {
            canonicalPath = screenshotsDir.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Log.d(TAG, "initializeObserver: canonical screenshots dir: " + canonicalPath);
        if (!screenshotsDir.exists()) {
            Log.d(TAG, "initializeObserver: screenshots dir not exists");
        }

        _observer = new FileObserver(canonicalPath) {
            @Override
            public void onEvent(int event, String path) {
                Log.d(TAG, "onEvent: path: " + path);
                switch (event) {
                    case FileObserver.CREATE:
                    case FileObserver.MOVED_TO:
                        Log.d(TAG, "new file created or moved to");
//                        upload(path);
                }
            }
        };
    }
     */

    // https://blog.piasy.com/2016/01/29/Android-Screenshot-Detector/
    private void initializeObserver() {
        _observer = new ContentObserver(null) {
            // for devices older than api 16
            @Override
            public void onChange(boolean selfChange) {
                this.onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                // What is `selfChange` ?
                Log.d(TAG, "onChange: selfChange: " + selfChange + ", uri: " + uri);

                if (filterUri(uri)) {
                    upload(uri);
                }
            }
        };
    }

    // return true if given uri is in Screenshots folder
    private boolean filterUri(Uri uri) {
        final String screenshotsDirName = "Screenshots";

        // get rows from database
        Cursor cursor = getContentResolver()
                .query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
        if (cursor == null) {
            return false;
        }

        boolean ok = false;
        if (cursor.moveToFirst()) {
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            Log.d(TAG, "filterUri: uri: " + uri + ", path: " + path);
            if (path.contains(screenshotsDirName)) {
                ok = true;
            }
        }
        cursor.close();
        return ok;
    }

    private void upload(Uri uri) {
        // start UploadService
        Intent intent = new Intent(this, UploadService.class);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
//        intent.putExtra(UploadService.KEY_TITLE, ""); // TODO get foreground app name

        startService(intent);
    }

    private Notification buildNotification() {
        // tap to open settings activity
        Intent intent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = TaskStackBuilder.create(this)
                .addNextIntent(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setContentTitle(getText(R.string.title_notification_stay))
                .setContentText(getText(R.string.desc_notification_stay))
                .setContentIntent(pendingIntent)
                // hide icon on notification bar
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }
}
