package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.SingleLiveEvent

private val empty = Post(
    id = 0,
    content = "",
    author = "",
    likedByMe = false,
    likes = 0,
    published = ""
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PostRepository = PostRepositoryImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() {
        _data.value = FeedModel(loading = true)

        repository.getAll(
            object : PostRepository.GetAllCallback {
                override fun onSuccess(posts: List<Post>) {
                    _data.postValue((FeedModel(posts = posts)))
                }

                override fun onError(e: Exception) {
                    _data.postValue(FeedModel(error = true))
                }
            }
        )
    }

    fun save() {
        edited.value?.let { post ->
            repository.save(post,
                object : PostRepository.UnitCallback {
                    override fun onSuccess() {
                        _postCreated.postValue(Unit)
                    }

                    override fun onError(e: Exception) {
                        _data.postValue(FeedModel(error = true))
                    }
                }
            )
            edited.value = empty
        }
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun likeById(id: Long) {
        repository.likeById(id,
            object : PostRepository.PostCallback {
                override fun onSuccess(post: Post) {
                    postToFeedModel(post, id)
                }

                override fun onError(e: Exception) {
                    _data.postValue(FeedModel(error = true))
                }
            })

    }

    fun dislikeById(id: Long) {
        repository.dislikeById(id,
            object : PostRepository.PostCallback {
                override fun onSuccess(post: Post) {
                    postToFeedModel(post, id)
                }

                override fun onError(e: Exception) {
                    _data.postValue(FeedModel(error = true))
                }
            }
        )
    }

    fun removeById(id: Long) {
        // Оптимистичная модель
        val old = _data.value?.posts.orEmpty()
        _data.postValue(
            _data.value?.copy(posts = _data.value?.posts.orEmpty()
                .filter { it.id != id }
            )
        )

        repository.removeById(id,
            object : PostRepository.UnitCallback {
                override fun onSuccess() {}

                override fun onError(e: Exception) {
                    _data.postValue(_data.value?.copy(posts = old))
                }
            }
        )
    }

    private fun postToFeedModel(post: Post, id: Long) {
        val posts = _data.value?.posts.orEmpty().map { if (it.id == id) post else it }
        FeedModel(posts = posts)
        _data.postValue(FeedModel(posts = posts))
    }
}


