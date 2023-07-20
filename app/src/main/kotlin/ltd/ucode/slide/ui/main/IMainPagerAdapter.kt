package ltd.ucode.slide.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

sealed class IMainPagerAdapter(fragmentManager: FragmentManager,
                               lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    abstract val currentFragment: Fragment?

    abstract fun doSetPrimary(obj: Any?, position: Int)
}
