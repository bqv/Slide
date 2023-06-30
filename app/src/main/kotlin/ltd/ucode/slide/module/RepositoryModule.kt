package ltd.ucode.slide.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import ltd.ucode.slide.repository.CommentRepository
import ltd.ucode.slide.repository.GroupRepository
import ltd.ucode.slide.repository.InstanceRepository
import ltd.ucode.slide.repository.PostRepository
import ltd.ucode.slide.repository.UserRepository

@Module
@InstallIn(ViewModelComponent::class)
object RepositoryModule {
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
    fun providesPostRepository(@ApplicationContext context: Context, instanceRepository: InstanceRepository): PostRepository =
        PostRepository(context = context, instanceRepository = instanceRepository)

    @Provides
    @ViewModelScoped
    fun providesUserRepository(@ApplicationContext context: Context, instanceRepository: InstanceRepository): UserRepository =
        UserRepository(context = context, instanceRepository = instanceRepository)
}
