package com.fixit.app.data.service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceRepository @Inject constructor(private val api: ServiceApi) {

    suspend fun mine(): List<ServiceResponse> = api.mine()

    suspend fun create(body: ServiceCreateRequest): ServiceResponse = api.create(body)

    suspend fun update(id: String, body: ServiceUpdateRequest): ServiceResponse =
        api.update(id, body)

    suspend fun delete(id: String) = api.delete(id)
}