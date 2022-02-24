package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun likeById(id: Long, callback: PostCallback<Post>)
    fun dislikeById(id: Long, callback: PostCallback<Post>)
    fun save(post: Post, callback: PostCallback<Unit>)
    fun removeById(id: Long, callback: PostCallback<Unit>)
    fun getAll(callback: PostCallback<List<Post>>)

    interface PostCallback<T> {
        fun onSuccess(type: T)
        fun onError(e: Exception)
    }
}
