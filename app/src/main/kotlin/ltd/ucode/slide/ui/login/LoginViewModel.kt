package ltd.ucode.slide.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ltd.ucode.slide.repository.AccountRepository
import ltd.ucode.slide.repository.InstanceRepository
import ltd.ucode.slide.table.Instance
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val instanceRepository: InstanceRepository,
): ViewModel() {
    private val _instanceList = MutableLiveData<List<Instance>>()
    val instanceList: LiveData<List<Instance>>
        get() = _instanceList

    init {
        fetchInstanceList()
    }

    private fun fetchInstanceList() {
        viewModelScope.launch {
            _instanceList.postValue(emptyList())
            instanceRepository.getInstanceList().let {
                _instanceList.postValue(it)
            }
        }
    }
}
