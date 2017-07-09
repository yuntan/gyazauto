package xyz.untan.gyazauto

import android.content.Intent
import android.net.Uri
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

internal object GyazoApi {
    val authorizeIntent: Intent
    val api: Api
    val uploadApi: UploadApi
    private val builder: Retrofit.Builder

    init {
        val client = OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) { // debug logging
                val logging = HttpLoggingInterceptor()
                logging.level = HttpLoggingInterceptor.Level.BASIC
                addInterceptor(logging)
            }
        }.build()

        val gson = GsonBuilder()
                // ex. accessToken -> access_token
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()

        builder = Retrofit.Builder()
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))

        api = builder
                .baseUrl(Api.ENDPOINT_URI)
                .build()
                .create(Api::class.java)

        uploadApi = builder
                .baseUrl(UploadApi.ENDPOINT_URI)
                .build()
                .create(UploadApi::class.java)

        val uri = (GyazoApi.Api.ENDPOINT_URI + "oauth/authorize"
                + "?client_id=" + Secrets.CLIENT_ID
                + "&redirect_uri=" + GyazoApi.Api.CALLBACK_URI
                + "&response_type=code")

        authorizeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
    }

    internal interface Api {
        companion object {
            const val ENDPOINT_URI = "https://api.gyazo.com/"
            const val CALLBACK_URI = "gyazauto://authorize"
        }

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
            internal var accessToken: String = ""
            internal var tokenType: String = ""
            internal var scope: String = ""
        }

        class DeleteResponse {
            internal var imageId: String = ""
            internal var type: String = ""
        }
    }

    // https://futurestud.io/tutorials/retrofit-2-how-to-upload-files-to-server
    internal interface UploadApi {
        companion object {
            const val ENDPOINT_URI = "https://upload.gyazo.com/"
        }

        @Multipart
        @POST("api/upload")
        fun upload(
                @Part("access_token") token: RequestBody,
                @Part image: MultipartBody.Part,
                @Part("title") title: RequestBody?,
                @Part("desc") desc: RequestBody?,
                @Part("created_at") createdAt: RequestBody?,
                @Part("collection_id") collectionId: RequestBody?): Call<UploadResponse>

        class UploadResponse {
            internal var imageId: String = ""
            internal var permalinkUrl: String = ""
            internal var thumbUrl: String = ""
            internal var url: String = ""
            internal var type: String = ""
        }
    }
}
