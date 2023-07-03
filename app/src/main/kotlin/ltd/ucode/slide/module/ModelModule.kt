package ltd.ucode.slide.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import ltd.ucode.slide.repository.InstanceRepository
import ltd.ucode.slide.ui.login.LoginModel

@Module
@InstallIn(ViewModelComponent::class)
object ModelModule {
    @Provides
    @ViewModelScoped
    fun providesLoginModel(instanceRepository: InstanceRepository): LoginModel =
        LoginModel(instanceRepository = instanceRepository)
}
