package me.ccrama.redditslide.views

import android.view.MotionEvent
import android.view.View
import androidx.viewpager2.widget.ViewPager2

/**
 * ViewPager2 extensions that allow swiping between pages to be enabled or disabled at runtime.
 */
object ViewPager2Extensions {
    fun ViewPager2.setSwipeLeftOnly(enabled: Boolean) {
        this.setOnTouchListener(SwipeControlTouchListener().apply {
            setSwipeDirection(if (enabled) SwipeDirection.LEFT else SwipeDirection.ALL)
        })
    }

    fun ViewPager2.setSwipingEnabled(enabled: Boolean) {
        this.setOnTouchListener(SwipeControlTouchListener().apply {
            setSwipeDirection(if (enabled) SwipeDirection.ALL else SwipeDirection.NONE)
        })
    }

    fun ViewPager2.disableSwipingUntilRelease() {
        this.setOnTouchListener(SwipeControlTouchListener().apply {
            disableUntilRelease()
        })
    }

    enum class SwipeDirection {
        ALL, // swipe allowed in left and right both directions
        LEFT, // swipe allowed in only Left direction
        RIGHT, // only right
        NONE, // swipe is disabled completely
    }

    private class SwipeControlTouchListener: View.OnTouchListener  {
        private var initialXValue = 0f
        private var direction: SwipeDirection = SwipeDirection.ALL
        private var disabledUntilRelease: Boolean = false

        fun setSwipeDirection(direction: SwipeDirection) {
            this.direction = direction
        }

        fun disableUntilRelease() {
            setSwipeDirection(SwipeDirection.NONE)
            this.disabledUntilRelease = true
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean = !isSwipeAllowed(event)

        private fun isSwipeAllowed(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                if (disabledUntilRelease) {
                    direction = SwipeDirection.ALL
                    disabledUntilRelease = false
                }
            }
            if (direction === SwipeDirection.ALL) return true
            if (direction == SwipeDirection.NONE) //disable any swipe
                return false
            if (event.action == MotionEvent.ACTION_DOWN) {
                initialXValue = event.x
                return true
            }
            if (event.action == MotionEvent.ACTION_MOVE) {
                try {
                    val diffX: Float = event.x - initialXValue
                    if (diffX > 0 && direction == SwipeDirection.RIGHT) {
                        // swipe from left to right detected
                        return false
                    } else if (diffX < 0 && direction == SwipeDirection.LEFT) {
                        // swipe from right to left detected
                        return false
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }
            return true
        }
    }
}
