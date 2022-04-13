package ru.netology.nmedia.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String,
    val authorId: Long,
    val authorAvatar: String,
    val content: String,
    val published: String,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val isNotSent: Boolean = false,
    var isVisible: Boolean,
    var ownedByMe: Boolean,
    @Embedded
    var attachment: AttachmentEmbeddable?,
) {
    fun toDto() = Post(id,
        author,
        authorId,
        authorAvatar,
        content,
        published,
        likedByMe,
        likes,
        ownedByMe,
        isNotSent,
        isVisible,
        attachment?.toDto())

    companion object {
        fun fromDto(dto: Post) =
            PostEntity(
                dto.id,
                dto.author,
                dto.authorId,
                dto.authorAvatar,
                dto.content,
                dto.published,
                dto.likedByMe,
                dto.likes,
                dto.isNotSent,
                dto.isVisible,
                dto.ownedByMe,
                AttachmentEmbeddable.fromDto(dto.attachment)
            )
    }
}

fun List<PostEntity>.toDto() = map { it.toDto() }

fun List<Post>.toEntity() = map { PostEntity.fromDto(it) }