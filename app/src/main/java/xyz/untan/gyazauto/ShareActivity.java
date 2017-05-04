package xyz.untan.gyazauto;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        if (!(action != null && action.equals(Intent.ACTION_SEND)
                && type != null && type.startsWith("image/")
                && imageUri != null)) {
            return;
        }

        // start UploadService
        intent = new Intent(this, UploadService.class);
        intent.setType(type);
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);

        startService(intent);

        finish(); // no need to show activity
    }
}
