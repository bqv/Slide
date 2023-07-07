package ltd.ucode.slide.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ltd.ucode.slide.App.Companion.appContext
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.data.entity.Site
import ltd.ucode.util.ExceptionExtensions.toast
import org.acra.ktx.sendSilentlyWithAcra
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val model: LoginModel,
): ViewModel() {
    private val _instanceList = MutableLiveData<Map<String, Site>>()
    val instanceList: LiveData<Map<String, Site>> get() = _instanceList

    init {
        fetchInstanceList()
    }

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

    private fun fetchInstanceList() {
        viewModelScope.launch {
            _instanceList.postValue(emptyMap())
            model.getInstanceList().let {
                _instanceList.postValue(it.associateBy { instance: Site -> instance.name })
            }
        }
    }
}

