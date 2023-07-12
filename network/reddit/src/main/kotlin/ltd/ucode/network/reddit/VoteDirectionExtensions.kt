package ltd.ucode.network.reddit

import ltd.ucode.network.SingleVote
import net.dean.jraw.models.VoteDirection

object VoteDirectionExtensions {
    fun VoteDirection.asSingleVote(): SingleVote {
        return when (this) {
            VoteDirection.UPVOTE -> SingleVote.UPVOTE
            VoteDirection.DOWNVOTE -> SingleVote.DOWNVOTE
            else -> SingleVote.NOVOTE
        }
    }

    fun SingleVote.asVoteDirection(): VoteDirection {
        return when (this) {
            SingleVote.UPVOTE -> VoteDirection.UPVOTE
            SingleVote.DOWNVOTE -> VoteDirection.DOWNVOTE
            else -> VoteDirection.NO_VOTE
        }
    }
}
