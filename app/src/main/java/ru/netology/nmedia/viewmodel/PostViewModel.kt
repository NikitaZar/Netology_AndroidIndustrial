package ru.netology.nmedia.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.ActionType
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.SingleLiveEvent

private val empty = Post(
    id = 0,
    content = "",
    author = "",
    authorAvatar = "",
    likedByMe = false,
    likes = 0,
    published = ""
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(application).postDao())

    private val _dataState = MutableLiveData(FeedModelState())
    val data: LiveData<FeedModel> = repository.data.map { FeedModel(it) }
    val dataState: LiveData<FeedModelState>
        get() = _dataState
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true, actionType = ActionType.LOAD)
        }
    }

    fun save() = viewModelScope.launch {
        try {
            edited.value?.let { post -> repository.save(post, false) }
            _postCreated.postValue(Unit)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = false, actionType = ActionType.NULL)
        } finally {
            edited.value = empty
        }
    }

    fun retrySave(post: Post) = viewModelScope.launch {
        try {
            if (post.isNotSent) {
                repository.save(post, true)
            }
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = false, actionType = ActionType.NULL)
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

    fun likeById(id: Long) = viewModelScope.launch {
        try {
            val isNotSent = data.value?.posts?.first { it.id == id }?.isNotSent ?: false
            if (isNotSent) {
                return@launch
            }
            repository.likeById(id)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true, actionType = ActionType.LIKE)
        }
    }

    fun dislikeById(id: Long) = viewModelScope.launch {
        try {
            val isNotSent = data.value?.posts?.first { it.id == id }?.isNotSent ?: false
            if (isNotSent) {
                return@launch
            }
            repository.dislikeById(id)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true, actionType = ActionType.DISLIKE)
        }
    }

    fun removeById(id: Long) = viewModelScope.launch {
        try {
            repository.removeById(id)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true, actionType = ActionType.REMOVE)
        }
    }

    fun retryActon(actionType: ActionType, id: Long) {
        when (actionType) {
            ActionType.LOAD -> loadPosts()
            ActionType.DISLIKE -> dislikeById(id)
            ActionType.LIKE -> likeById(id)
            ActionType.REMOVE -> removeById(id)
            ActionType.SAVE -> save()
            else -> return
        }
    }
}