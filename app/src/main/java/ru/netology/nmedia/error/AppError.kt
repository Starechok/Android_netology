package ru.netology.nmedia.error

import ru.netology.nmedia.R
import java.io.IOException
import android.database.SQLException

sealed class AppError(var code: String): RuntimeException() {
    companion object {
        fun from(e: Throwable): AppError = when (e) {
            is AppError -> e
            is SQLException -> DbError
            is IOException -> NetworkError
            else -> UnknownError
        }
    }
}

class ApiError(val status: Int, code: String): AppError(code)
object NetworkError : AppError("${R.string.network_error}")
object DbError : AppError("${R.string.error_db}")
object UnknownError: AppError("${R.string.error_unknown}")