package ltd.ucode.slide.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import ltd.ucode.slide.data.entity.Instance

@OptIn(ExperimentalPagingApi::class)
class InstanceMediator() : RemoteMediator<Int, Instance>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Instance>
    ): MediatorResult {
        TODO("Not yet implemented")
    }
}
