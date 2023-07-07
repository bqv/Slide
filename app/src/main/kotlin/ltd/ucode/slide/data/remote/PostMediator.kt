package ltd.ucode.slide.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import ltd.ucode.slide.data.entity.Post

@OptIn(ExperimentalPagingApi::class)
class PostMediator() : RemoteMediator<Int, Post>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Post>
    ): MediatorResult {
        TODO("Not yet implemented")
    }
}
