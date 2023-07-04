package ltd.ucode.slide.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ltd.ucode.slide.repository.AccountRepository
import ltd.ucode.slide.repository.CommentRepository
import ltd.ucode.slide.repository.GroupRepository
import ltd.ucode.slide.repository.InstanceRepository
import ltd.ucode.slide.repository.PostRepository
import ltd.ucode.slide.repository.SettingsRepository
import ltd.ucode.slide.repository.UserRepository
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun providesAccountRepository(@ApplicationContext context: Context): AccountRepository =
        AccountRepository(context = context)

    @Provides
    @Singleton
    fun providesInstanceRepository(@ApplicationContext context: Context, okHttpClient: OkHttpClient, @Named("userAgent") userAgent: String): InstanceRepository =
        InstanceRepository(context = context, okHttpClient = okHttpClient, userAgent = userAgent)

    @Provides
    @Singleton
    fun providesSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context = context)

    @Provides
    @Singleton
    fun providesCommentRepository(@ApplicationContext context: Context, instanceRepository: InstanceRepository): CommentRepository =
        CommentRepository(context = context, instanceRepository = instanceRepository)

    @Provides
    @Singleton
    fun providesGroupRepository(@ApplicationContext context: Context, instanceRepository: InstanceRepository): GroupRepository =
        GroupRepository(context = context, instanceRepository = instanceRepository)

    @Provides
    @Singleton
    fun providesPostRepository(@ApplicationContext context: Context, instanceRepository: InstanceRepository): PostRepository =
        PostRepository(context = context, instanceRepository = instanceRepository)

    @Provides
    @Singleton
    fun providesUserRepository(@ApplicationContext context: Context, instanceRepository: InstanceRepository): UserRepository =
        UserRepository(context = context, instanceRepository = instanceRepository)
}
