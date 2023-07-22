package ltd.ucode.slide.data.common.partial

import androidx.room.ColumnInfo
import androidx.room.Ignore

data class SiteImageMetadataPartial(
    @Ignore @ColumnInfo(name = "name") val name: String,
    @Ignore @ColumnInfo(name = "site_id") val siteId: Int,
    @Ignore @ColumnInfo(name = "software") val software: String?,
    @Ignore @ColumnInfo(name = "version") val version: String?,
    @ColumnInfo(name = "rowid") val rowId: Long = 0,
) {
    @ColumnInfo(name = "local_site_rowid") val localSiteRowId: Long = 0
    @ColumnInfo(name = "remote_site_rowid") val remoteSiteRowId: Long = 0
    @ColumnInfo(name = "remote_site_id") val remoteSiteId: Int = siteId

    @get:Ignore val federatedSite: SiteFederatedPartial get() = SiteFederatedPartial(this)

    class SiteFederatedPartial(image: SiteImageMetadataPartial) {
        @ColumnInfo(name = "name") val name: String = image.name
        @ColumnInfo(name = "site_id") val siteId: Int = image.siteId
        @ColumnInfo(name = "software") val software: String? = image.software
        @ColumnInfo(name = "version") val version: String? = image.version
    }
}
