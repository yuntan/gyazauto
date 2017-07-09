package xyz.untan.gyazauto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthActivity : AppCompatActivity() {
    // SharedPreference util
    private var _appStatus: AppStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize member
        _appStatus = AppStatus()

        val loginView = findViewById(R.id.button_login) as Button
        loginView.setOnClickListener {
            startActivity(GyazoApi.authorizeIntent)
            finish() // make sure don't get back to this activity
        }

        if (!_appStatus!!.accessToken.isNullOrEmpty()) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()

        val uri: Uri? = intent.data
        if (uri != null) { // callback from gyazo
            onIntentCallback(uri)
        }
    }

    private fun onIntentCallback(uri: Uri) {
        // get authorization code
        val code = uri.getQueryParameter("code")
        if (code.isEmpty()) {
            showErrorToast()
            return
        }

        val api = GyazoApi.api
        api.token(code).enqueue(object : Callback<GyazoApi.Api.TokenResponse> {
            override fun onResponse(call: Call<GyazoApi.Api.TokenResponse>,
                                    response: Response<GyazoApi.Api.TokenResponse>) {
                if (!response.isSuccessful) {
                    showErrorToast()
                    return
                }

                _appStatus!!.accessToken = response.body().accessToken
                _appStatus!!.save()

                startActivity(Intent(this@AuthActivity, SettingsActivity::class.java))
                finish() // make sure don't get back to this activity
            }

            override fun onFailure(call: Call<GyazoApi.Api.TokenResponse>, t: Throwable) {
                t.printStackTrace()
                showErrorToast()
            }
        })
    }

    private fun showErrorToast() {
        // TODO custom style
        Toast.makeText(this, R.string.error_auth_failed, Toast.LENGTH_LONG).show()
    }
}
