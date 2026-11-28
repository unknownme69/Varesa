
package app.varesa

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val Violet = Color(0xFFB58AFF)
private val DarkBackground = Color(0xFF101016)
private val DarkPanel = Color(0xFF1D1B28)
private val LightBackground = Color(0xFFF5F1FA)
private val LightPanel = Color.White

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
                        if (status == 429) {
                            "Anime service is busy. Wait a little and refresh."
                        } else {
                            "Anime service returned error $status."
                        }
                    )
                }

                val body = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                val results = JSONObject(body)
                    .optJSONArray("data")
                    ?: return@withContext emptyList()

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

                        val image = item.optJSONObject("images")
                            ?.optJSONObject("jpg")
                            ?.optString("large_image_url")
                            ?.takeIf { !it.isNullOrBlank() && it != "null" }
                            ?: ""

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VaresaApp()
        }
    }
}

@Composable
fun VaresaApp() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("varesa_preferences", 0)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var darkMode by remember {
        mutableStateOf(prefs.getBoolean("dark", true))
    }
    var genshinEnabled by remember {
        mutableStateOf(prefs.getBoolean("genshin", true))
    }
    var search by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }

    var saved by remember {
        mutableStateOf(
            prefs.getStringSet("saved", emptySet())?.toSet() ?: emptySet()
        )
    }

    var animeItems by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(search, refreshKey) {
        delay(if (search.isBlank()) 0 else 650)
        loading = true
        error = null

        try {
            animeItems = AnimeRepository.fetchAnime(search)
        } catch (e: Exception) {
            error = e.message ?: "Unable to load anime right now."
        }

        loading = false
    }

    val background = if (darkMode) DarkBackground else LightBackground
    val panel = if (darkMode) DarkPanel else LightPanel
    val textColor = if (darkMode) Color.White else Color(0xFF24202D)
    val muted = if (darkMode) Color(0xFFAAA5B8) else Color(0xFF777080)

    val genres = listOf("All", "Action", "Fantasy", "Isekai", "Comedy")

    MaterialTheme(
        colorScheme = if (darkMode) {
            darkColorScheme(
                primary = Violet,
                background = background,
                surface = panel,
                onSurface = textColor
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF8050C8),
                background = background,
                surface = panel,
                onSurface = textColor
            )
        }
    ) {
        Scaffold(
            containerColor = background,
            bottomBar = {
                NavigationBar(containerColor = panel) {
                    val names = listOf(
                        "Discover", "Gallery", "Videos", "Saved", "Settings"
                    )
                    val symbols = listOf("⌕", "▧", "▷", "♡", "⚙")

                    names.forEachIndexed { index, name ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Text(symbols[index], fontSize = 21.sp)
                            },
                            label = { Text(name, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Violet,
                                selectedTextColor = Violet,
                                indicatorColor = Violet.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (selectedTab) {
                    0 -> DiscoverPage(
                        search = search,
                        onSearch = { search = it },
                        selectedGenre = selectedGenre,
                        onGenre = { selectedGenre = it },
                        genres = genres,
                        animeItems = animeItems,
                        loading = loading,
                        error = error,
                        onRefresh = { refreshKey++ },
                        saved = saved,
                        onSave = { title ->
                            saved = if (title in saved) {
                                saved - title
                            } else {
                                saved + title
                            }
                            prefs.edit().putStringSet("saved", saved).apply()
                        },
                        panel = panel,
                        textColor = textColor,
                        muted = muted
                    )

                    1 -> GalleryPage(
                        genshinEnabled, panel, textColor, muted
                    )

                    2 -> VideosPage(panel, textColor, muted)

                    3 -> SavedPage(
                        saved = saved,
                        onRemove = { title ->
                            saved = saved - title
                            prefs.edit().putStringSet("saved", saved).apply()
                        },
                        panel = panel,
                        textColor = textColor,
                        muted = muted
                    )

                    4 -> SettingsPage(
                        darkMode = darkMode,
                        onDarkMode = {
                            darkMode = it
                            prefs.edit().putBoolean("dark", it).apply()
                        },
                        genshinEnabled = genshinEnabled,
                        onGenshin = {
                            genshinEnabled = it
                            prefs.edit().putBoolean("genshin", it).apply()
                        },
                        panel = panel,
                        textColor = textColor,
                        muted = muted
                    )
                }
            }
        }
    }
}

@Composable
fun PageTitle(
    title: String,
    subtitle: String,
    textColor: Color,
    muted: Color
) {
    Column(Modifier.padding(bottom = 20.dp)) {
        Text(
            title,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(subtitle, fontSize = 13.sp, color = muted)
    }
}

@Composable
fun DiscoverPage(
    search: String,
    onSearch: (String) -> Unit,
    selectedGenre: String,
    onGenre: (String) -> Unit,
    genres: List<String>,
    animeItems: List<Anime>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    saved: Set<String>,
    onSave: (String) -> Unit,
    panel: Color,
    textColor: Color,
    muted: Color
) {
    val context = LocalContext.current

    val filtered = animeItems.filter { anime ->
        selectedGenre == "All" ||
            anime.genre.contains(selectedGenre, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp)
    ) {
        item {
            PageTitle(
                "VARESA",
                "Your personal anime universe",
                textColor,
                muted
            )
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Search anime...", color = muted)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LIVE ANIME CATALOGUE",
                    color = Violet,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onRefresh) {
                    Text("↻ Refresh", color = Violet)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    genres.take(3).forEach { genre ->
                        FilterChip(
                            selected = selectedGenre == genre,
                            onClick = { onGenre(genre) },
                            label = { Text(genre) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    genres.drop(3).forEach { genre ->
                        FilterChip(
                            selected = selectedGenre == genre,
                            onClick = { onGenre(genre) },
                            label = { Text(genre) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        if (loading && animeItems.isEmpty()) {
            item {
                LoadingMessage("Finding anime for you...", muted)
            }
        }

        if (loading && animeItems.isNotEmpty()) {
            item {
                Text(
                    "Updating results...",
                    color = muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        if (error != null) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(panel, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        "Couldn't load anime",
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(error, color = muted, fontSize = 13.sp)
                    TextButton(onClick = onRefresh) {
                        Text("Try again", color = Violet)
                    }
                }
            }
        }

        items(filtered, key = { it.malId }) { anime ->
            AnimeCard(
                anime = anime,
                saved = anime.title in saved,
                onSave = { onSave(anime.title) },
                panel = panel,
                textColor = textColor,
                muted = muted,
                onOpen = {
                    if (anime.malUrl.isNotBlank()) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(anime.malUrl)
                            )
                        )
                    }
                }
            )
        }

        if (!loading && error == null && filtered.isEmpty()) {
            item {
                Text(
                    if (animeItems.isEmpty()) {
                        "No results found. Try another search."
                    } else {
                        "No titles match this genre."
                    },
                    color = muted,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        item {
            Text(
                "Catalogue data provided by Jikan / MyAnimeList. " +
                    "Tap a title to open its source page.",
                color = muted,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun LoadingMessage(message: String, muted: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 35.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Violet)
        Spacer(Modifier.height(12.dp))
        Text(message, color = muted, fontSize = 13.sp)
    }
}

@Composable
fun AnimeCard(
    anime: Anime,
    saved: Boolean,
    onSave: () -> Unit,
    panel: Color,
    textColor: Color,
    muted: Color,
    onOpen: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(panel, RoundedCornerShape(17.dp))
            .clickable { onOpen() }
            .padding(11.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (anime.imageUrl.isNotBlank()) {
            AsyncImage(
                model = anime.imageUrl,
                contentDescription = "${anime.title} cover art",
                modifier = Modifier
                    .width(88.dp)
                    .height(124.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .width(88.dp)
                    .height(124.dp)
                    .background(
                        Violet.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("V", color = Violet, fontSize = 30.sp)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                anime.title,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "${anime.genre}  •  ${anime.year}",
                color = Violet,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                anime.description,
                color = muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 4
            )

            Spacer(Modifier.height(4.dp))

            TextButton(
                onClick = onSave,
                contentPadding = PaddingValues(0.dp)
                
