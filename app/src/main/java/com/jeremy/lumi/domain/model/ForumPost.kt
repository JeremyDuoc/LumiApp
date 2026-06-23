package com.jeremy.lumi.domain.model

data class ForumPost(
    val id: String,
    val question: String,
    val upvotes: Int,
    val repliesCount: Int,
    val isUpvotedByMe: Boolean = false
)