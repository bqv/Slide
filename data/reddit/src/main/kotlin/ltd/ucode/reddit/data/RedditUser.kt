package ltd.ucode.reddit.data

import kotlinx.datetime.Instant
import ltd.ucode.slide.data.ISite
import ltd.ucode.slide.data.IUser

class RedditUser(override val name: String) : IUser() {
    override val uri: String
        get() = TODO("Not yet implemented")
    override val displayName: String?
        get() = TODO("Not yet implemented")
    override val avatarUrl: String?
        get() = TODO("Not yet implemented")
    override val bannerUrl: String?
        get() = TODO("Not yet implemented")
    override val bio: String?
        get() = TODO("Not yet implemented")
    override val isAdmin: Boolean
        get() = TODO("Not yet implemented")
    override val isBanned: Boolean
        get() = TODO("Not yet implemented")
    override val banExpires: Instant?
        get() = TODO("Not yet implemented")
    override val isBotAccount: Boolean
        get() = TODO("Not yet implemented")
    override val isDeleted: Boolean
        get() = TODO("Not yet implemented")
    override val commentCount: Int
        get() = TODO("Not yet implemented")
    override val commentScore: Int
        get() = TODO("Not yet implemented")
    override val postCount: Int
        get() = TODO("Not yet implemented")
    override val postScore: Int
        get() = TODO("Not yet implemented")
    override val site: ISite
        get() = TODO("Not yet implemented")
}
