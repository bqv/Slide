package ltd.ucode.slide.table

data class Instance(
    val name: String,
    val version: String,
    val country: String,
    val localPosts: Int? = 0,
    val localComments: Int? = 0,
    val usersTotal: Int? = 0,
    val usersHalfYear: Int? = 0,
    val usersMonthly: Int? = 0,
    val usersWeekly: Int? = 0,
) {
}
