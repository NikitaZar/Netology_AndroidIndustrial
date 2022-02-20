package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun likeById(id: Long, callback: PostCallback)
    fun dislikeById(id: Long, callback: PostCallback)
    fun save(post: Post, callback: UnitCallback)
    fun removeById(id: Long, callback: UnitCallback)
    fun getAll(callback: GetAllCallback)

    interface GetAllCallback {
        fun onSuccess(posts: List<Post>)
        fun onError(e: Exception)
    }

    interface UnitCallback {
        fun onSuccess()
        fun onError(e: Exception)
    }

    interface PostCallback {
        fun onSuccess(post: Post)
        fun onError(e: Exception)
    }


}
