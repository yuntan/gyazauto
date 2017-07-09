package xyz.untan.gyazauto

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.ContextCompat
import android.util.Log

class ScreenshotObserverService : Service() {
    companion object {
        const val NOTIFY_ID = 1
    }
    val TAG: String = ScreenshotObserverService::class.java.simpleName
    //    private FileObserver _observer; // prevent GC
    private var _observer: ContentObserver? = null

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "onCreate")

        initializeObserver()
    }

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action: String? = intent?.action
        Log.d(TAG, "onStartCommand: action: " + action)

        Log.d(TAG, "onStartCommand: start watching")
        //        _observer.startWatching();
        contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, _observer!!)

        startForeground(NOTIFY_ID, buildNotification())

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: stop watching")
        //        _observer.stopWatching();
        contentResolver.unregisterContentObserver(_observer!!)
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
    private fun initializeObserver() {
        _observer = object : ContentObserver(null) {
            // for devices older than api 16
            override fun onChange(selfChange: Boolean) {
                this.onChange(selfChange, null)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                // What is `selfChange` ?
                Log.d(TAG, "onChange: selfChange: $selfChange, uri: $uri")

                if (uri != null && filterUri(uri)) {
                    upload(uri)
                }
            }
        }
    }

    // return true if given uri is in Screenshots folder
    private fun filterUri(uri: Uri): Boolean {
        val screenshotsDirName = "Screenshots"

        // get rows from database
        val cursor = contentResolver
                .query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null) ?: return false

        var ok = false
        if (cursor.moveToFirst()) {
            val path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            Log.d(TAG, "filterUri: uri: $uri, path: $path")
            if (path.contains(screenshotsDirName)) {
                ok = true
            }
        }
        cursor.close()
        return ok
    }

    private fun upload(uri: Uri) {
        // start UploadService
        val intent = Intent(this, UploadService::class.java).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
//            putExtra(UploadService.KEY_TITLE, "") // TODO get foreground app name
        }
        startService(intent)
    }

    private fun buildNotification(): Notification {
        // tap to open settings activity
        val intent = Intent(this, SettingsActivity::class.java)
        val pendingIntent = TaskStackBuilder.create(this)
                .addNextIntent(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(applicationContext)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setContentTitle(getText(R.string.title_notification_stay))
                .setContentText(getText(R.string.desc_notification_stay))
                .setContentIntent(pendingIntent)
                // hide icon on notification bar
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
    }
}
