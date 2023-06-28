package ltd.ucode.slide.ui.login

import ltd.ucode.slide.repository.AccountRepository
import ltd.ucode.slide.repository.InstanceRepository
import ltd.ucode.slide.table.Instance
import javax.inject.Inject

class LoginModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val instanceRepository: InstanceRepository,
) {
    var username: String = ""
    var password: String = ""
    var instance: String = ""

    suspend fun getInstanceList(): List<Instance> {
        return instanceRepository.fetchInstanceList()
    }

    suspend fun createAccount() {
        return accountRepository.create(username, password, instance)
    }
}
