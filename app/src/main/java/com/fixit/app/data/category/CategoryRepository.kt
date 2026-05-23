package com.fixit.app.data.category

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(private val api: CategoryApi) {
    suspend fun list(): List<CategoryResponse> = api.list()
}