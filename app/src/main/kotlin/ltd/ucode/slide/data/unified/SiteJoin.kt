package ltd.ucode.slide.data.unified

import androidx.room.Embedded
import androidx.room.Relation
import ltd.ucode.slide.data.ISite
import ltd.ucode.slide.data.entity.Site
import ltd.ucode.slide.data.entity.Tagline

class SiteJoin(
    @Embedded val site: Site,
) : ISite by site {
    @Relation(parentColumn = "rowid", entityColumn = "instance_rowid")
    lateinit var taglineList: List<Tagline>
}
