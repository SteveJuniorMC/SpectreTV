package com.spectretv.app.data.remote

import com.spectretv.app.data.remote.api.XtreamApi
import com.spectretv.app.data.remote.dto.XtreamCategory
import com.spectretv.app.domain.model.Channel
import com.spectretv.app.domain.model.Episode
import com.spectretv.app.domain.model.Movie
import com.spectretv.app.domain.model.Series
import com.spectretv.app.domain.model.Source
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XtreamClient @Inject constructor(
    private val xtreamApi: XtreamApi
) {
    suspend fun authenticate(source: Source): Boolean {
        return try {
            val baseUrl = buildPlayerApiUrl(source.serverUrl!!)
            val response = xtreamApi.authenticate(
                baseUrl = baseUrl,
                username = source.username!!,
                password = source.password!!
            )
            response.userInfo?.status == "Active"
        } catch (e: Exception) {
            false
        }
    }

    // Live TV
    suspend fun getCategories(source: Source): List<XtreamCategory> {
        val baseUrl = buildPlayerApiUrl(source.serverUrl!!)
        return xtreamApi.getLiveCategories(
            baseUrl = baseUrl,
            username = source.username!!,
            password = source.password!!
        )
    }

    suspend fun getChannels(source: Source): List<Channel> {
        val baseUrl = buildPlayerApiUrl(source.serverUrl!!)
        val categories = getCategories(source)
        val categoryMap = categories.associate {
            (it.categoryId ?: "") to (it.categoryName ?: "Uncategorized")
        }

        val streams = xtreamApi.getLiveStreams(
            baseUrl = baseUrl,
            username = source.username!!,
            password = source.password!!
        )

        return streams.mapNotNull { stream ->
            stream.streamId?.let { streamId ->
                Channel(
                    id = "${source.id}_xtream_$streamId",
                    name = stream.name ?: "Unknown",
                    streamUrl = buildLiveStreamUrl(source, streamId),
                    logoUrl = stream.streamIcon?.takeIf { it.isNotBlank() },
                    group = categoryMap[stream.categoryId] ?: "Uncategorized",
                    epgId = stream.epgChannelId,
                    sourceId = source.id
                )
            }
        }
    }

    // VOD (Movies)
    suspend fun getVodCategories(source: Source): List<XtreamCategory> {
        val baseUrl = buildPlayerApiUrl(source.serverUrl!!)
        return try {
            xtreamApi.getVodCategories(
                baseUrl = baseUrl,
                username = source.username!!,
                password = source.password!!
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMovies(source: Source): List<Movie> {
        val baseUrl = buildPlayerApiUrl(source.serverUrl!!)
        val categories = getVodCategories(source)
        val categoryMap = categories.associate {
            (it.categoryId ?: "") to (it.categoryName ?: "Uncategorized")
        }

        return try {
            val streams = xtreamApi.getVodStreams(
                baseUrl = baseUrl,
                username = source.username!!,
                password = source.password!!
            )

            streams.mapNotNull { stream ->
                stream.streamId?.let { streamId ->
                    val extension = stream.containerExtension ?: "mp4"
                    Movie(
                        id = "${source.id}_vod_$streamId",
                        name = stream.name ?: "Unknown",
                        streamUrl = buildVodStreamUrl(source, streamId, extension),
                        posterUrl = stream.streamIcon?.takeIf { it.isNotBlank() },
                        backdropUrl = stream.backdropPath?.firstOrNull(),
                        plot = stream.plot,
                        genre = categoryMap[stream.categoryId] ?: "Uncategorized",
                        year = stream.releaseDate?.take(4),
                        rating = stream.rating,
                        sourceId = source.id,
                        containerExtension = extension
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Series
    suspend fun getSeriesCategories(source: Source): List<XtreamCategory> {
        val baseUrl = buildPlayerApiUrl(source.serverUrl!!)
        return try {
            xtreamApi.getSeriesCategories(
                baseUrl = baseUrl,
                username = source.username!!,
                password = source.password!!
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeries(source: Source): List<Series> {
        val baseUrl = buildPlayerApiUrl(source.serverUrl!!)
        val categories = getSeriesCategories(source)
        val categoryMap = categories.associate {
            (it.categoryId ?: "") to (it.categoryName ?: "Uncategorized")
        }

        return try {
            val seriesList = xtreamApi.getSeries(
                baseUrl = baseUrl,
                username = source.username!!,
                password = source.password!!
            )

            seriesList.mapNotNull { series ->
                series.seriesId?.let { seriesId ->
                    Series(
                        id = "${source.id}_series_$seriesId",
                        name = series.name ?: "Unknown",
                        posterUrl = series.cover?.takeIf { it.isNotBlank() }
                            ?: series.streamIcon?.takeIf { it.isNotBlank() },
                        backdropUrl = series.backdropPath?.firstOrNull(),
                        plot = series.plot,
                        genre = categoryMap[series.categoryId] ?: "Uncategorized",
                        year = series.releaseDate?.take(4),
                        rating = series.rating,
                        sourceId = source.id
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getEpisodes(source: Source, seriesId: String): List<Episode> {
        val baseUrl = buildPlayerApiUrl(source.serverUrl!!)

        // Extract the numeric series ID from our composite ID
        val numericSeriesId = seriesId.substringAfterLast("_series_")

        return try {
            val seriesInfo = xtreamApi.getSeriesInfo(
                baseUrl = baseUrl,
                username = source.username!!,
                password = source.password!!,
                seriesId = numericSeriesId
            )

            val episodes = mutableListOf<Episode>()
            seriesInfo.episodes?.forEach { (seasonNum, episodeList) ->
                val seasonNumber = seasonNum.toIntOrNull() ?: 1
                episodeList.forEach { episode ->
                    episode.id?.let { episodeId ->
                        val extension = episode.containerExtension ?: "mp4"
                        episodes.add(
                            Episode(
                                id = "${source.id}_episode_$episodeId",
                                seriesId = seriesId,
                                seasonNumber = seasonNumber,
                                episodeNumber = episode.episodeNum ?: 0,
                                name = episode.title ?: "Episode ${episode.episodeNum}",
                                streamUrl = buildSeriesStreamUrl(source, episodeId, extension),
                                posterUrl = episode.info?.movieImage,
                                plot = episode.info?.plot,
                                duration = episode.info?.duration,
                                sourceId = source.id,
                                containerExtension = extension
                            )
                        )
                    }
                }
            }
            episodes.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun buildPlayerApiUrl(serverUrl: String): String {
        val cleanUrl = serverUrl.trimEnd('/')
        return "$cleanUrl/player_api.php"
    }

    private fun buildLiveStreamUrl(source: Source, streamId: Int): String {
        val cleanUrl = source.serverUrl!!.trimEnd('/')
        return "$cleanUrl/live/${source.username}/${source.password}/$streamId.m3u8"
    }

    private fun buildVodStreamUrl(source: Source, streamId: Int, extension: String): String {
        val cleanUrl = source.serverUrl!!.trimEnd('/')
        return "$cleanUrl/movie/${source.username}/${source.password}/$streamId.$extension"
    }

    private fun buildSeriesStreamUrl(source: Source, episodeId: String, extension: String): String {
        val cleanUrl = source.serverUrl!!.trimEnd('/')
        return "$cleanUrl/series/${source.username}/${source.password}/$episodeId.$extension"
    }
}
