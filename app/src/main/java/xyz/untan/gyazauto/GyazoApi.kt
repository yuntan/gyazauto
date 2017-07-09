package xyz.untan.gyazauto

import android.content.Intent
import android.net.Uri

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query


internal object GyazoApi {

    val authorizeIntent: Intent
        get() {
            val uri = GyazoApi.Api.ENDPOINT_URI + "oauth/authorize"
            +"?client_id=" + Secrets.CLIENT_ID
            +"&redirect_uri=" + GyazoApi.Api.CALLBACK_URI
            +"&response_type=code"
            return Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        }

    val api: Api
        get() = builder
                .baseUrl(Api.ENDPOINT_URI)
                .build()
                .create(Api::class.java)

    val uploadApi: UploadApi
        get() = builder
                .baseUrl(UploadApi.ENDPOINT_URI)
                .build()
                .create(UploadApi::class.java)

    private // debug logging
            // ex. accessToken -> access_token
    val builder: Retrofit.Builder
        get() {
            val builder = OkHttpClient.Builder()
            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor()
                logging.level = HttpLoggingInterceptor.Level.BASIC
                builder.addInterceptor(logging)
            }

            val gson = GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create()

            return Retrofit.Builder()
                    .client(builder.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
        }

    internal interface Api {

        @POST("oauth/token"
                + "?client_id=" + Secrets.CLIENT_ID
                + "&client_secret=" + Secrets.CLIENT_SECRET
                + "&redirect_uri=" + CALLBACK_URI
                + "&grant_type=authorization_code")
        fun token(@Query("code") code: String): Call<TokenResponse>

        @DELETE("api/images/{image_id}")
        fun delete(
                @Path("image_id") imageId: String,
                @Query("access_token") token: String): Call<DeleteResponse>

        class TokenResponse {
            internal var accessToken: String? = null
            internal var tokenType: String? = null
            internal var scope: String? = null
        }

        class DeleteResponse {
            internal var imageId: String? = null
            internal var type: String? = null
        }

        companion object {
            val ENDPOINT_URI = "https://api.gyazo.com/"
            val CALLBACK_URI = "gyazauto://authorize"
        }
    }

    // https://futurestud.io/tutorials/retrofit-2-how-to-upload-files-to-server
    internal interface UploadApi {

        @Multipart
        @POST("api/upload")
        fun upload(
                @Part("access_token") token: RequestBody,
                @Part image: MultipartBody.Part,
                @Part("title") title: RequestBody,
                @Part("desc") desc: RequestBody,
                @Part("created_at") createdAt: RequestBody,
                @Part("collection_id") collectionId: RequestBody): Call<UploadResponse>

        class UploadResponse {
            internal var imageId: String? = null
            internal var permalinkUrl: String? = null
            internal var thumbUrl: String? = null
            internal var url: String? = null
            internal var type: String? = null
        }

        companion object {
            val ENDPOINT_URI = "https://upload.gyazo.com/"
        }
    }
}
