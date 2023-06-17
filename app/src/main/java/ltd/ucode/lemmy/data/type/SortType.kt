package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
enum class SortType {
    Active,
    Hot,
    MostComments,
    New,
    NewComments,
    Old,
    TopAll,
    TopDay,
    TopMonth,
    TopWeek,
    TopYear,
}
