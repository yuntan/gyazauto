package xyz.untan.gyazauto

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast

import java.io.File

import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UploadService : Service() {
    private var _appStatus: AppStatus? = null

    override fun onCreate() {
        super.onCreate()

        _appStatus = AppStatus()
    }

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val type = intent.type
        val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        //        String desc = intent.getStringExtra(KEY_DESC);

        // get rows from database
        val cursor = contentResolver
                .query(imageUri, arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED), null, null, null)!!
        cursor.moveToFirst()
        val imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        val added = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))
        cursor.close()

        val file = File(imagePath)
        val image = RequestBody.create(MediaType.parse(type), file)
        val part = MultipartBody.Part.createFormData("imagedata", file.name, image)

        var title: RequestBody? = null
        if (intent.getStringExtra(KEY_TITLE) != null) {
            title = RequestBody.create(MultipartBody.FORM, intent.getStringExtra(KEY_TITLE))
        }
        val token = RequestBody.create(MultipartBody.FORM, _appStatus!!.accessToken!!)
        val createdAt = RequestBody.create(MultipartBody.FORM, added.toString())

        // TODO show progress notification
        val api = GyazoApi.uploadApi
        api.upload(token, part, null!!, null!!, createdAt, null!!).enqueue(callback)

        return Service.START_STICKY
    }

    private // copy URL
    val callback: Callback<GyazoApi.UploadApi.UploadResponse>
        get() = object : Callback<GyazoApi.UploadApi.UploadResponse> {
            override fun onResponse(
                    call: Call<GyazoApi.UploadApi.UploadResponse>,
                    response: Response<GyazoApi.UploadApi.UploadResponse>) {
                if (!response.isSuccessful) {
                    showErrorNotification()
                    return
                }

                showNotification(response.body().getPermalinkUrl())

                if (PreferenceManager.getDefaultSharedPreferences(this@UploadService)
                        .getBoolean(SettingsActivity.getKEY_COPY_URL(), false)) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newUri(contentResolver, "URI",
                            Uri.parse(response.body().getPermalinkUrl()))
                    clipboard.primaryClip = clip

                    Toast.makeText(this@UploadService, R.string.toast_copied, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GyazoApi.UploadApi.UploadResponse>, t: Throwable) {
                showErrorNotification()
            }
        }

    private fun showNotification(permalinkUrl: String) {
        val builder = NotificationCompat.Builder(applicationContext)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getText(R.string.title_notification_upload))
                .setContentText(getText(R.string.desc_notification_upload))
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val openLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(permalinkUrl))

            var sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, permalinkUrl)
            sendIntent.type = "text/plain"
            sendIntent = Intent.createChooser(sendIntent, "title")

            val contentIntent = TaskStackBuilder.create(this)
                    .addNextIntent(openLinkIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            val copyActionIntent = TaskStackBuilder.create(this)
                    .addNextIntent(Intent()) // FIXME intent for copy text
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            val shareActionIntent = TaskStackBuilder.create(this)
                    .addNextIntent(sendIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            val deleteActionIntent = TaskStackBuilder.create(this)
                    .addNextIntent(Intent()) // FIXME intent to call delete api
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

            val copyAction = NotificationCompat.Action.Builder// FIXME change icon
            R.drawable.ic_share_black_24dp, getText(R.string.action_copy), copyActionIntent)
            .build()
            val shareAction = NotificationCompat.Action.Builder(R.drawable.ic_share_black_24dp, getText(R.string.action_share), shareActionIntent)
                    .build()
            val deleteAction = NotificationCompat.Action.Builder// FIXME change icon
            R.drawable.ic_share_black_24dp, getText(R.string.action_delete), deleteActionIntent)
            .build()

            builder.setContentIntent(contentIntent)
                    //                    .addAction(copyAction)
                    //                    .addAction(deleteAction)
                    .addAction(shareAction)
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFY_ID, builder.build())
    }

    private fun showErrorNotification() {
        val builder = NotificationCompat.Builder(applicationContext)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getText(R.string.title_notification_failed))
                .setContentText(getText(R.string.desc_notification_failed)) // TODO tap to retry
                .setAutoCancel(true)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFY_ID, builder.build())

    }

    companion object {
        internal val KEY_TITLE = "title"
        internal val NOTIFY_ID = 1
    }
}
