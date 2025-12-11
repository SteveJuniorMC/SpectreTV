package com.spectretv.app.data.remote

import com.spectretv.app.data.remote.api.XtreamApi
import com.spectretv.app.data.remote.dto.XtreamCategory
import com.spectretv.app.data.remote.dto.XtreamStream
import com.spectretv.app.domain.model.Channel
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
                    streamUrl = buildStreamUrl(source, streamId),
                    logoUrl = stream.streamIcon?.takeIf { it.isNotBlank() },
                    group = categoryMap[stream.categoryId] ?: "Uncategorized",
                    epgId = stream.epgChannelId,
                    sourceId = source.id
                )
            }
        }
    }

    private fun buildPlayerApiUrl(serverUrl: String): String {
        val cleanUrl = serverUrl.trimEnd('/')
        return "$cleanUrl/player_api.php"
    }

    private fun buildStreamUrl(source: Source, streamId: Int): String {
        val cleanUrl = source.serverUrl!!.trimEnd('/')
        return "$cleanUrl/live/${source.username}/${source.password}/$streamId.m3u8"
    }
}
