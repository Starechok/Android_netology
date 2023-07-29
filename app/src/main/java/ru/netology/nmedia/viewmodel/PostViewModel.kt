package ru.netology.nmedia.viewmodel

import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import androidx.work.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.AdItem
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.util.SingleLiveEvent
import ru.netology.nmedia.work.RemovePostWorker
import ru.netology.nmedia.work.SavePostWorker
import javax.inject.Inject
import kotlin.random.Random

private val empty = Post(
    id = 0,
    authorId = 0,
    author = "",
    authorAvatar = "",
    content = "",
    published = 0,
    likedByMe = false,
    likes = 0,
    reposts = 0,
    videoUrl = "",
    attachment = null
)

private val noPhoto = PhotoModel()

@ExperimentalCoroutinesApi
@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val workManager: WorkManager,
    auth: AppAuth
) : ViewModel() {

    private val cashed: Flow<PagingData<FeedItem>> = repository
        .dataPaging
        .map {
            it.insertSeparators(
                generator = { before, after ->
                    if (before?.id?.rem(5) != 0L) null else
                        AdItem(
                            Random.nextLong(),
                            "https://netology.ru",
                            "figma.jpg",
                        )
                }
            )
        }
        .cachedIn(viewModelScope)

    val dataPaging: Flow<PagingData<FeedItem>> = auth.authStateFlow
        .flatMapLatest { (myId, _) ->
            cashed
                .map {
                it.map { item ->
                    if (item !is Post) item else item.copy(ownedByMe = item.authorId == myId)
                }
            }
        }

    private val dataDb: LiveData<FeedModel> = auth.authStateFlow
        .flatMapLatest { (myId, _) ->
            repository.dataDb
                .map { posts ->
                FeedModel(
                    posts.map { it.copy(ownedByMe = it.authorId == myId) },
                    posts.isEmpty()
                )
            }
        }.asLiveData(Dispatchers.Default)

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

    init {
        loadPosts()
    }

    fun loadPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    val newerCount: LiveData<Int> = dataDb.switchMap {
        repository.getNewerCount(it.posts.firstOrNull()?.id ?: 0L)
            .catch { e -> e.printStackTrace() }
            .asLiveData()
    }

    fun readAll() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.readAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun likeById(id: Long) {
        if (dataDb.value?.posts.orEmpty().filter { it.id == id }.none { it.likedByMe }) {
            viewModelScope.launch {
                try {
                    repository.likeById(id)
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    repository.dislikeById(id)
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
    }

    fun removeById(id: Long) {
        val posts = dataDb.value?.posts.orEmpty()
            .filter { it.id != id }
        dataDb.value?.copy(posts = posts, empty = posts.isEmpty())

        viewModelScope.launch {
            try {
                val data = workDataOf(RemovePostWorker.name to id)
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val request = OneTimeWorkRequestBuilder<RemovePostWorker>()
                    .setInputData(data)
                    .setConstraints(constraints)
                    .build()
                workManager.enqueue(request)

            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun repostById(id: Long) = viewModelScope.launch { repository.repostById(id) }

     fun save() {
        edited.value?.let {
            _postCreated.value = Unit
            viewModelScope.launch {
                try {
                    val id = repository.saveWork(
                        it, _photo.value?.uri?.let { MediaUpload(it.toFile()) }
                    )
                    val data = workDataOf(SavePostWorker.postKey to id)
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                    val request = OneTimeWorkRequestBuilder<SavePostWorker>()
                        .setInputData(data)
                        .setConstraints(constraints)
                        .build()
                    workManager.enqueue(request)

                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    e.printStackTrace()
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
        edited.value = empty
        _photo.value = noPhoto
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

    fun changePhoto(uri: Uri?) {
        _photo.value = PhotoModel(uri)
    }
}