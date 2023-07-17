package ltd.ucode.slide.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ltd.ucode.slide.repository.AccountRepository
import ltd.ucode.slide.repository.CommentRepository
import ltd.ucode.slide.repository.NetworkRepository
import ltd.ucode.slide.repository.PostRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val model: MainModel,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val accountRepository: AccountRepository,
    private val networkRepository: NetworkRepository,
): ViewModel() {
}
