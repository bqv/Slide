package ltd.ucode.slide.data.unified

import androidx.room.Embedded
import androidx.room.Relation
import ltd.ucode.slide.data.entity.Site
import ltd.ucode.slide.data.entity.User

data class UserJoin(
    @Embedded val user: User,
) {
    @Relation(parentColumn = "instance_rowid", entityColumn = "rowid")
    lateinit var site: Site
}
