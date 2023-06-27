package ltd.ucode.slide.ui.login

import android.content.Context
import android.widget.ArrayAdapter

class LoginAdapter : ArrayAdapter<String> {
    private val objects: MutableList<String>

    constructor(context: Context) : this(context, mutableListOf())

    private constructor(context: Context, objects: MutableList<String>)
            : super(context, android.R.layout.select_dialog_item, objects) {
        this.objects = objects
    }

    override fun getCount(): Int = objects.size

    fun addData(list: List<String>) {
        objects.addAll(list.minus(objects.toSet()))
    }
}
