package ltd.ucode.slide.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ltd.ucode.Util
import ltd.ucode.slide.BuildConfig
import me.ccrama.redditslide.util.LogUtil
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    @Provides
    @Named("buildFlavor")
    fun provideFlavor() = BuildConfig.FLAVOR

    @Provides
    @Singleton
    fun providesOkHttpClient() = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            if (BuildConfig.DEBUG) LogUtil.v("OkHttp: ${request.method} ${request.url}")
            val reqBuilder = request.newBuilder()
                .header("User-Agent", Util.userAgent)
            val response = chain.proceed(reqBuilder.build())
            if (BuildConfig.DEBUG) LogUtil.v("OkHttp: ${request.method} ${request.url} returned ${response.code}")
            response
        }
        .build()
}
