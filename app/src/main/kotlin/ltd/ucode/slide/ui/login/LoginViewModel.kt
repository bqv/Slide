package ltd.ucode.slide.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ltd.ucode.slide.App.Companion.appContext
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.data.common.entity.Site
import ltd.ucode.slide.repository.NetworkRepository
import ltd.ucode.slide.util.ExceptionExtensions.toast
import org.acra.ktx.sendSilentlyWithAcra
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val model: LoginModel,
    private val networkRepository: NetworkRepository,
): ViewModel() {
    private val _instanceList = networkRepository.fetchInstanceList()
        .map { it.associateBy(Site::name) }
    val instanceList: LiveData<Map<String, Site>> = _instanceList
        .asLiveData(context = viewModelScope.coroutineContext)

    fun updateUsername(text: String) { model.username = text.trim() }
    fun updatePassword(text: String) {
        model.password = text.run {
            // https://github.com/LemmyNet/lemmy-ui/issues/1120
            substring(0, Integer.min(length, 60))
        }
    }
    fun updateToken(text: String) {
        model.totp = text.run {
            substring(0, Integer.min(length, 6))
        }.ifBlank { null }
    }
    fun clearToken() {
        model.totp = null
    }
    fun updateInstance(text: String) { model.instance = text.trim() }

    fun doLogin(onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            try {
                model.createAccount()
                Authentication.isLoggedIn = true
                Authentication.name = "${model.username}@${model.instance}"
                onSuccess()
            } catch (e: Exception) {
                e.sendSilentlyWithAcra()
                e.toast(appContext)
                onFailure()
            }
        }
    }

    private suspend fun LoginModel.createAccount() {
        return networkRepository.create(username, password, totp, instance)
    }
}

