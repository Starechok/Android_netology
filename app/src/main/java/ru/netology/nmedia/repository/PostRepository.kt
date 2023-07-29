package ru.netology.nmedia.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post

interface PostRepository {
    val dataPaging: Flow<PagingData<Post>>
    val dataDb: Flow<List<Post>>

    suspend fun getAll()
    suspend fun getLatest()
    fun getNewerCount(id: Long): Flow<Int>
    suspend fun readAll()
    suspend fun getPostById(id: Long)
    suspend fun likeById(id: Long)
    suspend fun dislikeById(id: Long)
    suspend fun removeById(id: Long)
    suspend fun upload(upload: MediaUpload): Media
    suspend fun authentication(login: String, pass: String)
    suspend fun registration(name: String, login: String, pass: String)
    suspend fun repostById(id: Long)
    suspend fun saveWork(post: Post, upload: MediaUpload? = null): Long
    suspend fun processWork(id: Long)

}

