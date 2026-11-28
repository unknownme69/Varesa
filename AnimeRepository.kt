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
    val description: String,
    val year: String,
    val imageUrl: String,
    val malUrl: String
)

object AnimeRepository {

    suspend fun fetchAnime(query: String = ""): List<Anime> =
        withContext(Dispatchers.IO) {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")

            val endpoint = if (query.isBlank()) {
                "https://api.jikan.moe/v4/top/anime?limit=25"
            } else {
                "https://api.jikan.moe/v4/anime?q=$encodedQuery&limit=25&sfw=true"
            }

            val connection = URL(endpoint).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 12000
                connection.readTimeout = 15000
                connection.setRequestProperty(
                    "User-Agent",
                    "VaresaAndroidApp/1.0"
                )

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw Exception(
                        if (responseCode == 429) {
                            "The anime service is busy. Wait a moment and try again."
                        } else {
                            "The anime service returned error $responseCode."
                        }
                    )
                }

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                val root = JSONObject(response)
                val data = root.optJSONArray("data")
                    ?: return@withContext emptyList()

                buildList {
                    for (index in 0 until data.length()) {
                        val item = data.optJSONObject(index) ?: continue
                        val images = item.optJSONObject("images")
                        val webp = images?.optJSONObject("webp")
                        val jpg = images?.optJSONObject("jpg")

                        val image = webp?.optString("large_image_url")
                            ?.takeIf { !it.isNullOrBlank() && it != "null" }
                            ?: jpg?.optString("large_image_url")
                                ?.takeIf { !it.isNullOrBlank() && it != "null" }
                            ?: jpg?.optString("image_url")
                                ?.takeIf { !it.isNullOrBlank() && it != "null" }
                            ?: ""

                        val title = item.optString("title_english")
                            .takeIf { it.isNotBlank() && it != "null" }
                            ?: item.optString("title")
                                .takeIf { it.isNotBlank() && it != "null" }
                            ?: "Untitled anime"

                        val genreNames = mutableListOf<String>()

                        fun readNames(arrayName: String) {
                            val array = item.optJSONArray(arrayName) ?: return
                            for (i in 0 until array.length()) {
                                val name = array.optJSONObject(i)
                                    ?.optString("name")
                                    ?.takeIf { !it.isNullOrBlank() }
                                if (name != null) genreNames.add(name)
                            }
                        }

                        readNames("genres")
                        readNames("themes")

                        val aired = item.optJSONObject("aired")
                        val from = aired?.optJSONObject("prop")
                            ?.optJSONObject("from")
                        val year = if (from != null && from.optInt("year") > 0) {
                            from.optInt("year").toString()
                        } else {
                            "Year unknown"
                        }

                        add(
                            Anime(
                                malId = item.optInt("mal_id"),
                                title = title,
                                genre = genreNames.distinct().take(3)
                                    .joinToString(" • ")
                                    .ifBlank { "Anime" },
                                description = item.optString("synopsis")
                                    .takeIf { it.isNotBlank() && it != "null" }
                                    ?: "No synopsis is available for this title yet.",
                                year = year,
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
