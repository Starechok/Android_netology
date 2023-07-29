package ru.netology.nmedia.dao

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getPagingSource(): PagingSource<Int, PostEntity>

    @Query("UPDATE PostEntity SET wasRead = 1 WHERE wasRead = 0")
    suspend fun readAll()

    @Query("SELECT COUNT(*) == 0 FROM PostEntity")
    suspend fun isEmpty(): Boolean

    @Query("SELECT COUNT(id) FROM PostEntity WHERE wasRead = 0")
    suspend fun getUnreadCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: List<PostEntity>)

    @Query("DELETE FROM PostEntity WHERE id=:id")
    suspend fun removeById(id: Long)

    @Query(
        """
            UPDATE PostEntity SET
            likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
            likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
            WHERE id = :id
        """
    )
    suspend fun likeById(id: Long)

    @Query(
        """
            UPDATE PostEntity SET
            reposts = reposts + 1
            WHERE id = :id
        """
    )
    suspend fun repostById(id: Long)
}
