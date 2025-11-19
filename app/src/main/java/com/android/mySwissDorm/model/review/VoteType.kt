package com.android.mySwissDorm.model.review

/**
 * Represents the type of vote a user has cast on a review.
 *
 * @property UPVOTE The user has upvoted the review.
 * @property DOWNVOTE The user has downvoted the review.
 * @property NONE The user has not voted on the review.
 */
enum class VoteType {
  UPVOTE,
  DOWNVOTE,
  NONE
}

/**
 * Computes the optimistic vote state change for an upvote action.
 *
 * @param currentVote The current vote state.
 * @return A pair of (newVote, scoreDelta) representing the vote change.
 */
fun computeUpvoteChange(currentVote: VoteType): Pair<VoteType, Int> =
    when (currentVote) {
      VoteType.UPVOTE -> VoteType.NONE to -1 // Toggle off
      VoteType.DOWNVOTE -> VoteType.UPVOTE to 2 // Switch from downvote (+1) and add upvote (+1)
      VoteType.NONE -> VoteType.UPVOTE to 1 // Add upvote
    }

/**
 * Computes the optimistic vote state change for a downvote action.
 *
 * @param currentVote The current vote state.
 * @return A pair of (newVote, scoreDelta) representing the vote change.
 */
fun computeDownvoteChange(currentVote: VoteType): Pair<VoteType, Int> =
    when (currentVote) {
      VoteType.DOWNVOTE -> VoteType.NONE to 1 // Toggle off
      VoteType.UPVOTE -> VoteType.DOWNVOTE to -2 // Switch from upvote (-1) and add downvote (-1)
      VoteType.NONE -> VoteType.DOWNVOTE to -1 // Add downvote
    }
