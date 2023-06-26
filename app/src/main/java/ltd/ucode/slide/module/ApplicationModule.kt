package ltd.ucode.slide.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ltd.ucode.Util
import ltd.ucode.slide.BuildConfig
import ltd.ucode.slide.repository.AccountRepository
import ltd.ucode.slide.repository.InstanceRepository
import me.ccrama.redditslide.util.LogUtil
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationModule {
    @Provides
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

    @Provides
    @Singleton
    fun providesAccountRepository(context: Context): AccountRepository =
        AccountRepository(context = context)

    @Provides
    @Singleton
    fun providesInstanceRepository(context: Context, okHttpClient: OkHttpClient): InstanceRepository =
        InstanceRepository(context = context, okHttpClient = okHttpClient)
}
