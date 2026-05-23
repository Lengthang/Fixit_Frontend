package com.fixit.app.data.customer

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(private val api: CustomerApi) {
    suspend fun me(): UserResponse = api.me()
    suspend fun update(name: String?, email: String?): UserResponse =
        api.updateMe(CustomerUpdateRequest(name = name, email = email))
}