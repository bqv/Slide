package ltd.ucode.slide.data.common.partial

import androidx.room.Embedded
import androidx.room.Relation
import ltd.ucode.network.data.ISite
import ltd.ucode.slide.data.common.entity.Group
import ltd.ucode.slide.data.common.entity.Site
import ltd.ucode.slide.data.common.entity.Tagline

data class SiteReference(
    @Embedded val site: Site,
    @Relation(
        parentColumn = "rowid",
        entityColumn = "remote_site_rowid",
    ) val image: Site.Image,
    @Relation(
        parentColumn = "rowid",
        entityColumn = "site_rowid",
    ) val taglineList: List<Tagline>,
    @Relation(
        parentColumn = "rowid",
        entity = Group::class,
        entityColumn = "site_rowid",
    ) val groups: List<Group>,
    // TODO: admins
) : ISite by site {
}
