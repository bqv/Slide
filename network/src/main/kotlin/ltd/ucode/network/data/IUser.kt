package ltd.ucode.network.data

import kotlinx.datetime.Instant

abstract class IUser {
    abstract val name: String
    abstract val uri: String

    abstract val displayName: String?
    abstract val avatarUrl: String?
    abstract val bannerUrl: String?
    abstract val bio: String?

    abstract val isAdmin: Boolean
    abstract val isBanned: Boolean
    abstract val banExpires: Instant?
    abstract val isBotAccount: Boolean
    abstract val isDeleted: Boolean

    abstract val commentCount: Int
    abstract val commentScore: Int
    abstract val postCount: Int
    abstract val postScore: Int

    abstract val site: ISite

    val fullName: String
        get() = "$name@$domain"
    val domain: String
        get() = site.name
}
