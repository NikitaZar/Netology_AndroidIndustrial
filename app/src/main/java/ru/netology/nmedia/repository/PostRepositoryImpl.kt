package ru.netology.nmedia.repository

import android.util.Log
import androidx.paging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.*
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.errors.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


const val PAGE_SIZE = 5
const val ENABLE_PLACE_HOLDERS = false


@Singleton
class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
    private val apiService: ApiService,
    mediator: PostRemoteMediator
) : PostRepository {

    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = ENABLE_PLACE_HOLDERS),
        remoteMediator = mediator,
        pagingSourceFactory = { dao.getVisible() },
    ).flow.map { pagingData ->
        pagingData.map(PostEntity::toDto)
    }

    override suspend fun getAll() {
        try {
            val response = apiService.getAll()
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

    override fun getNewerCount(): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val response = apiService.getNewer(getMaxId())
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
            val response = apiService.likeById(id)
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
            val response = apiService.dislikeById(id)
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
            val response = apiService.save(post.copy(id = 0))
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
            val response = apiService.removeById(id)
            checkResponse(response)
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

            val response = apiService.upload(media)
            checkResponse(response)
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

    override suspend fun updateUser(login: String, pass: String): AuthState {
        try {
            val response = apiService.updateUser(login, pass)
            checkResponse(response)
            return response.body() ?: throw ApiException(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkException
        } catch (e: Exception) {
            throw UnknownException
        }
    }

    override suspend fun registerUser(login: String, pass: String, name: String): AuthState {
        try {
            val response = apiService.registerUser(login, pass, name)
            checkResponse(response)
            return response.body() ?: throw ApiException(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkException
        } catch (e: Exception) {
            throw UnknownException
        }
    }

    override suspend fun registerWithPhoto(login: String, pass: String, name: String, upload: MediaUpload): AuthState {
        val media = MultipartBody.Part.createFormData(
            "file", upload.file.name, upload.file.asRequestBody()
        )

        val response = apiService.registerWithPhoto(
            login.toRequestBody(),
            pass.toRequestBody(),
            name.toRequestBody(),
            media
        )

        checkResponse(response)
        return response.body() ?: throw ApiException(response.code(), response.message())
    }

    override suspend fun getPostById(id: Long) = dao.getPostById(id)?.toDto() ?: throw DbError

    override suspend fun getMaxId() = dao.getPostMaxId()?.toDto()?.id ?: throw DbError
}

private fun checkResponse(response: Response<out Any>) {
    if (!response.isSuccessful) {
        throw ApiException(response.code(), response.message())
    }
}