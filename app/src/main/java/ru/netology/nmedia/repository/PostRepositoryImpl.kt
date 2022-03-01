package ru.netology.nmedia.repository

import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.Post
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Call

private const val BODY_IS_NULL_TEXT = "body is null"

class PostRepositoryImpl : PostRepository {

    override fun likeById(id: Long, callback: PostRepository.PostCallback<Post>) {
        PostsApi.retrofitService.likeById(id).enqueue(
            object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException(response.message()))
                        return
                    }
                    callback.onSuccess(response.body() ?: throw RuntimeException(BODY_IS_NULL_TEXT))
                }

                override fun onFailure(call: Call<Post>, t: Throwable) {
                    callback.onError(RuntimeException(t.message))
                }
            }
        )
    }

    override fun dislikeById(id: Long, callback: PostRepository.PostCallback<Post>) {
        PostsApi.retrofitService.dislikeById(id).enqueue(
            object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException(response.message()))
                        return
                    }
                    callback.onSuccess(response.body() ?: throw RuntimeException(BODY_IS_NULL_TEXT))
                }

                override fun onFailure(call: Call<Post>, t: Throwable) {
                    callback.onError(RuntimeException(t.message))
                }
            }
        )
    }

    override fun save(post: Post, callback: PostRepository.PostCallback<Unit>) {
        PostsApi.retrofitService.save(post).enqueue(
            object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException(response.message()))
                        return
                    }
                    callback.onSuccess(Unit)
                }

                override fun onFailure(call: Call<Post>, t: Throwable) {
                    callback.onError(RuntimeException(t.message))
                }
            }
        )
    }

    override fun removeById(id: Long, callback: PostRepository.PostCallback<Unit>) {
        PostsApi.retrofitService.removeById(id).enqueue(
            object : Callback<Unit> {
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException(response.message()))
                        return
                    }
                    callback.onSuccess(Unit)
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    callback.onError(RuntimeException(t.message))
                }

            }
        )
    }

    override fun getAll(callback: PostRepository.PostCallback<List<Post>>) {
        PostsApi.retrofitService.getAll().enqueue(
            object : Callback<List<Post>> {
                override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException(response.message()))
                        return
                    }
                    callback.onSuccess(response.body() ?: throw RuntimeException(BODY_IS_NULL_TEXT))
                }

                override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                    callback.onError(RuntimeException(t.message))
                }
            }
        )
    }

}