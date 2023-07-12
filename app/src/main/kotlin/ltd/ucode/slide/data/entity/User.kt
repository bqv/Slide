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
import ltd.ucode.network.lemmy.data.type.Person
import ltd.ucode.network.lemmy.data.type.PersonAggregates
import ltd.ucode.network.lemmy.data.type.PersonView
import ltd.ucode.network.data.IUser

@Entity(tableName = "users", indices = [
    Index(value = ["name"], unique = true)
], foreignKeys = [
    ForeignKey(entity = Site::class,
        parentColumns = ["rowid"],
        childColumns = ["instance_rowid"])
])
data class User(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,
        override val name: String,
        @ColumnInfo(name = "instance_rowid") val instanceRowId: Int, // home instance
        @ColumnInfo(name = "person_id") val personId: Int, // home instance

        @ColumnInfo(name = "uri") override val uri: String,

        @ColumnInfo(name = "display_name") override val displayName: String? = null,
        @ColumnInfo(name = "avatar_url") override val avatarUrl: String? = null,
        @ColumnInfo(name = "banner_url") override val bannerUrl: String? = null,
        @ColumnInfo(name = "bio") override val bio: String? = null,

        @ColumnInfo(name = "is_admin") override val isAdmin: Boolean = false,
        @ColumnInfo(name = "is_banned") override val isBanned: Boolean = false,
        @ColumnInfo(name = "ban_expires") override val banExpires: Instant? = null,
        @ColumnInfo(name = "is_bot_account") override val isBotAccount: Boolean = false,
        @ColumnInfo(name = "is_deleted") override val isDeleted: Boolean = false,

        @ColumnInfo(name = "comment_count") override val commentCount: Int = 0,
        @ColumnInfo(name = "comment_score") override val commentScore: Int = 0,
        @ColumnInfo(name = "post_count") override val postCount: Int = 0,
        @ColumnInfo(name = "post_score") override val postScore: Int = 0,

        val discovered: Instant = Clock.System.now(),
        val created: Instant = Instant.DISTANT_PAST,
        val updated: Instant? = null, // home instance
) : IUser() {
    @Ignore override lateinit var site: Site

    @Entity(tableName = "user_images")
    data class Image(
            @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val id: Int = -1,
            @ColumnInfo(name = "user_rowid") val userRowId: Int,
            @ColumnInfo(name = "instance_rowid") val instanceRowId: Int, // imaged instance
            @ColumnInfo(name = "person_id") val personId: Int,

            @ColumnInfo(name = "uri") val uri: String,

            @ColumnInfo(name = "is_admin") val isAdmin: Boolean = false,
            @ColumnInfo(name = "is_banned") val isBanned: Boolean = false,
            @ColumnInfo(name = "ban_expires") val banExpires: Instant? = null,

            val discovered: Instant = Clock.System.now(),
            val updated: Instant? = null, // imaged instance
    )

    companion object {
        fun from(other: PersonView, site: Site): User {
            return User(name = other.person.name,
                instanceRowId = site.rowId,
                personId = other.person.id.id,
                uri = other.person.actorId)
                .copy(other)
        }
    }

    fun copy(other: PersonView): User {
        return copy(name = other.person.name,
            personId = other.person.id.id,
            uri = other.person.actorId)
            .copy(other.person)
            .copy(other.counts)
    }

    fun copy(other: Person): User {
        return copy(
            personId = other.id.id,

            uri = other.actorId,

            displayName = other.displayName,
            avatarUrl = other.avatar,
            bannerUrl = other.banner,
            bio = other.bio,

            isAdmin = other.isAdmin,
            isBanned = other.isBanned,
            banExpires = other.banExpires?.toInstant(TimeZone.UTC),
            isBotAccount = other.isBotAccount,
            isDeleted = other.isDeleted,

            created = other.published.toInstant(TimeZone.UTC),
            updated = other.updated?.toInstant(TimeZone.UTC),
        )
    }

    fun copy(other: PersonAggregates): User {
        return copy(
            commentCount = other.commentCount,
            commentScore = other.commentScore,
            personId = other.personId.id,
            postCount = other.postCount,
            postScore = other.postScore,
        )
    }
}
