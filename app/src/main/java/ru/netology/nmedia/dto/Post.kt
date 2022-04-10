package ru.netology.nmedia.dto

import com.google.gson.annotations.SerializedName

data class Post(
    @SerializedName("id")
    val id: Long,
    @SerializedName("author")
    val author: String,
    @SerializedName("authorId")
    val authorId: Long,
    @SerializedName("authorAvatar")
    val authorAvatar: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("published")
    val published: String,
    @SerializedName("likedByMe")
    val likedByMe: Boolean,
    @SerializedName("likes")
    val likes: Int = 0,
    @SerializedName("ownedByMe")
    val ownedByMe: Boolean = false,
    @Transient
    val isNotSent: Boolean = false,
    @Transient
    var isVisible: Boolean = false,
    @SerializedName("attachment")
    val attachment: Attachment? = null,
)

