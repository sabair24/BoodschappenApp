package com.boodschappen.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface UpcItemDbApi {
    @GET("prod/trial/lookup")
    suspend fun lookup(@Query("upc") upc: String): UpcItemDbResponse
}

data class UpcItemDbResponse(
    val code: String = "",
    val items: List<UpcItemDbProduct> = emptyList()
)

data class UpcItemDbProduct(
    val title: String = "",
    val brand: String = "",
    val images: List<String> = emptyList()
)
