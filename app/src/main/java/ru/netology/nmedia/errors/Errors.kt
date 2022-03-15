package ru.netology.nmedia.errors

import java.lang.RuntimeException

sealed class AppError(val code: Int, val info: String) : RuntimeException(info)

class ApiException(code: Int, message: String) : AppError(code, message)

object NetworkException : AppError(-1, "no_network")
object UnknownException : AppError(-1, "unknown")