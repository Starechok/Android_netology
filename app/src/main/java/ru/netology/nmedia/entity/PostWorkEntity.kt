package ru.netology.nmedia.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity
data class PostWorkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val postId: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String?,
    val content: String,
    val published: Long,
    var likedByMe: Boolean,
    val likes: Long = 0,
    val reposts: Long,
    val videoUrl: String?,
    val wasRead: Boolean,
    @Embedded
    val attachment: AttachmentEmbeddable?,
    val uri: String? = null,
) {
    fun toDto() = Post(
        postId,
        authorId,
        author,
        authorAvatar,
        content,
        published,
        likedByMe,
        likes,
        reposts,
        videoUrl,
        attachment?.toDto()
    )

    companion object {
        fun fromDto(dto: Post, uri: String? = null) =
            PostWorkEntity(
                0L,
                dto.id,
                dto.authorId,
                dto.author,
                dto.authorAvatar,
                dto.content,
                dto.published,
                dto.likedByMe,
                dto.likes,
                dto.reposts,
                dto.videoUrl,
                false,
                AttachmentEmbeddable.fromDto(dto.attachment),
                uri
            )
    }
}
