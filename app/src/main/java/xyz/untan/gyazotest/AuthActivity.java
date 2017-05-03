package xyz.untan.gyazotest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class AuthActivity extends AppCompatActivity {
    // SharedPreference util
    private AppStatus _appStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize member
        _appStatus = new AppStatus();

        Button loginView = (Button) findViewById(R.id.button_login);
        loginView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(GyazoApi.getAuthorizeIntent());
                finish(); // make sure don't get back to this activity
            }
        });

        if (_appStatus.accessToken != null && !_appStatus.accessToken.isEmpty()) {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Uri uri = getIntent().getData();
        if (uri != null) { // callback from gyazo
            onIntentCallback(uri);
        }
    }

    private void onIntentCallback(Uri uri) {
        // get authorization code
        String code = uri.getQueryParameter("code");
        if (code == null || code.isEmpty()) {
            showErrorToast();
            return;
        }

        GyazoApi.Api api = GyazoApi.getApi();
        api.token(code).enqueue(new Callback<GyazoApi.Api.TokenResponse>() {
            @Override
            public void onResponse(Call<GyazoApi.Api.TokenResponse> call,
                                   Response<GyazoApi.Api.TokenResponse> response) {
                if (!response.isSuccessful()) {
                    showErrorToast();
                    return;
                }

                _appStatus.accessToken = response.body().accessToken;
                _appStatus.save();

                startActivity(new Intent(AuthActivity.this, SettingsActivity.class));
                finish(); // make sure don't get back to this activity
            }

            @Override
            public void onFailure(Call<GyazoApi.Api.TokenResponse> call, Throwable t) {
                t.printStackTrace();
                showErrorToast();
            }
        });
    }

    private void showErrorToast() {
        // TODO custom style
        Toast.makeText(this, R.string.error_auth_failed, Toast.LENGTH_LONG).show();
    }
}
