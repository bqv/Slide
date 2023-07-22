package ltd.ucode.slide.data.common.view

import androidx.room.DatabaseView

@DatabaseView("""
    SELECT 1 one
""", viewName = "_one")
data class One(
    val one: Int
)
