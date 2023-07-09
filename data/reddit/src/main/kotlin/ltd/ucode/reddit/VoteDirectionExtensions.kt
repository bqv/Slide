package ltd.ucode.reddit

import ltd.ucode.slide.SingleVote
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
