package ltd.ucode.slide.ui.login

import ltd.ucode.slide.repository.NetworkRepository
import ltd.ucode.slide.data.entity.Site
import javax.inject.Inject

class LoginModel @Inject constructor(
        private val networkRepository: NetworkRepository,
) {
    var username: String = ""
    var password: String = ""
    var totp: String? = null
    var instance: String = ""

    suspend fun getInstanceList(): List<Site> {
        return networkRepository.fetchInstanceList()
    }

    suspend fun createAccount() {
        return networkRepository.create(username, password, totp, instance)
    }
}
