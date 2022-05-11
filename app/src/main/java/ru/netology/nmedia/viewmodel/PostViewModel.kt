package ru.netology.nmedia.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.TextItemSeparator
import ru.netology.nmedia.enumeration.SeparatorTimeType
import ru.netology.nmedia.hiltModules.CurrentTime
import ru.netology.nmedia.model.ActionType
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import java.text.SimpleDateFormat
import javax.inject.Inject
import kotlin.random.Random

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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PostRepository,
    private val auth: AppAuth,
    private val currentTime: CurrentTime
) : ViewModel() {

    private val _dataState = MutableLiveData(FeedModelState())

    private val cached
        get() = repository.data.cachedIn(viewModelScope)

    @SuppressLint("SimpleDateFormat")
    val data: Flow<PagingData<FeedItem>> = auth.authStateFlow
        .flatMapLatest {
            cached.map { pagingData ->
                pagingData.insertSeparators(
                    generator = { before, after ->
                        val beforeTime = currentTime.getDaySeparatorType(before?.published?.toLong())
                        val afterTime = currentTime.getDaySeparatorType(after?.published?.toLong())

                        val text = when {
                            beforeTime == SeparatorTimeType.NULL && afterTime == SeparatorTimeType.TODAY ->
                                context.getString(R.string.today)
                            beforeTime == SeparatorTimeType.TODAY && afterTime == SeparatorTimeType.YESTERDAY ->
                                context.getString(R.string.yesterday)
                            beforeTime == SeparatorTimeType.YESTERDAY && afterTime == SeparatorTimeType.MORE_OLD ->
                                context.getString(R.string.more_old)
                            else -> null
                        }
                        text?.let { TextItemSeparator(Random.nextLong(), it) } ?: run { null }
                    }
                )
                    .map { feedItem ->
                        when (feedItem) {
                            is Post -> {
                                feedItem.copy(
                                    published = SimpleDateFormat("dd.MM.yy HH:mm:ss")
                                        .format(feedItem.published.toLong() * 1000L)
                                )
                            }
                            is TextItemSeparator -> feedItem
                        }
                    }
            }
        }

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

    val newerCount: LiveData<Int> = repository.getNewerCount()
        .catch { e -> e.printStackTrace() }
        .asLiveData(Dispatchers.Default)

    fun retryActon(actionType: ActionType, id: Long, load: () -> Unit) {

        when (actionType) {
            ActionType.LOAD -> load
            ActionType.DISLIKE -> dislikeById(id)
            ActionType.LIKE -> likeById(id)
            ActionType.REMOVE -> removeById(id)
            ActionType.SAVE -> save()
            else -> return
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
        edited.value = edited.value?.copy(content = text, published = currentTime.currentTime.toString())
    }

    fun likeById(id: Long) = viewModelScope.launch {
        try {
            val isNotSent = repository.getPostById(id).isNotSent
            Log.i("isNotSent", isNotSent.toString())
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
            val isNotSent = repository.getPostById(id).isNotSent
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

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }

    fun changeAvatar(uri: Uri?) {
        _avatar.value = PhotoModel(uri, uri?.toFile())
    }

    fun updateUser(login: String, pass: String) = viewModelScope.launch {
        try {
            val authState = repository.updateUser(login, pass)
            auth.setAuth(authState.id, authState.token, login)
        } catch (e: Exception) {
            Log.i("updateUser", e.message.toString())
        }
    }

    fun registerUser(login: String, pass: String, name: String) = viewModelScope.launch {
        try {
            when (_avatar.value) {
                noPhoto -> {
                    val authState = repository.registerUser(login, pass, name)
                    auth.setAuth(authState.id, authState.token, login)
                }
                else -> {
                    _avatar.value?.file?.let { file ->
                        val authState = repository.registerWithPhoto(login, pass, name, MediaUpload(file))
                        auth.setAuth(authState.id, authState.token, login)
                    }
                }
            }
        } catch (e: Exception) {
            Log.i("updateUser", e.message.toString())
        }
    }
}

