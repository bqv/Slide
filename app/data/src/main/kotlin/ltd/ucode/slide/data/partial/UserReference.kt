package ltd.ucode.slide.data.partial

import androidx.room.Embedded
import androidx.room.Relation
import ltd.ucode.slide.data.entity.Site
import ltd.ucode.slide.data.entity.User

data class UserReference(
    @Embedded val user: User,
) {
    @Relation(parentColumn = "site_rowid", entityColumn = "rowid")
    lateinit var site: Site
}
