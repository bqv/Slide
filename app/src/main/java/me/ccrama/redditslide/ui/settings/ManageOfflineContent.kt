package me.ccrama.redditslide.ui.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import ltd.ucode.slide.R
import ltd.ucode.slide.repository.CommentRepository
import ltd.ucode.slide.repository.PostRepository
import ltd.ucode.slide.ui.BaseActivityAnim
import javax.inject.Inject

@AndroidEntryPoint
class ManageOfflineContent : BaseActivityAnim() {
    var fragment = ManageOfflineContentFragment(this)

    @Inject
    lateinit var postRepository: PostRepository
    @Inject
    lateinit var commentRepository: CommentRepository

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_manage_history)
        setupAppBar(R.id.toolbar, R.string.manage_offline_content, true, true)
        (findViewById<View>(R.id.manage_history) as ViewGroup).addView(
            layoutInflater.inflate(R.layout.activity_manage_history_child, null)
        )
        fragment.Bind()
    }
}
