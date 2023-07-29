package ru.netology.nmedia.repository

import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.paging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.dao.PostWorkDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.*
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostWorkEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PostRepositoryImpl @Inject constructor(
    private val appDb: AppDb,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val postWorkDao: PostWorkDao,
    private val apiService: ApiService,
    private val auth: AppAuth,
) : PostRepository {

    private companion object {
        private const val PAGE_SIZE = 15
    }

    @OptIn(ExperimentalPagingApi::class)
    override val dataPaging: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(PAGE_SIZE),
        remoteMediator = PostRemoteMediator(apiService, appDb, postDao, postRemoteKeyDao),
        pagingSourceFactory = postDao::getPagingSource,
    ).flow.map { pagingData ->
        pagingData.map(PostEntity::toDto)
    }

    override val dataDb = postDao.getAll()
        .map(List<PostEntity>::toDto)

    override suspend fun getAll() {
        try {
            val response = apiService.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(body.toEntity(wasRead = true))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getLatest() {
        try {
            val response = apiService.getLatest(PAGE_SIZE)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(body.toEntity(wasRead = true))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(120_000L)
            val response = apiService.getNewer(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(body.toEntity(wasRead = false))
            emit(postDao.getUnreadCount())
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)

    override suspend fun readAll() {
        postDao.readAll()
    }

    override suspend fun getPostById(id: Long) {
        try {
            val response = apiService.getPostById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long) {
        postDao.likeById(id)
        try {
            val response = apiService.likeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun dislikeById(id: Long) {
        postDao.likeById(id)
        try {
            val response = apiService.dislikeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        postDao.removeById(id)
        try {
            val response = apiService.removeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun upload(upload: MediaUpload): Media {
        try {
            val media = MultipartBody.Part.createFormData(
                "file", upload.file.name, upload.file.asRequestBody()
            )
            val response = apiService.upload(media)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun authentication(login: String, pass: String) {
        try {
            val response = apiService.updateUser(login, pass)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val authState = response.body() ?: throw ApiError(response.code(), response.message())
            authState.token?.let { auth.setAuth(authState.id, it) }

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun registration(name: String, login: String, pass: String) {
        try {
            val response = apiService.registrationUser(name, login, pass)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val authState = response.body() ?: throw ApiError(response.code(), response.message())
            authState.token?.let { auth.setAuth(authState.id, it) }

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun repostById(id: Long) {
        try {
            return postDao.repostById(id)
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWork(post: Post, upload: MediaUpload?): Long =
        postWorkDao.insert(PostWorkEntity.fromDto(post, upload?.file?.toUri()?.toString()))

    override suspend fun processWork(id: Long) {
        val postToUpload = postWorkDao.getById(id).toDto()
        if (postToUpload.attachment == null) {
            val response = apiService.save(postToUpload)
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(PostEntity.fromDto(body))
        } else {
            val media = upload(MediaUpload(postToUpload.attachment.url.toUri().toFile()))
            // TODO: add support for other types
            val postWithAttachment =
                postToUpload.copy(attachment = Attachment(media.id, AttachmentType.IMAGE))
            val response = apiService.save(postWithAttachment)
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(PostEntity.fromDto(body))
        }
        postWorkDao.removeById(id)
    }
}








