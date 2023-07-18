package ltd.ucode.slide.ui.main

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ltd.ucode.slide.data.entity.Post

class PostAdapter(diffCallback: DiffUtil.ItemCallback<Post>) : PagingDataAdapter<Post, RecyclerView.ViewHolder>(diffCallback) {
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        TODO("Not yet implemented")
    }
}
