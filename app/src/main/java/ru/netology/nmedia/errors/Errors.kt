package ru.netology.nmedia.errors

import android.database.SQLException
import java.io.IOException
import java.lang.RuntimeException

sealed class AppError(val code: Int, val info: String) : RuntimeException(info) {
    companion object {
        fun from(e: Throwable): AppError = when (e) {
            is AppError -> e
            is SQLException -> DbError
            is IOException -> NetworkException
            else -> UnknownException
        }
    }
}

class ApiException(code: Int, message: String) : AppError(code, message)

object NetworkException : AppError(-1, "no_network")
object UnknownException : AppError(-1, "unknown")
object DbError : AppError(-1, "error_db")