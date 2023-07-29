package ru.netology.nmedia.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String?,
    val content: String,
    val published: Long,
    var likedByMe: Boolean,
    val likes: Long,
    val reposts: Long,
    val videoUrl: String?,
    val wasRead: Boolean,
    @Embedded
    var attachment: AttachmentEmbeddable?,
) {
    fun toDto() = Post(
        id,
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
        fun fromDto(dto: Post, wasRead: Boolean = false) =
            PostEntity(
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
                wasRead,
                AttachmentEmbeddable.fromDto(dto.attachment)
            )
    }
}

data class AttachmentEmbeddable(
    var url: String,
    var type: AttachmentType,
) {
    fun toDto() = Attachment(url, type)

    companion object {
        fun fromDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(it.url, it.type)
        }
    }
}

fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)
fun List<Post>.toEntity(wasRead: Boolean = false): List<PostEntity> {
    return map { PostEntity.fromDto(it, wasRead) }
}
