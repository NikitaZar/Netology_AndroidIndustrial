package ru.netology.nmedia.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.AuthState
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post

interface PostRepository {
    val data: Flow<PagingData<Post>>
    fun getNewerCount(): Flow<Int>
    suspend fun likeById(id: Long)
    suspend fun dislikeById(id: Long)
    suspend fun save(post: Post, retry: Boolean)
    suspend fun removeById(id: Long)
    suspend fun getAll()
    suspend fun asVisibleAll()
    suspend fun uploadMedia(upload: MediaUpload): Media
    suspend fun saveWithAttachment(post: Post, upload: MediaUpload, retry: Boolean)
    suspend fun updateUser(login: String, pass: String): AuthState
    suspend fun registerUser(login: String, pass: String, name: String): AuthState
    suspend fun registerWithPhoto(login: String, pass: String, name: String, upload: MediaUpload): AuthState
    suspend fun getPostById(id: Long): Post
    suspend fun getMaxId(): Long
}
