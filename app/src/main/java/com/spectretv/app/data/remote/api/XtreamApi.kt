package com.spectretv.app.data.remote.api

import com.spectretv.app.data.remote.dto.XtreamAuthResponse
import com.spectretv.app.data.remote.dto.XtreamCategory
import com.spectretv.app.data.remote.dto.XtreamStream
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface XtreamApi {

    @GET
    suspend fun authenticate(
        @Url baseUrl: String,
        @Query("username") username: String,
        @Query("password") password: String
    ): XtreamAuthResponse

    @GET
    suspend fun getLiveCategories(
        @Url baseUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<XtreamCategory>

    @GET
    suspend fun getLiveStreams(
        @Url baseUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams"
    ): List<XtreamStream>

    @GET
    suspend fun getLiveStreamsByCategory(
        @Url baseUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String
    ): List<XtreamStream>
}
