package ru.netology.nmedia.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.dto.Post
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit


class PostRepositoryImpl : PostRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val typeToken = object : TypeToken<List<Post>>() {}

    companion object {
        private const val BODY_IS_NULL_TEXT = "body is null"
        private const val BASE_URL = "http://10.0.2.2:9999"
        private val JSON_TYPE = "application/json".toMediaType()
    }

    override fun likeById(id: Long, callback: PostRepository.PostCallback) {
        val likesUrl = "${BASE_URL}/api/slow/posts/${id}/likes"
        val request: Request = Request.Builder()
            .post("".toRequestBody())
            .url(likesUrl)
            .build()

        return client.newCall(request)
            .enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        callback.onError(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string() ?: throw throw RuntimeException(BODY_IS_NULL_TEXT)
                        try {
                            callback.onSuccess(gson.fromJson(body, Post::class.java))
                        } catch (e: Exception) {
                            callback.onError(e)
                        }
                    }
                }
            )
    }

    override fun dislikeById(id: Long, callback: PostRepository.PostCallback) {
        val likesUrl = "${BASE_URL}/api/slow/posts/${id}/likes"
        val request: Request = Request.Builder()
            .delete()
            .url(likesUrl)
            .build()

        return client.newCall(request)
            .enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        callback.onError(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string() ?: throw throw RuntimeException(BODY_IS_NULL_TEXT)
                        try {
                            callback.onSuccess(gson.fromJson(body, Post::class.java))
                        } catch (e: Exception) {
                            callback.onError(e)
                        }
                    }
                }
            )
    }

    override fun save(post: Post, callback: PostRepository.UnitCallback) {
        val request: Request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(JSON_TYPE))
            .url("${BASE_URL}/api/slow/posts")
            .build()

        client.newCall(request)
            .enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        callback.onError(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        callback.onSuccess()
                    }
                }
            )
    }

    override fun removeById(id: Long, callback: PostRepository.UnitCallback) {
        val request: Request = Request.Builder()
            .delete()
            .url("${BASE_URL}/api/slow/posts/$id")
            .build()

        client.newCall(request)
            .enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        callback.onError(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                    }
                }
            )
    }

    override fun getAll(callback: PostRepository.GetAllCallback) {
        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        client.newCall(request)
            .enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        callback.onError(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string() ?: throw throw RuntimeException(BODY_IS_NULL_TEXT)
                        try {
                            callback.onSuccess(gson.fromJson(body, typeToken.type))
                        } catch (e: Exception) {
                            callback.onError(e)
                        }
                    }
                }
            )
    }
}