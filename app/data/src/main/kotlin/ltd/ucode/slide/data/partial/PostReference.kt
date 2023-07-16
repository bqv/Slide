package ltd.ucode.slide.data.partial

import androidx.room.Embedded
import androidx.room.Relation
import ltd.ucode.network.data.ISite
import ltd.ucode.slide.data.entity.Group
import ltd.ucode.slide.data.entity.Language
import ltd.ucode.slide.data.entity.Post
import ltd.ucode.slide.data.entity.Site
import ltd.ucode.slide.data.entity.User

data class PostReference(
    @Embedded val post: Post,
    @Relation(
        parentColumn = "rowid",
        entityColumn = "post_rowid",
    ) val image: Post.Image,
    @Relation(
        parentColumn = "rowid",
        entityColumn = "site_rowid",
    ) val site: Site,
    @Relation(
        parentColumn = "rowid",
        entityColumn = "group_rowid",
    ) val group: Group,
    @Relation(
        parentColumn = "rowid",
        entityColumn = "user_rowid",
    ) val user: User,
    @Relation(
        parentColumn = "rowid",
        entityColumn = "language_rowid",
    ) val language: Language,
) : ISite by site {
}
