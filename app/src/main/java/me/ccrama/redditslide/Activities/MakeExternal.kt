package me.ccrama.redditslide.Activities

import android.app.Activity
import android.os.Bundle
import ltd.ucode.slide.SettingValues
import java.net.MalformedURLException
import java.net.URL

class MakeExternal : Activity() {
    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        val url = intent.getStringExtra("url")
        if (url != null) {
            try {
                val u = URL(url)
                SettingValues.alwaysExternal = SettingValues.alwaysExternal.orEmpty().plus(u.host)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
        }
        finish()
    }
}
