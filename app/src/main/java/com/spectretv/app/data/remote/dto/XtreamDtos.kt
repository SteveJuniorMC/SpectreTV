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

// VOD (Movies) DTOs
data class XtreamVodStream(
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
    @SerializedName("added")
    val added: String?,
    @SerializedName("category_id")
    val categoryId: String?,
    @SerializedName("container_extension")
    val containerExtension: String?,
    @SerializedName("rating")
    val rating: String?,
    @SerializedName("rating_5based")
    val rating5Based: Double?,
    // Series specific - use Any to handle both Int and String from different providers
    @SerializedName("series_id")
    val seriesIdRaw: Any?,
    @SerializedName("cover")
    val cover: String?,
    @SerializedName("plot")
    val plot: String?,
    @SerializedName("cast")
    val cast: String?,
    @SerializedName("director")
    val director: String?,
    @SerializedName("genre")
    val genre: String?,
    @SerializedName("releaseDate")
    val releaseDate: String?,
    @SerializedName("release_date")
    val releaseDateAlt: String?,
    @SerializedName("last_modified")
    val lastModified: String?,
    // Use Any to handle both String and List<String> from different providers
    @SerializedName("backdrop_path")
    val backdropPathRaw: Any?
) {
    // Helper to get series_id as Int regardless of how provider returns it
    val seriesId: Int?
        get() = when (seriesIdRaw) {
            is Number -> seriesIdRaw.toInt()
            is String -> seriesIdRaw.toIntOrNull()
            else -> null
        }

    // Helper to get backdrop_path as String regardless of format
    val backdropPath: String?
        get() = when (backdropPathRaw) {
            is String -> backdropPathRaw.takeIf { it.isNotBlank() }
            is List<*> -> (backdropPathRaw.firstOrNull() as? String)?.takeIf { it.isNotBlank() }
            else -> null
        }
}

data class XtreamVodInfo(
    @SerializedName("info")
    val info: XtreamMovieInfo?,
    @SerializedName("movie_data")
    val movieData: XtreamMovieData?
)

data class XtreamMovieInfo(
    @SerializedName("movie_image")
    val movieImage: String?,
    @SerializedName("tmdb_id")
    val tmdbId: String?,
    @SerializedName("backdrop_path")
    val backdropPathRaw: Any?,
    @SerializedName("youtube_trailer")
    val youtubeTrailer: String?,
    @SerializedName("genre")
    val genre: String?,
    @SerializedName("plot")
    val plot: String?,
    @SerializedName("cast")
    val cast: String?,
    @SerializedName("rating")
    val rating: String?,
    @SerializedName("director")
    val director: String?,
    @SerializedName("releasedate")
    val releaseDate: String?,
    @SerializedName("duration_secs")
    val durationSecs: Int?,
    @SerializedName("duration")
    val duration: String?
) {
    val backdropPath: String?
        get() = when (backdropPathRaw) {
            is String -> backdropPathRaw.takeIf { it.isNotBlank() }
            is List<*> -> (backdropPathRaw.firstOrNull() as? String)?.takeIf { it.isNotBlank() }
            else -> null
        }
}

data class XtreamMovieData(
    @SerializedName("stream_id")
    val streamId: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("added")
    val added: String?,
    @SerializedName("category_id")
    val categoryId: String?,
    @SerializedName("container_extension")
    val containerExtension: String?
)

// Series DTOs
data class XtreamSeriesInfo(
    @SerializedName("seasons")
    val seasons: List<XtreamSeason>?,
    @SerializedName("info")
    val info: XtreamSeriesDetails?,
    @SerializedName("episodes")
    val episodes: Map<String, List<XtreamEpisode>>?
)

data class XtreamSeriesDetails(
    @SerializedName("name")
    val name: String?,
    @SerializedName("cover")
    val cover: String?,
    @SerializedName("plot")
    val plot: String?,
    @SerializedName("cast")
    val cast: String?,
    @SerializedName("director")
    val director: String?,
    @SerializedName("genre")
    val genre: String?,
    @SerializedName("releaseDate")
    val releaseDate: String?,
    @SerializedName("last_modified")
    val lastModified: String?,
    @SerializedName("rating")
    val rating: String?,
    @SerializedName("rating_5based")
    val rating5Based: Double?,
    @SerializedName("backdrop_path")
    val backdropPathRaw: Any?,
    @SerializedName("youtube_trailer")
    val youtubeTrailer: String?,
    @SerializedName("episode_run_time")
    val episodeRunTime: String?,
    @SerializedName("category_id")
    val categoryId: String?
) {
    val backdropPath: String?
        get() = when (backdropPathRaw) {
            is String -> backdropPathRaw.takeIf { it.isNotBlank() }
            is List<*> -> (backdropPathRaw.firstOrNull() as? String)?.takeIf { it.isNotBlank() }
            else -> null
        }
}

data class XtreamSeason(
    @SerializedName("season_number")
    val seasonNumber: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("episode_count")
    val episodeCount: Int?,
    @SerializedName("cover")
    val cover: String?,
    @SerializedName("cover_big")
    val coverBig: String?
)

data class XtreamEpisode(
    @SerializedName("id")
    val id: String?,
    @SerializedName("episode_num")
    val episodeNum: Int?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("container_extension")
    val containerExtension: String?,
    @SerializedName("info")
    val info: XtreamEpisodeInfo?,
    @SerializedName("custom_sid")
    val customSid: String?,
    @SerializedName("added")
    val added: String?,
    @SerializedName("season")
    val season: Int?,
    @SerializedName("direct_source")
    val directSource: String?
)

data class XtreamEpisodeInfo(
    @SerializedName("movie_image")
    val movieImage: String?,
    @SerializedName("plot")
    val plot: String?,
    @SerializedName("releasedate")
    val releaseDate: String?,
    @SerializedName("rating")
    val rating: Double?,
    @SerializedName("duration_secs")
    val durationSecs: Int?,
    @SerializedName("duration")
    val duration: String?,
    @SerializedName("bitrate")
    val bitrate: Int?
)
