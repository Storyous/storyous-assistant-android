package com.storyous.assistant

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.reflect.KClass

const val LOCALHOST = "https://localhost"

fun retrofitNewInstance(url: String): Retrofit {
    val httpClientBuilder = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        )

    return Retrofit.Builder()
        .baseUrl(url)
        .client(httpClientBuilder.build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

fun <T : Any> Retrofit.build(cls: KClass<T>): T {
    return create(cls.java)
}

fun <T : Any> Retrofit.buildError(cls: KClass<T>): Converter<ResponseBody, T> {
    return responseBodyConverter(cls.java, arrayOfNulls<Annotation>(0))
}
