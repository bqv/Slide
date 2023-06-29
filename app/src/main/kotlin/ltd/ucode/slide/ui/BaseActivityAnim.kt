package ltd.ucode.slide.ui

import android.os.Bundle
import ltd.ucode.slide.App
import ltd.ucode.slide.R
import me.ccrama.redditslide.SwipeLayout.app.SwipeBackActivityBase

/**
 * Used as the base if an enter or exit animation is required (if the user can swipe out of the
 * activity)
 */
open class BaseActivityAnim : BaseActivity(), SwipeBackActivityBase {
    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (App.peek) {
            overridePendingTransition(R.anim.pop_in, 0)
        } else {
            overridePendingTransition(R.anim.slide_in, 0)
        }
    }
}
