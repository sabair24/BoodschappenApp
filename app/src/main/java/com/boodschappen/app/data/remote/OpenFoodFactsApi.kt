package com.boodschappen.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse
}

data class ProductResponse(
    val status: Int = 0,
    val product: ProductDto? = null
)

data class ProductDto(
    val product_name: String? = null,
    val product_name_nl: String? = null,
    val brands: String? = null,
    val image_url: String? = null,
    val image_front_url: String? = null,
    val image_front_small_url: String? = null,
    val categories_tags: List<String>? = null,
    val quantity: String? = null,
    val nutriscore_grade: String? = null
) {
    fun getBestName(): String? = product_name_nl?.takeIf { it.isNotBlank() } ?: product_name?.takeIf { it.isNotBlank() }
    fun getBestImage(): String? = image_front_url ?: image_url
}
