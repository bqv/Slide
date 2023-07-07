package ltd.ucode.slide.ui.login

import ltd.ucode.slide.repository.InstanceRepository
import ltd.ucode.slide.data.entity.Site
import javax.inject.Inject

class LoginModel @Inject constructor(
    private val instanceRepository: InstanceRepository,
) {
    var username: String = ""
    var password: String = ""
    var totp: String? = null
    var instance: String = ""

    suspend fun getInstanceList(): List<Site> {
        return instanceRepository.fetchInstanceList()
    }

    suspend fun createAccount() {
        return instanceRepository.create(username, password, totp, instance)
    }
}
