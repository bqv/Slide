package ltd.ucode.slide.ui.submissionView

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ltd.ucode.slide.data.value.Feed
import ltd.ucode.slide.repository.CommentRepository
import ltd.ucode.slide.repository.PostRepository
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
class SubmissionsViewViewModel @Inject constructor(
    private val model: SubmissionsViewModel,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
): ViewModel() {
    var id: String? = null
    var main by Delegates.notNull<Boolean>()
    var forceLoad by Delegates.notNull<Boolean>()

    var flow = postRepository.getPosts(null, Feed.All)
}
