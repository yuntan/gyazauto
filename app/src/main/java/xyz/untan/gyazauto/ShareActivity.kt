package xyz.untan.gyazauto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class ShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent: Intent? = intent
        if (intent == null) {
            finish()
            return
        }

        val imageUri: Uri? = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

        if (!(intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") ?: false
                && imageUri != null)) {
            return
        }

        // start UploadService
        val newIntent = Intent(this, UploadService::class.java).apply {
            type = intent.type
            putExtra(Intent.EXTRA_STREAM, imageUri)
        }

        startService(newIntent)

        finish() // no need to show activity
    }
}
