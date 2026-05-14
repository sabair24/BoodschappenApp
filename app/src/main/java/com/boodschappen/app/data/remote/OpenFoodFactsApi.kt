package com.boodschappen.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse

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
    @SerializedName("products") val products: List<ProductDto> = emptyList()
)

data class ProductResponse(
    @SerializedName("status")  val status:  Int        = 0,
    @SerializedName("product") val product: ProductDto? = null
)

data class ProductDto(
    @SerializedName("product_name")           val product_name:           String?       = null,
    @SerializedName("product_name_nl")        val product_name_nl:        String?       = null,
    @SerializedName("brands")                 val brands:                 String?       = null,
    @SerializedName("image_url")              val image_url:              String?       = null,
    @SerializedName("image_front_url")        val image_front_url:        String?       = null,
    @SerializedName("image_front_small_url")  val image_front_small_url:  String?       = null,
    @SerializedName("categories_tags")        val categories_tags:        List<String>? = null,
    @SerializedName("quantity")               val quantity:               String?       = null,
    @SerializedName("nutriscore_grade")       val nutriscore_grade:       String?       = null
) {
    fun getBestName(): String? = product_name_nl?.takeIf { it.isNotBlank() } ?: product_name?.takeIf { it.isNotBlank() }
    fun getBestImage(): String? = image_front_url?.takeIf { it.isNotBlank() } ?: image_url?.takeIf { it.isNotBlank() }
}
