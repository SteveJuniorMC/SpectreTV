package com.spectretv.app.data.remote.api

import com.spectretv.app.data.remote.dto.XtreamAuthResponse
import com.spectretv.app.data.remote.dto.XtreamCategory
import com.spectretv.app.data.remote.dto.XtreamSeriesInfo
import com.spectretv.app.data.remote.dto.XtreamStream
import com.spectretv.app.data.remote.dto.XtreamVodInfo
import com.spectretv.app.data.remote.dto.XtreamVodStream
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

    // Live TV
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

    // VOD (Movies)
    @GET
    suspend fun getVodCategories(
        @Url baseUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): List<XtreamCategory>

    @GET
    suspend fun getVodStreams(
        @Url baseUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams"
    ): List<XtreamVodStream>

    @GET
    suspend fun getVodStreamsByCategory(
        @Url baseUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String
    ): List<XtreamVodStream>

    @GET
    suspend fun getVodInfo(
        @Url baseUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_info",
        @Query("vod_id") vodId: String
    ): XtreamVodInfo

    // Series
    @GET
    suspend fun getSeriesCategories(
        @Url baseUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): List<XtreamCategory>

    @GET
    suspend fun getSeries(
        @Url baseUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series"
    ): List<XtreamVodStream>

    @GET
    suspend fun getSeriesByCategory(
        @Url baseUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: String
    ): List<XtreamVodStream>

    @GET
    suspend fun getSeriesInfo(
        @Url baseUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: String
    ): XtreamSeriesInfo
}
