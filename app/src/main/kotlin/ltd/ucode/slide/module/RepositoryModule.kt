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
import ltd.ucode.slide.repository.SettingsRepository
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
    fun providesInstanceRepository(@ApplicationContext context: Context, okHttpClient: OkHttpClient, @Named("userAgent") userAgent: String): InstanceRepository =
        InstanceRepository(context = context, okHttpClient = okHttpClient, userAgent = userAgent)

    @Provides
    @ViewModelScoped
    fun providesSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context = context)
}
