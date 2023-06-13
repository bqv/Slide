package ltd.ucode.slide

import android.content.SharedPreferences
import ltd.ucode.slide.activity.Slide

object Preferences {
    val settings: SharedPreferences by lazy { getSharedPreferences("SETTINGS") }

    val authentication: SharedPreferences by lazy { getSharedPreferences("AUTHENTICATION") }

    val colours: SharedPreferences by lazy { getSharedPreferences("COLOUR") }

    val appRestart: SharedPreferences by lazy { getSharedPreferences("APP_RESTART") }

    val tags: SharedPreferences by lazy { getSharedPreferences("TAGS") }

    val seen: SharedPreferences by lazy { getSharedPreferences("SEEN") }

    val hidden: SharedPreferences by lazy { getSharedPreferences("HIDDEN") }

    val hiddenPosts: SharedPreferences by lazy { getSharedPreferences("HIDDEN_POSTS") }

    val albums: SharedPreferences by lazy { getSharedPreferences("ALBUMS") }

    val tumblr: SharedPreferences by lazy { getSharedPreferences("TUMBLR") }

    val cache: SharedPreferences by lazy { getSharedPreferences("CACHE") }

    val subscriptions: SharedPreferences by lazy { getSharedPreferences("SUBS") }

    val filters: SharedPreferences by lazy { getSharedPreferences("FILTERS") }

    val upgrade: SharedPreferences by lazy { getSharedPreferences("UPGRADE") }
}

private fun getSharedPreferences(name: String, mode: Int = 0): SharedPreferences {
    return App.appContext.getSharedPreferences(name, mode)
}
