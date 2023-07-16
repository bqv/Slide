package ltd.ucode.slide.data.partial

import androidx.room.Embedded
import androidx.room.Relation
import ltd.ucode.network.data.ISite
import ltd.ucode.slide.data.entity.Site

data class SiteById(
    @Embedded val site: Site,
    @Relation(
        parentColumn = "local_site_rowid",
        entityColumn = "remote_site_rowid",
    ) val image: Site.Image,
) : ISite by site {
}
