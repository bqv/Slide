package ltd.ucode.slide.data.common.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions.TOKENIZER_UNICODE61
import androidx.room.PrimaryKey

@Entity(tableName = "post_search")
@Fts4(contentEntity = Post::class,
    tokenizer = TOKENIZER_UNICODE61,
    tokenizerArgs = ["tokenchars=._-=#@&"])
data class PostSearch(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int = -1,
    @ColumnInfo(name = "uri") val uri: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "link") val link: String,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String,
)
