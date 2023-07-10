package ltd.ucode.slide.data

abstract class IGroup {
    abstract val name: String
    abstract val uri: String

    abstract val title: String?
    abstract val iconUrl: String?
    abstract val bannerUrl: String?
    abstract val description: String?

    abstract val isNsfw: Boolean
    abstract val isHidden: Boolean
    abstract val isRestricted: Boolean
    abstract val isDeleted: Boolean
    abstract val isRemoved: Boolean

    abstract val commentCount: Int
    abstract val postCount: Int
    abstract val subscriberCount: Int
    abstract val hotRank: Int

    abstract val activityDaily: Int
    abstract val activityWeekly: Int
    abstract val activityMonthly: Int
    abstract val activitySemiannual: Int

    abstract val instance: ISite
    abstract val mods: List<IUser>
}
