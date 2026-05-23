package com.fixit.app.data.category

import retrofit2.http.GET

interface CategoryApi {
    @GET("categories/")
    suspend fun list(): List<CategoryResponse>
}