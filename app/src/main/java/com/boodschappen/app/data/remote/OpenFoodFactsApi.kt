package com.boodschappen.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "product_name,product_name_nl,brands,image_front_url,image_front_small_url,image_url,categories_tags,quantity,nutriscore_grade"
    ): ProductResponse

    @GET("cgi/search.pl")
    suspend fun searchByName(
        @Query("search_terms")  query:      String,
        @Query("search_simple") simple:     Int    = 1,
        @Query("action")        action:     String = "process",
        @Query("json")          json:       Int    = 1,
        @Query("page_size")     pageSize:   Int    = 5,
        @Query("fields")        fields:     String = "product_name,product_name_nl,brands,image_front_url,image_url,categories_tags"
    ): SearchResponse
}

data class SearchResponse(
    val products: List<ProductDto> = emptyList()
)

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
    fun getBestImage(): String? = image_front_url?.takeIf { it.isNotBlank() } ?: image_url?.takeIf { it.isNotBlank() }
}
