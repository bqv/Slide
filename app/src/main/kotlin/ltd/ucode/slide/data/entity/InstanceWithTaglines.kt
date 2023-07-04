package ltd.ucode.slide.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class InstanceWithTaglines(
    @Embedded val instance: Instance,
    @Relation(parentColumn = "rowid", entityColumn = "instance_id")
    val taglines: List<Tagline>,
)
