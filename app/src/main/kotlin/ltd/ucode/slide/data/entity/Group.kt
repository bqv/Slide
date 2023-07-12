package ltd.ucode.slide.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ltd.ucode.network.lemmy.data.type.Community
import ltd.ucode.network.lemmy.data.type.CommunityAggregates
import ltd.ucode.network.lemmy.data.type.CommunityView
import ltd.ucode.network.data.IGroup

@Entity(tableName = "groups", indices = [
    Index(value = ["name"], unique = true)
], foreignKeys = [
    ForeignKey(entity = Site::class,
        parentColumns = ["rowid"],
        childColumns = ["instance_rowid"])
])
data class Group(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,
    override val name: String,
    @ColumnInfo(name = "instance_rowid") val instanceRowId: Int, // home instance
    @ColumnInfo(name = "group_id") val groupId: Int, // home instance

    @ColumnInfo(name = "uri") override val uri: String,

    @ColumnInfo(name = "title") override val title: String? = null,
    @ColumnInfo(name = "icon_url") override val iconUrl: String? = null,
    @ColumnInfo(name = "banner_url") override val bannerUrl: String? = null,
    @ColumnInfo(name = "description") override val description: String? = null,

    @ColumnInfo(name = "is_nsfw") override val isNsfw: Boolean = false,
    @ColumnInfo(name = "is_hidden") override val isHidden: Boolean = false,
    @ColumnInfo(name = "is_restricted") override val isRestricted: Boolean = false,
    @ColumnInfo(name = "is_deleted") override val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_removed") override val isRemoved: Boolean = false,

    @ColumnInfo(name = "comment_count") override val commentCount: Int = 0,
    @ColumnInfo(name = "post_count") override val postCount: Int = 0,
    @ColumnInfo(name = "subscriber_score") override val subscriberCount: Int = 0,
    @ColumnInfo(name = "hot_rank") override val hotRank: Int = 0,

    @ColumnInfo(name = "activity_daily") override val activityDaily: Int = 0,
    @ColumnInfo(name = "activity_weekly") override val activityWeekly: Int = 0,
    @ColumnInfo(name = "activity_monthly") override val activityMonthly: Int = 0,
    @ColumnInfo(name = "activity_semiannual") override val activitySemiannual: Int = 0,

    val discovered: Instant = Clock.System.now(),
    val created: Instant = Instant.DISTANT_PAST,
    val updated: Instant? = null, // home instance
) : IGroup() {
    @Ignore override lateinit var site: Site
    @Ignore override lateinit var mods: MutableList<out User>
    @Ignore lateinit var subscriptions: MutableList<out GroupSubscription>

    @Entity(tableName = "group_images")
    data class Image(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val id: Int = -1,
        @ColumnInfo(name = "group_rowid") val groupRowId: Int,
        @ColumnInfo(name = "instance_rowid") val instanceRowId: Int, // imaged instance
        @ColumnInfo(name = "group_id") val groupId: Int,

        @ColumnInfo(name = "is_hidden") val isHidden: Boolean = false,
        @ColumnInfo(name = "is_removed") val isRemoved: Boolean = false,

        @ColumnInfo(name = "comment_count") val commentCount: Int = 0,
        @ColumnInfo(name = "post_count") val postCount: Int = 0,
        @ColumnInfo(name = "subscriber_score") val subscriberCount: Int = 0,
        @ColumnInfo(name = "hot_rank") val hotRank: Int = 0,

        @ColumnInfo(name = "activity_daily") val activityDaily: Int = 0,
        @ColumnInfo(name = "activity_weekly") val activityWeekly: Int = 0,
        @ColumnInfo(name = "activity_monthly") val activityMonthly: Int = 0,
        @ColumnInfo(name = "activity_semiannual") val activitySemiannual: Int = 0,

        val discovered: Instant = Clock.System.now(),
        val updated: Instant? = null, // imaged instance
    )

    companion object {
        fun from(other: CommunityView, site: Site): Group {
            return Group(name = other.community.name,
                instanceRowId = site.rowId,
                groupId = other.community.id.id,
                uri = other.community.actorId)
                .copy(other)
        }
    }

    fun copy(other: CommunityView): Group {
        return copy(
            name = other.community.name,
            groupId = other.community.id.id,
            uri = other.community.actorId
        )
    }

    fun copy(other: Community): Group {
        return copy(
            groupId = other.id.id,

            uri = other.actorId,

            title = other.title,
            iconUrl = other.icon,
            bannerUrl = other.banner,
            description = other.description,

            isNsfw = other.isNsfw,
            isHidden = other.isHidden,
            isRestricted = other.isPostingRestricted,
            isDeleted = other.isDeleted,
            isRemoved = other.isRemoved,

            created = other.published.toInstant(TimeZone.UTC),
            updated = other.updated?.toInstant(TimeZone.UTC),
        )
    }

    fun copy(other: CommunityAggregates): Group {
        return copy(
            commentCount = other.comments,
            postCount = other.posts,
            subscriberCount = other.subscribers,
            hotRank = other.hotRank,

            activityDaily = other.usersActiveDay,
            activityWeekly = other.usersActiveWeek,
            activityMonthly = other.usersActiveMonth,
            activitySemiannual = other.usersActiveHalfYear,
        )
    }
}
