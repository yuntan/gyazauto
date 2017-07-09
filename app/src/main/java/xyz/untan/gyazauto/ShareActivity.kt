package xyz.untan.gyazauto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class ShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var intent = intent
        val action = intent.action
        val type = intent.type
        val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

        if (!(action != null && action == Intent.ACTION_SEND
                && type != null && type.startsWith("image/")
                && imageUri != null)) {
            return
        }

        // start UploadService
        intent = Intent(this, UploadService::class.java)
        intent.type = type
        intent.putExtra(Intent.EXTRA_STREAM, imageUri)

        startService(intent)

        finish() // no need to show activity
    }
}
