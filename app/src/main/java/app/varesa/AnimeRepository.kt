
package app.varesa

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Anime(
    val malId: Int,
    val title: String,
    val genre: String,
    val year: String,
    val description: String,
    val imageUrl: String,
    val malUrl: String
)

object AnimeRepository {
    suspend fun fetchAnime(query: String): List<Anime> =
        withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val endpoint = if (query.isBlank()) {
                "https://api.jikan.moe/v4/top/anime?limit=20"
            } else {
                "https://api.jikan.moe/v4/anime?q=$encoded&limit=20"
            }

            val connection =
                URL(endpoint).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 12000
                connection.readTimeout = 15000
                connection.setRequestProperty("Accept", "application/json")

                val status = connection.responseCode
                if (status !in 200..299) {
                    throw IllegalStateException(
                        if (status == 429)
                            "Anime service is busy. Wait and refresh."
                        else "Anime service returned error $status."
                    )
                }

                val body = connection.inputStream
                    .bufferedReader().use { it.readText() }

                val results = JSONObject(body)
                    .optJSONArray("data") ?: return@withContext emptyList()

                buildList {
                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue

                        val title = item.optString("title_english")
                            .takeIf { it.isNotBlank() && it != "null" }
                            ?: item.optString("title")

                        if (title.isBlank() || title == "null") continue

                        val genres = item.optJSONArray("genres")
                        val genreNames = mutableListOf<String>()

                        if (genres != null) {
                            for (g in 0 until genres.length()) {
                                val name = genres.optJSONObject(g)
                                    ?.optString("name")
                                if (!name.isNullOrBlank() && name != "null") {
                                    genreNames.add(name)
                                }
                            }
                        }

                        val images = item.optJSONObject("images")
                            ?.optJSONObject("jpg")

                        val image = images?.optString("large_image_url")
                            ?.takeIf { !it.isNullOrBlank() && it != "null" }
                            ?: images?.optString("image_url").orEmpty()

                        val year = item.optJSONObject("aired")
                            ?.optJSONObject("prop")
                            ?.optJSONObject("from")
                            ?.optInt("year", 0) ?: 0

                        add(
                            Anime(
                                malId = item.optInt("mal_id"),
                                title = title,
                                genre = genreNames.joinToString(", ")
                                    .ifBlank { "Anime" },
                                year = if (year > 0) year.toString()
                                    else "Year unknown",
                                description = item.optString("synopsis")
                                    .takeIf {
                                        !it.isNullOrBlank() && it != "null"
                                    } ?: "No synopsis is available yet.",
                                imageUrl = image,
                                malUrl = item.optString("url")
                            )
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
}
