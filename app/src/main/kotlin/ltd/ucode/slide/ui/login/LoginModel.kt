package ltd.ucode.slide.ui.login

import ltd.ucode.slide.repository.InstanceRepository
import ltd.ucode.slide.data.entity.Instance
import javax.inject.Inject

class LoginModel @Inject constructor(
    private val instanceRepository: InstanceRepository,
) {
    var username: String = ""
    var password: String = ""
    var totp: String? = null
    var instance: String = ""

    suspend fun getInstanceList(): List<Instance> {
        return instanceRepository.fetchInstanceList()
    }

    suspend fun createAccount() {
        return instanceRepository.create(username, password, totp, instance)
    }
}
