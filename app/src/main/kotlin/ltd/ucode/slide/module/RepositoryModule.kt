package ltd.ucode.slide.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import ltd.ucode.slide.repository.AccountRepository
import ltd.ucode.slide.repository.CommentRepository
import ltd.ucode.slide.repository.GroupRepository
import ltd.ucode.slide.repository.InstanceRepository
import ltd.ucode.slide.repository.PostRepository
import ltd.ucode.slide.repository.SettingsRepository
import ltd.ucode.slide.repository.UserRepository
import okhttp3.OkHttpClient
import javax.inject.Named

@Module
@InstallIn(ViewModelComponent::class)
object RepositoryModule {
    @Provides
    @ViewModelScoped
    fun providesAccountRepository(@ApplicationContext context: Context, instanceRepository: InstanceRepository): AccountRepository =
        AccountRepository(context = context, instanceRepository = instanceRepository)

    @Provides
    @ViewModelScoped
    fun providesCommentRepository(@ApplicationContext context: Context, instanceRepository: InstanceRepository): CommentRepository =
        CommentRepository(context = context, instanceRepository = instanceRepository)

    @Provides
    @ViewModelScoped
    fun providesGroupRepository(@ApplicationContext context: Context, instanceRepository: InstanceRepository): GroupRepository =
        GroupRepository(context = context, instanceRepository = instanceRepository)

    @Provides
    @ViewModelScoped
    fun providesInstanceRepository(@ApplicationContext context: Context, okHttpClient: OkHttpClient, @Named("userAgent") userAgent: String): InstanceRepository =
        InstanceRepository(context = context, okHttpClient = okHttpClient, userAgent = userAgent)

    @Provides
    @ViewModelScoped
    fun providesPostRepository(@ApplicationContext context: Context, instanceRepository: InstanceRepository): PostRepository =
        PostRepository(context = context, instanceRepository = instanceRepository)

    @Provides
    @ViewModelScoped
    fun providesSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context = context)

    @Provides
    @ViewModelScoped
    fun providesUserRepository(@ApplicationContext context: Context, instanceRepository: InstanceRepository): UserRepository =
        UserRepository(context = context, instanceRepository = instanceRepository)
}
