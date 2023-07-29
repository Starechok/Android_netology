package ru.netology.nmedia.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import javax.inject.Inject
import javax.inject.Singleton

@HiltWorker
class RemovePostWorker @AssistedInject constructor(
    @Assisted appllicationContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: PostRepository
) : CoroutineWorker(appllicationContext, params) {
    companion object {
        const val name = "ru.netology.work.RemovePostsWorker"
    }

    override suspend fun doWork(): Result {
        val id = inputData.getLong(name, 0L)
        if (id == 0L) {
            return Result.failure()
        }
        return try {
            repository.removeById(id)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
