package ltd.ucode.slide.ui.login

import androidx.lifecycle.ViewModel
import ltd.ucode.slide.repository.AccountRepository
import javax.inject.Inject

class LoginViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
): ViewModel() {
}
