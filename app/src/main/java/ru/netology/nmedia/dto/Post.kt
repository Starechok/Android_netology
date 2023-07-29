package ru.netology.nmedia.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Post(
    override val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String?,
    val content: String,
    val published: Long,
    var likedByMe: Boolean,
    val likes: Long,
    val reposts: Long,
    val videoUrl: String?,
    val attachment: @RawValue Attachment?,
    val ownedByMe: Boolean = false
) : FeedItem, Parcelable