package ru.netology.nmedia.repository

import android.util.Log
import androidx.lifecycle.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.Post
import retrofit2.Response
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.errors.ApiException
import ru.netology.nmedia.errors.AppError
import ru.netology.nmedia.errors.NetworkException
import ru.netology.nmedia.errors.UnknownException
import java.io.IOException

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {

    override val data = dao.getVisible().map { list -> list.toDto() }

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val response = PostsApi.retrofitService.getNewer(id)
            if (!response.isSuccessful) {
                throw ApiException(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiException(response.code(), response.message())
            dao.insert(body.toEntity())
            emit(body.size)
        }
    }.catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)

    override suspend fun likeById(id: Long) {
        try {
            val response = PostsApi.retrofitService.likeById(id)
            checkResponse(response)
            val body = response.body() ?: throw ApiException(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body).also { it.isVisible = true })
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw NetworkException
        } catch (e: Exception) {
            throw UnknownException
        }
    }

    override suspend fun dislikeById(id: Long) {
        try {
            val response = PostsApi.retrofitService.dislikeById(id)
            checkResponse(response)
            val body = response.body() ?: throw ApiException(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body).also { it.isVisible = true })
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw NetworkException
        } catch (e: Exception) {
            throw UnknownException
        }
    }

    override suspend fun save(post: Post, retry: Boolean) {
        try {
            var newId = 0L
            if (!retry) {
                newId = dao.insert(PostEntity.fromDto(post.copy(isNotSent = true, isVisible = true)))
            }
            val response = PostsApi.retrofitService.save(post.copy(id = 0))
            checkResponse(response)
            val body = response.body() ?: throw ApiException(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body).also { it.isVisible = true })

            if (!retry) {
                dao.removeById(newId)
            } else {
                dao.removeById(post.id)
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw NetworkException
        } catch (e: Exception) {
            throw UnknownException
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            dao.removeById(id)
            val response = PostsApi.retrofitService.removeById(id)
            checkResponse(response)
        } catch (e: IOException) {
            throw NetworkException
        } catch (e: Exception) {
            throw UnknownException
        }
    }

    override suspend fun getAll() {
        try {
            val response = PostsApi.retrofitService.getAll()
            checkResponse(response)
            val body = response.body() ?: throw ApiException(response.code(), response.message())
            dao.insert(body.toEntity().onEach { it.isVisible = true })
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw NetworkException
        } catch (e: Exception) {
            throw UnknownException
        }
    }

    override suspend fun asVisibleAll() = dao.asVisibleAll()

    override suspend fun uploadMedia(upload: MediaUpload): Media {
        try {
            val media = MultipartBody.Part.createFormData(
                "file", upload.file.name, upload.file.asRequestBody()
            )

            val response = PostsApi.retrofitService.upload(media)
            if (!response.isSuccessful) {
                throw ApiException(response.code(), response.message())
            }

            return response.body() ?: throw ApiException(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkException
        } catch (e: Exception) {
            throw UnknownException
        }
    }

    override suspend fun saveWithAttachment(post: Post, upload: MediaUpload, retry: Boolean) {
        try {
            val media = uploadMedia(upload)
            // TODO: add support for other types
            val postWithAttachment = post.copy(attachment = Attachment(media.id, AttachmentType.IMAGE))
            save(postWithAttachment, retry)
        } catch (e: AppError) {
            throw e
        } catch (e: IOException) {
            throw NetworkException
        } catch (e: Exception) {
            throw UnknownException
        }
    }
}

private fun checkResponse(response: Response<out Any>) {
    if (!response.isSuccessful) {
        throw ApiException(response.code(), response.message())
    }
}