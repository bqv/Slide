package ltd.ucode.network.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
enum class PostSortType {
    Active,
    Hot,
    MostComments,
    New,
    NewComments,
    Old,
    TopHour,
    TopSixHour,
    TopTwelveHour,
    TopDay,
    TopWeek,
    TopMonth,
    TopThreeMonths,
    TopSixMonths,
    TopNineMonths,
    TopYear,
    TopAll,
    ;

}
