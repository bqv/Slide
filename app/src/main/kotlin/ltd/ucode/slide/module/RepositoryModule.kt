package ltd.ucode.slide.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ltd.ucode.slide.data.content.ContentDatabase
import ltd.ucode.slide.data.auth.CredentialDatabase
import ltd.ucode.slide.repository.AccountRepository
import ltd.ucode.slide.repository.CommentRepository
import ltd.ucode.slide.repository.GroupRepository
import ltd.ucode.slide.repository.NetworkRepository
import ltd.ucode.slide.repository.PostRepository
import ltd.ucode.slide.repository.SettingsRepository
import ltd.ucode.slide.repository.SiteRepository
import ltd.ucode.slide.repository.UserRepository
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun providesAccountRepository(@ApplicationContext context: Context,
                                  contentDatabase: ContentDatabase,
                                  credentialDatabase: CredentialDatabase,
    ): AccountRepository =
        AccountRepository(context = context,
            contentDatabase = contentDatabase,
            credentialDatabase = credentialDatabase)

    @Provides
    @Singleton
    fun providesNetworkRepository(@ApplicationContext context: Context,
                                  okHttpClient: OkHttpClient,
                                  @Named("userAgent") userAgent: String,
                                  contentDatabase: ContentDatabase,
                                  credentialDatabase: CredentialDatabase,
    ): NetworkRepository =
        NetworkRepository(context = context,
            okHttpClient = okHttpClient,
            userAgent = userAgent,
            contentDatabase = contentDatabase,
            credentialDatabase = credentialDatabase)

    @Provides
    @Singleton
    fun providesSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context = context)

    @Provides
    @Singleton
    fun providesSiteRepository(@ApplicationContext context: Context,
                               contentDatabase: ContentDatabase,
                               networkRepository: NetworkRepository
    ): SiteRepository =
        SiteRepository(context = context,
            contentDatabase = contentDatabase,
            networkRepository = networkRepository)

    @Provides
    @Singleton
    fun providesCommentRepository(@ApplicationContext context: Context,
                                  contentDatabase: ContentDatabase,
                                  networkRepository: NetworkRepository
    ): CommentRepository =
        CommentRepository(context = context,
            contentDatabase = contentDatabase,
            networkRepository = networkRepository)

    @Provides
    @Singleton
    fun providesGroupRepository(@ApplicationContext context: Context,
                                contentDatabase: ContentDatabase,
                                networkRepository: NetworkRepository
    ): GroupRepository =
        GroupRepository(context = context,
            contentDatabase = contentDatabase,
            networkRepository = networkRepository)

    @Provides
    @Singleton
    fun providesPostRepository(@ApplicationContext context: Context,
                               contentDatabase: ContentDatabase,
                               networkRepository: NetworkRepository
    ): PostRepository =
        PostRepository(context = context,
            contentDatabase = contentDatabase,
            networkRepository = networkRepository)

    @Provides
    @Singleton
    fun providesUserRepository(@ApplicationContext context: Context,
                               contentDatabase: ContentDatabase,
                               networkRepository: NetworkRepository
    ): UserRepository =
        UserRepository(context = context,
            contentDatabase = contentDatabase,
            networkRepository = networkRepository)
}
