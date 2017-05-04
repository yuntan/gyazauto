package xyz.untan.gyazauto;

import android.content.Intent;
import android.net.Uri;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.DELETE;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;


class GyazoApi {

    static Intent getAuthorizeIntent() {
        String uri = GyazoApi.Api.ENDPOINT_URI + "oauth/authorize"
                + "?client_id=" + Secrets.CLIENT_ID
                + "&redirect_uri=" + GyazoApi.Api.CALLBACK_URI
                + "&response_type=code";
        return new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
    }

    static Api getApi() {
        return getBuilder()
                .baseUrl(Api.ENDPOINT_URI)
                .build()
                .create(Api.class);
    }

    static UploadApi getUploadApi() {
        return getBuilder()
                .baseUrl(UploadApi.ENDPOINT_URI)
                .build()
                .create(UploadApi.class);
    }

    private static Retrofit.Builder getBuilder() {
        // debug logging
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            builder.addInterceptor(logging);
        }

        Gson gson = new GsonBuilder()
                // ex. accessToken -> access_token
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        return new Retrofit.Builder()
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create(gson));
    }

    interface Api {
        String ENDPOINT_URI = "https://api.gyazo.com/";
        String CALLBACK_URI = "gyazauto://authorize";

        @POST("oauth/token"
                + "?client_id=" + Secrets.CLIENT_ID
                + "&client_secret=" + Secrets.CLIENT_SECRET
                + "&redirect_uri=" + CALLBACK_URI
                + "&grant_type=authorization_code")
        Call<TokenResponse> token(@Query("code") String code);

        @DELETE("api/images/{image_id}")
        Call<DeleteResponse> delete(
                @Path("image_id") String imageId,
                @Query("access_token") String token);

        class TokenResponse {
            String accessToken;
            String tokenType;
            String scope;
        }

        class DeleteResponse {
            String imageId;
            String type;
        }
    }

    // https://futurestud.io/tutorials/retrofit-2-how-to-upload-files-to-server
    interface UploadApi {
        String ENDPOINT_URI = "https://upload.gyazo.com/";

        @Multipart
        @POST("api/upload")
        Call<UploadResponse> upload(
                @Part("access_token") RequestBody token,
                @Part MultipartBody.Part image,
                @Part("title") RequestBody title,
                @Part("desc") RequestBody desc,
                @Part("created_at") RequestBody createdAt,
                @Part("collection_id") RequestBody collectionId);

        class UploadResponse {
            String imageId;
            String permalinkUrl;
            String thumbUrl;
            String url;
            String type;
        }
    }
}
