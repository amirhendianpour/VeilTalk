package com.example.veiltalk.common.util

import com.example.veiltalk.core.network.dto.ErrorResponseDto
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(
    json: Json,
    call: suspend () -> Response<T>
): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Success(Unit as T) // برای پاسخ‌های بدون بدنه
            }
        } else {
            ApiResult.Error(extractErrorMessage(json, response.errorBody()))
        }
    } catch (e: IOException) {
        ApiResult.Error("خطا در اتصال به سرور. اتصال اینترنت را بررسی کنید.")
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "خطای ناشناخته رخ داد.")
    }
}

private fun extractErrorMessage(json: Json, errorBody: ResponseBody?): String {
    return try {
        val raw = errorBody?.string()
        if (raw.isNullOrBlank()) "خطایی رخ داد."
        else json.decodeFromString<ErrorResponseDto>(raw).error ?: "خطایی رخ داد."
    } catch (e: Exception) {
        "خطایی رخ داد."
    }
}