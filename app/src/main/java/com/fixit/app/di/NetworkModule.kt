package com.fixit.app.di

import com.fixit.app.data.auth.AuthApi
import com.fixit.app.data.booking.BookingApi
import com.fixit.app.data.category.CategoryApi
import com.fixit.app.data.customer.CustomerApi
import com.fixit.app.data.network.AuthInterceptor
import com.fixit.app.data.payment.PaymentApi
import com.fixit.app.data.provider.ProviderApi
import com.fixit.app.data.review.ReviewApi
import com.fixit.app.data.service.ServiceApi
import com.fixit.app.data.upload.UploadApi
import com.fixit.app.data.wallet.WalletApi
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

    @Provides @Singleton fun authApi(r: Retrofit): AuthApi         = r.create(AuthApi::class.java)
    @Provides @Singleton fun customerApi(r: Retrofit): CustomerApi = r.create(CustomerApi::class.java)
    @Provides @Singleton fun providerApi(r: Retrofit): ProviderApi = r.create(ProviderApi::class.java)
    @Provides @Singleton fun categoryApi(r: Retrofit): CategoryApi = r.create(CategoryApi::class.java)
    @Provides @Singleton fun walletApi(r: Retrofit): WalletApi     = r.create(WalletApi::class.java)
    @Provides @Singleton fun bookingApi(r: Retrofit): BookingApi   = r.create(BookingApi::class.java)
    @Provides @Singleton fun paymentApi(r: Retrofit): PaymentApi   = r.create(PaymentApi::class.java)
    @Provides @Singleton fun reviewApi(r: Retrofit): ReviewApi     = r.create(ReviewApi::class.java)
    @Provides @Singleton fun serviceApi(r: Retrofit): ServiceApi   = r.create(ServiceApi::class.java)
    @Provides @Singleton fun uploadApi(r: Retrofit): UploadApi     = r.create(UploadApi::class.java)
}