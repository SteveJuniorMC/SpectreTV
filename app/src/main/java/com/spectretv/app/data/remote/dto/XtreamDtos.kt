package com.spectretv.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class XtreamAuthResponse(
    @SerializedName("user_info")
    val userInfo: XtreamUserInfo?,
    @SerializedName("server_info")
    val serverInfo: XtreamServerInfo?
)

data class XtreamUserInfo(
    @SerializedName("username")
    val username: String?,
    @SerializedName("password")
    val password: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("exp_date")
    val expDate: String?,
    @SerializedName("is_trial")
    val isTrial: String?,
    @SerializedName("active_cons")
    val activeCons: String?,
    @SerializedName("max_connections")
    val maxConnections: String?
)

data class XtreamServerInfo(
    @SerializedName("url")
    val url: String?,
    @SerializedName("port")
    val port: String?,
    @SerializedName("https_port")
    val httpsPort: String?,
    @SerializedName("server_protocol")
    val serverProtocol: String?,
    @SerializedName("rtmp_port")
    val rtmpPort: String?,
    @SerializedName("timezone")
    val timezone: String?
)

data class XtreamCategory(
    @SerializedName("category_id")
    val categoryId: String?,
    @SerializedName("category_name")
    val categoryName: String?,
    @SerializedName("parent_id")
    val parentId: Int?
)

data class XtreamStream(
    @SerializedName("num")
    val num: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("stream_type")
    val streamType: String?,
    @SerializedName("stream_id")
    val streamId: Int?,
    @SerializedName("stream_icon")
    val streamIcon: String?,
    @SerializedName("epg_channel_id")
    val epgChannelId: String?,
    @SerializedName("added")
    val added: String?,
    @SerializedName("category_id")
    val categoryId: String?,
    @SerializedName("custom_sid")
    val customSid: String?,
    @SerializedName("tv_archive")
    val tvArchive: Int?,
    @SerializedName("direct_source")
    val directSource: String?,
    @SerializedName("tv_archive_duration")
    val tvArchiveDuration: Int?
)
