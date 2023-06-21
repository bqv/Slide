package ltd.ucode.slide.data

import kotlinx.datetime.Instant
import net.dean.jraw.models.VoteDirection

interface IItem {
    val permalink: String
    val published: Instant
    val updated: Instant?
    val creator: IUser
    val score: Int
    val myVote: VoteDirection
    val upvoteRatio: Double
    val isSaved: Boolean
}
