package ru.netology.nmedia.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Part
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.ActionType
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File

private val empty = Post(
    id = 0,
    authorId = 0,
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

    val data: LiveData<FeedModel> = AppAuth.getInstance()
        .authStateFlow
        .flatMapLatest { (myId, _) ->
            repository.data
                .map { posts ->
                    FeedModel(
                        posts.map { it.copy(ownedByMe = it.authorId == myId) }
                    )
                }
        }.asLiveData(Dispatchers.Default)

    val dataState: LiveData<FeedModelState>
        get() = _dataState

    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val noPhoto = PhotoModel()
    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

    private val _avatar = MutableLiveData(noPhoto)
    val avatar: LiveData<PhotoModel>
        get() = _avatar

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
            edited.value?.let { post ->
                when (_photo.value) {
                    noPhoto -> repository.save(post, false)
                    else -> _photo.value?.file?.let { file ->
                        repository.saveWithAttachment(post, MediaUpload(file), false)
                    }
                }
            }
            _postCreated.postValue(Unit)
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = false, actionType = ActionType.NULL)
        } finally {
            edited.value = empty
            _photo.value = noPhoto
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
            _dataState.value =
                FeedModelState(error = true, actionType = ActionType.LIKE, actionId = id)
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
            _dataState.value =
                FeedModelState(error = true, actionType = ActionType.DISLIKE, actionId = id)
        }
    }

    fun removeById(id: Long) = viewModelScope.launch {
        try {
            repository.removeById(id)
        } catch (e: Exception) {
            _dataState.value =
                FeedModelState(error = true, actionType = ActionType.REMOVE, actionId = id)
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

    val newerCount: LiveData<Int> = data.switchMap {
        repository.getNewerCount(it.posts.firstOrNull()?.id ?: 0L)
            .catch { e -> e.printStackTrace() }
            .asLiveData(Dispatchers.Default)
    }

    fun asVisibleAll() = viewModelScope.launch { repository.asVisibleAll() }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }

    fun refreshPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(refreshing = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }

    fun changeAvatar(uri: Uri?) {
        _avatar.value = PhotoModel(uri, uri?.toFile())
    }

    fun updateUser(login: String, pass: String) = viewModelScope.launch {
        try {
            val authData = repository.updateUser(login, pass)
            AppAuth.getInstance().setAuth(authData.id, authData.token, login)
        } catch (e: Exception) {
            Log.i("updateUser", e.message.toString())
        }
    }

    fun registerUser(login: String, pass: String, name: String) = viewModelScope.launch {
        try {
            when (_avatar.value) {
                noPhoto -> {
                    val authData = repository.registerUser(login, pass, name)
                    AppAuth.getInstance().setAuth(authData.id, authData.token, login)
                }
                else -> {
                    _avatar.value?.file?.let { file ->
                        val authData = repository.registerWithPhoto(login, pass, name, MediaUpload(file))
                        AppAuth.getInstance().setAuth(authData.id, authData.token, login)
                    }
                }
            }
        } catch (e: Exception) {
            Log.i("updateUser", e.message.toString())
        }
    }
}