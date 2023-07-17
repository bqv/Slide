package ltd.ucode.slide.shim

import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.PagingData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking

object FlowExtensions {
    fun <T : Any> Flow<PagingData<T>>.items(): List<T> {
        return runBlocking(Dispatchers.Main) {
            AsyncPagingDataDiffer(
                diffCallback = object : DiffUtil.ItemCallback<T>() {
                    override fun areItemsTheSame(oldItem: T, newItem: T)
                        = oldItem == newItem

                    override fun areContentsTheSame(oldItem: T, newItem: T)
                        = oldItem == newItem
                },
                updateCallback = object : ListUpdateCallback {
                    override fun onInserted(position: Int, count: Int) {}
                    override fun onRemoved(position: Int, count: Int) {}
                    override fun onMoved(fromPosition: Int, toPosition: Int) {}
                    override fun onChanged(position: Int, count: Int, payload: Any?) {}
                },
                workerDispatcher = Dispatchers.IO,
            ).also { it.submitData(this@items.single()) }
                .snapshot().items
        }
    }
}
