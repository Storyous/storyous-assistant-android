package com.storyous.assistant

import retrofit2.http.GET
import retrofit2.http.Url

val firestoreApi = retrofitNewInstance(LOCALHOST).build(FirestoreApi::class)

interface FirestoreApi {

    @GET
    suspend fun loadAccess(
        @Url url: String
    ): AccessResponse
}
