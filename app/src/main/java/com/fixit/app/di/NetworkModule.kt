package com.fixit.app.di

import com.fixit.app.data.auth.AuthApi
import com.fixit.app.data.customer.CustomerApi
import com.fixit.app.data.network.AuthInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Point to your dev backend. Use 10.0.2.2 if running on the Android emulator
    // against a server on the host machine. Move to BuildConfig per variant later.
    private const val BASE_URL = "http://10.0.2.2:8000/"

    @Provides @Singleton
    fun moshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Provides @Singleton
    fun okHttp(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

    @Provides @Singleton
    fun retrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides @Singleton fun authApi(r: Retrofit): AuthApi = r.create(AuthApi::class.java)
    @Provides @Singleton fun customerApi(r: Retrofit): CustomerApi = r.create(CustomerApi::class.java)

    @Provides @Singleton fun providerApi(r: Retrofit): com.fixit.app.data.provider.ProviderApi =
        r.create(com.fixit.app.data.provider.ProviderApi::class.java)

    @Provides @Singleton fun categoryApi(r: Retrofit): com.fixit.app.data.category.CategoryApi =
        r.create(com.fixit.app.data.category.CategoryApi::class.java)
}