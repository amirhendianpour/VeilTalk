package com.example.veiltalk.feature.chat.data

import com.example.veiltalk.feature.chat.data.dto.ReceiptDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MessageApi {
    @POST("api/messages/receipt")
    suspend fun postReceipt(@Body receipt: ReceiptDto): Response<Unit>
}
