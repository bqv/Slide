package ltd.ucode.slide.util

import android.content.Context
import android.widget.Toast

object ExceptionExtensions {
    fun Exception.toast(context: Context,
                        what: String? = null,
                        length: Int = Toast.LENGTH_LONG
    ): Toast {
        return Toast.makeText(context,
            "${what?.let { "$it: " }}${message}",
            length)!!.also(Toast::show)
    }
}
