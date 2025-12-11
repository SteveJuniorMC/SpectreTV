package com.spectretv.app.data.remote.parser

import com.spectretv.app.domain.model.Channel
import java.security.MessageDigest

class M3UParser {

    fun parse(content: String, sourceId: Long): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.startsWith("#EXTINF:")) {
                val info = parseExtInf(line)

                // Find the URL (next non-comment, non-empty line)
                var urlIndex = i + 1
                while (urlIndex < lines.size) {
                    val urlLine = lines[urlIndex].trim()
                    if (urlLine.isNotEmpty() && !urlLine.startsWith("#")) {
                        val channelId = generateChannelId(urlLine, sourceId)
                        channels.add(
                            Channel(
                                id = channelId,
                                name = info.name,
                                streamUrl = urlLine,
                                logoUrl = info.logoUrl,
                                group = info.group,
                                epgId = info.epgId,
                                sourceId = sourceId
                            )
                        )
                        i = urlIndex
                        break
                    }
                    urlIndex++
                }
            }
            i++
        }

        return channels
    }

    private fun parseExtInf(line: String): ExtInfInfo {
        // Format: #EXTINF:-1 tvg-id="..." tvg-name="..." tvg-logo="..." group-title="...",Channel Name
        var name = ""
        var logoUrl: String? = null
        var group = "Uncategorized"
        var epgId: String? = null

        // Extract channel name (after the last comma)
        val commaIndex = line.lastIndexOf(',')
        if (commaIndex != -1) {
            name = line.substring(commaIndex + 1).trim()
        }

        // Extract attributes
        val attributePattern = """(\w+(?:-\w+)*)="([^"]*)"""".toRegex()
        attributePattern.findAll(line).forEach { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2]

            when (key) {
                "tvg-logo" -> logoUrl = value.takeIf { it.isNotBlank() }
                "group-title" -> group = value.takeIf { it.isNotBlank() } ?: "Uncategorized"
                "tvg-id" -> epgId = value.takeIf { it.isNotBlank() }
                "tvg-name" -> if (name.isBlank()) name = value
            }
        }

        return ExtInfInfo(
            name = name.ifBlank { "Unknown Channel" },
            logoUrl = logoUrl,
            group = group,
            epgId = epgId
        )
    }

    private fun generateChannelId(url: String, sourceId: Long): String {
        val input = "$sourceId:$url"
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private data class ExtInfInfo(
        val name: String,
        val logoUrl: String?,
        val group: String,
        val epgId: String?
    )
}
