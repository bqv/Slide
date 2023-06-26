package ltd.ucode.slide.table

data class Instance(
    val name: String,
    val version: String,
    val country: String,
    val localPosts: Int?,
    val localComments: Int?,
    val usersTotal: Int?,
    val usersHalfYear: Int?,
    val usersMonthly: Int?,
    val usersWeekly: Int?,
) {
}
