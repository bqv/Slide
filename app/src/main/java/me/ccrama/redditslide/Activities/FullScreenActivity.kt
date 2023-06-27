package me.ccrama.redditslide.Activities

import android.os.Bundle
import android.view.View
import android.view.Window
import ltd.ucode.slide.App
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivity
import me.ccrama.redditslide.Visuals.Palette

/**
 * This Activity allows for fullscreen viewing without the statusbar visible
 */
open class FullScreenActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        //TODO something like this getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
        //   WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (App.peek) {
            overridePendingTransition(R.anim.pop_in, 0)
        } else {
            overridePendingTransition(R.anim.slide_in, 0)
        }
        setRecentBar(null, Palette.getDefaultColor())
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out)
    }

    public override fun onPostCreate(savedInstanceState: Bundle?) {
        try {
            findViewById<View>(android.R.id.content).viewTreeObserver.addOnGlobalLayoutListener {
                //   Blurry.with(FullScreenActivity.this).radius(2).sampling(5).animate().color(Color.parseColor("#99000000")).onto((ViewGroup) findViewById(android.R.id.content));
            }
        } catch (e: Exception) {
        }
        super.onPostCreate(savedInstanceState)
    }
}
