package ltd.ucode.network.data

import ltd.ucode.network.trait.IVotable

abstract class IComment : IVotable {
    abstract val rowId: Int
    abstract val commentId: Int
    abstract val parentRowId: Int?
    abstract val parentId: Int?

    abstract val content: String

    abstract val isRemoved: Boolean
    abstract val isDeleted: Boolean
    abstract val isDistinguished: Boolean

    abstract val groupName: String
}
