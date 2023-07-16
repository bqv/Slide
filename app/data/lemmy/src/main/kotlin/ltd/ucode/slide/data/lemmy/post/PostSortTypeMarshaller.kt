package ltd.ucode.slide.data.lemmy.post

import ltd.ucode.network.lemmy.data.type.PostSortType
import ltd.ucode.slide.data.value.Period
import ltd.ucode.slide.data.value.Sorting

object PostSortTypeMarshaller {
    fun PostSortType.Companion.from(sorting: Sorting, period: Period): PostSortType {
        return PostSortType.Active
    }
}
