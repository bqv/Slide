package ltd.ucode.slide.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ltd.ucode.slide.BuildConfig
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    @Provides
    @Named("buildFlavor")
    fun provideFlavor(): String = BuildConfig.FLAVOR

    @Provides
    @Named("userAgent")
    fun provideUserAgent(): String = "android:${BuildConfig.APPLICATION_ID}:v${BuildConfig.VERSION_NAME}"

    @Provides
    @Singleton
    fun providesOkHttpClient(@Named("userAgent") userAgent: String): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            if (BuildConfig.DEBUG) me.ccrama.redditslide.util.LogUtil.v("OkHttp: ${request.method} ${request.url}")
            val reqBuilder = request.newBuilder()
                .header("User-Agent", userAgent)
            val response = chain.proceed(reqBuilder.build())
            if (BuildConfig.DEBUG) me.ccrama.redditslide.util.LogUtil.v("OkHttp: ${request.method} ${request.url} returned ${response.code}")
            response
        }
        .build()
}
