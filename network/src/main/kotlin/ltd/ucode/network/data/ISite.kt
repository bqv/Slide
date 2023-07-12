package ltd.ucode.network.data

interface ISite {
    val name: String
    val software: String?
    val version: String?
    val countryCode: String?
    val localPosts: Int?
    val localComments: Int?
    val usersTotal: Int?
    val usersHalfYear: Int?
    val usersMonthly: Int?
    val usersWeekly: Int?

    val taglines: List<String>

    val uri: String
        get() = "https://$name/"
}
