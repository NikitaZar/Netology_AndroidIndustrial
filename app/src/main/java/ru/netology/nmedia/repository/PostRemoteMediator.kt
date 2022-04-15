package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.errors.ApiException
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator @Inject constructor(
    private val service: ApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val db: AppDb
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, PostEntity>): MediatorResult {
        try {
            val response = when (loadType) {
                LoadType.REFRESH -> service.getLatest(state.config.initialLoadSize)
                LoadType.PREPEND -> {
                    return MediatorResult.Success(true)
                }
                LoadType.APPEND -> {
                    val lastId = postRemoteKeyDao.min() ?: return MediatorResult.Success(false)
                    service.getBefore(lastId, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) {
                throw ApiException(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiException(
                response.code(),
                response.message(),
            )

            db.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        when (db.postRemoteKeyDao().isEmpty()) {
                            true -> postRemoteKeyDao.insert(
                                listOf(
                                    PostRemoteKeyEntity(PostRemoteKeyEntity.KeyType.BEFORE, body.last().id),
                                    PostRemoteKeyEntity(PostRemoteKeyEntity.KeyType.AFTER, body.first().id),
                                )
                            )
                            false -> postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(PostRemoteKeyEntity.KeyType.BEFORE, body.last().id),
                            )
                        }
                    }
                    LoadType.APPEND -> {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(PostRemoteKeyEntity.KeyType.BEFORE, body.last().id),
                        )
                    }
                    LoadType.PREPEND -> {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(PostRemoteKeyEntity.KeyType.AFTER, body.first().id),
                        )
                    }
                }
            }
            postDao.insert(body.map(PostEntity::fromDto))
            return MediatorResult.Success(true)
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}