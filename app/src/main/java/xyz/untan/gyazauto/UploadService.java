package xyz.untan.gyazauto;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadService extends Service {
    static final String KEY_TITLE = "title";
    static final int NOTIFY_ID = 1;
    private AppStatus _appStatus;

    @Override
    public void onCreate() {
        super.onCreate();

        _appStatus = new AppStatus();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String type = intent.getType();
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
//        String desc = intent.getStringExtra(KEY_DESC);

        // get rows from database
        Cursor cursor = getContentResolver()
                .query(imageUri, new String[]{
                        MediaStore.Images.Media.DATA,
                        MediaStore.Images.Media.DATE_ADDED
                }, null, null, null);
        assert cursor != null;
        cursor.moveToFirst();
        String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        long added = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED));
        cursor.close();

        File file = new File(imagePath);
        RequestBody image = RequestBody.create(MediaType.parse(type), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("imagedata", file.getName(), image);

        RequestBody title = null;
        if (intent.getStringExtra(KEY_TITLE) != null) {
            title = RequestBody.create(MultipartBody.FORM, intent.getStringExtra(KEY_TITLE));
        }
        RequestBody token = RequestBody.create(MultipartBody.FORM, _appStatus.accessToken);
        RequestBody createdAt = RequestBody.create(MultipartBody.FORM, String.valueOf(added));

        // TODO show progress notification
        GyazoApi.UploadApi api = GyazoApi.getUploadApi();
        api.upload(token, part, null, null, createdAt, null).enqueue(getCallback());

        return START_STICKY;
    }

    private Callback<GyazoApi.UploadApi.UploadResponse> getCallback() {
        return new Callback<GyazoApi.UploadApi.UploadResponse>() {
            @Override
            public void onResponse(
                    Call<GyazoApi.UploadApi.UploadResponse> call,
                    Response<GyazoApi.UploadApi.UploadResponse> response) {
                if (!response.isSuccessful()) {
                    showErrorNotification();
                    return;
                }

                showNotification(response.body().permalinkUrl);

                if (PreferenceManager.getDefaultSharedPreferences(UploadService.this)
                        .getBoolean(SettingsActivity.KEY_COPY_URL, false)) {
                    // copy URL
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newUri(getContentResolver(), "URI",
                            Uri.parse(response.body().permalinkUrl));
                    clipboard.setPrimaryClip(clip);

                    Toast.makeText(UploadService.this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GyazoApi.UploadApi.UploadResponse> call, Throwable t) {
                showErrorNotification();
            }
        };
    }

    private void showNotification(String permalinkUrl) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getText(R.string.title_notification_upload))
                .setContentText(getText(R.string.desc_notification_upload))
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Intent openLinkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(permalinkUrl));

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, permalinkUrl);
            sendIntent.setType("text/plain");
            sendIntent = Intent.createChooser(sendIntent, "title");

            PendingIntent contentIntent = TaskStackBuilder.create(this)
                    .addNextIntent(openLinkIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent copyActionIntent = TaskStackBuilder.create(this)
                    .addNextIntent(new Intent()) // FIXME intent for copy text
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent shareActionIntent = TaskStackBuilder.create(this)
                    .addNextIntent(sendIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent deleteActionIntent = TaskStackBuilder.create(this)
                    .addNextIntent(new Intent()) // FIXME intent to call delete api
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Action copyAction = new NotificationCompat.Action
                    // FIXME change icon
                    .Builder(R.drawable.ic_share_black_24dp, getText(R.string.action_copy), copyActionIntent)
                    .build();
            NotificationCompat.Action shareAction = new NotificationCompat.Action
                    .Builder(R.drawable.ic_share_black_24dp, getText(R.string.action_share), shareActionIntent)
                    .build();
            NotificationCompat.Action deleteAction = new NotificationCompat.Action
                    // FIXME change icon
                    .Builder(R.drawable.ic_share_black_24dp, getText(R.string.action_delete), deleteActionIntent)
                    .build();

            builder.setContentIntent(contentIntent)
//                    .addAction(copyAction)
//                    .addAction(deleteAction)
                    .addAction(shareAction);
        }

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFY_ID, builder.build());
    }

    private void showErrorNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getText(R.string.title_notification_failed))
                .setContentText(getText(R.string.desc_notification_failed)) // TODO tap to retry
                .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFY_ID, builder.build());

    }
}
