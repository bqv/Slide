package ltd.ucode.slide.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import ltd.ucode.slide.data.entity.Site
import ltd.ucode.slide.repository.NetworkRepository
import javax.inject.Inject

class LoginModel @Inject constructor(
) {
    var username: String = ""
    var password: String = ""
    var totp: String? = null
    var instance: String = ""
}
