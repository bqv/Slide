package ltd.ucode.slide.trait

import kotlinx.datetime.Instant
import ltd.ucode.slide.data.IUser
import net.dean.jraw.models.VoteDirection

interface IVotable {
    val link: String
    val permalink: String
    val published: Instant
    val updated: Instant?
    val creator: IUser
    val score: Int
    val myVote: VoteDirection
    val upvoteRatio: Double
    val isSaved: Boolean
}
