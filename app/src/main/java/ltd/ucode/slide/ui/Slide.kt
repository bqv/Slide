package ltd.ucode.slide.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import ltd.ucode.slide.ui.main.MainActivity

class Slide : Activity() {
    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        if (!hasStarted) {
            hasStarted = true
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }
        finish()
    }

    companion object {
        @JvmField var hasStarted = false
    }
}
