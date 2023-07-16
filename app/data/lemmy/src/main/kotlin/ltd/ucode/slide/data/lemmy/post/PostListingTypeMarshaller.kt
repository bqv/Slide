package ltd.ucode.slide.data.lemmy.post

import ltd.ucode.network.lemmy.data.type.PostListingType
import ltd.ucode.slide.data.value.Feed

object PostListingTypeMarshaller {
    fun PostListingType.Companion.from(feed: Feed): PostListingType {
        return PostListingType.All
    }
}
