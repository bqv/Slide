package ltd.ucode.network.trait

import kotlinx.datetime.Instant
import ltd.ucode.network.SingleVote
import ltd.ucode.network.data.IUser

interface IVotable {
    val uri: String
    val discovered: Instant
    val created: Instant
    val updated: Instant?
    val user: IUser
    val score: Int
        get() = upvotes - downvotes
    val myVote: SingleVote
    val upvoteRatio: Double
        get() = (100.0 * upvotes) / downvotes
    val upvotes: Int
    val downvotes: Int
}

/*
 up votes = u
 down votes = d

 score = s = u - d
 ratio = r = u / d

 u = s + d = r * d
 d = u - s = u / r

 u = s + (u / r) =
 u = r * (u - s) =

 d = (r * d) - s =
 d = (s + d) / r =
*/
