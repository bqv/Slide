package ltd.ucode.slide.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import ltd.ucode.slide.BuildConfig
import ltd.ucode.slide.data.ContentDatabase
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okio.Buffer
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    private val logger: KLogger = KotlinLogging.logger {}

    @Provides
    @Named("buildFlavor")
    fun provideFlavor(): String = BuildConfig.FLAVOR

    @Provides
    @Singleton
    fun providesContentDatabase(@ApplicationContext context: Context): ContentDatabase =
        Room.databaseBuilder(context,
            ContentDatabase::class.java, ContentDatabase.filename)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Named("userAgent")
    fun provideUserAgent(): String = "android:${BuildConfig.APPLICATION_ID}:v${BuildConfig.VERSION_NAME}"

    @Provides
    @Singleton
    fun providesOkHttpClient(@Named("userAgent") userAgent: String): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            fun HttpUrl.safe(): HttpUrl {
                return this.newBuilder().removeAllQueryParameters("auth").build()
            }

            val request = chain.request()
            if (BuildConfig.DEBUG) {
                logger.debug("OkHttp: ${request.method} ${request.url.safe()}")
                val body = request.body?.let { Buffer().also(it::writeTo).readUtf8() }
                if (request.body != null) logger.trace { "  Body: $body" }
            }

            val response = chain.proceed(request.newBuilder()
                .header("User-Agent", userAgent)
                .build())
            if (BuildConfig.DEBUG) {
                logger.debug("OkHttp: ${request.method} ${request.url.safe()} returned ${response.code}")
                val body = response.peekBody(Long.MAX_VALUE).string()
                logger.trace { "  Body: $body" }
            }

            response
        }
        .build()
}
