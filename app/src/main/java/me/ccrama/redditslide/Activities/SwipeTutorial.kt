package me.ccrama.redditslide.Activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import ltd.ucode.slide.R

class SwipeTutorial : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.swipe_tutorial)
        if (intent.hasExtra("subtitle")) {
            (findViewById<View>(R.id.subtitle) as TextView).text =
                intent.getStringExtra("subtitle")
        }
    }
}
