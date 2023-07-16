package ltd.ucode.slide.data.view

import androidx.room.DatabaseView

@DatabaseView("""
    SELECT 1 one
""", viewName = "one")
data class One(
    val one: Int
)
