package ltd.ucode.slide.util

import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

object ContextExtensions {
    private val logger: KLogger = KotlinLogging.logger {}

    val Context.lifecycleScope: LifecycleCoroutineScope get() = lifecycleOwner.lifecycle.coroutineScope
    val Context.lifecycleOwner: LifecycleOwner get() = getLifecycleOwnerImpl()!!

    private tailrec fun Context?.getLifecycleOwnerImpl(): LifecycleOwner? {
        logger.trace { "getLifecycleOwner: at ${this?.javaClass?.canonicalName}" }
        return when (this) {
            is ContextWrapper ->
                if (this is AppCompatActivity) this
                else this.baseContext.getLifecycleOwnerImpl()

            else -> null
        }
    }
}
