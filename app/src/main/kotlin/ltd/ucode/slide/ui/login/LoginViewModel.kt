package ltd.ucode.slide.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ltd.ucode.slide.App.Companion.appContext
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.table.Instance
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val model: LoginModel
): ViewModel() {
    private val _instanceList = MutableLiveData<Map<String, Instance>>()
    val instanceList: LiveData<Map<String, Instance>> get() = _instanceList

    init {
        fetchInstanceList()
    }

    fun updateUsername(text: String) { model.username = text }
    fun updatePassword(text: String) { model.password = text }
    fun updateInstance(text: String) { model.instance = text }

    fun doLogin(onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            try {
                model.createAccount()
                Authentication.isLoggedIn = true
                Authentication.name = "${model.username}@${model.instance}"
                onSuccess()
            } catch (e: Exception) {
                android.widget.Toast.makeText(appContext,
                    "Login Failed: ${e.message}",
                    android.widget.Toast.LENGTH_LONG)
                onFailure()
            }
        }
    }

    private fun fetchInstanceList() {
        viewModelScope.launch {
            _instanceList.postValue(emptyMap())
            model.getInstanceList().let {
                _instanceList.postValue(it.associateBy { instance: Instance -> instance.name })
            }
        }
    }
}
