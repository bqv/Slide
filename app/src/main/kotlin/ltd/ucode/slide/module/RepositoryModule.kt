package ltd.ucode.slide.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import ltd.ucode.slide.repository.AccountRepository
import ltd.ucode.slide.repository.InstanceRepository
import okhttp3.OkHttpClient

@Module
@InstallIn(ViewModelComponent::class)
object RepositoryModule {
    @Provides
    @ViewModelScoped
    fun providesAccountRepository(@ApplicationContext context: Context): AccountRepository =
        AccountRepository(context = context)

    @Provides
    @ViewModelScoped
    fun providesInstanceRepository(@ApplicationContext context: Context, okHttpClient: OkHttpClient): InstanceRepository =
        InstanceRepository(context = context, okHttpClient = okHttpClient)
}
