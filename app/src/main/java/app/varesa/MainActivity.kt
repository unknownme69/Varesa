
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

private val Violet = Color(0xFFB58AFF)
private val DarkBackground = Color(0xFF101016)
private val DarkPanel = Color(0xFF1D1B28)
private val LightBackground = Color(0xFFF5F1FA)
private val LightPanel = Color(0xFFFFFFFF)

data class Anime(
    val title: String,
    val genre: String,
    val description: String,
    val year: String
)

private val animeList = listOf(
    Anime("That Time I Got Reincarnated as a Slime", "Isekai", "A human begins a new life as a powerful slime.", "2018"),
    Anime("The Eminence in Shadow", "Action", "A young man builds a secret organization from the shadows.", "2022"),
    Anime("No Game No Life", "Fantasy", "Two gaming geniuses enter a world ruled by games.", "2014"),
    Anime("Reincarnated as a Sword", "Isekai", "A sentient sword and a young catgirl become partners.", "2022"),
    Anime("Overlord", "Fantasy", "A player remains trapped in a game as an immensely powerful ruler.", "2015"),
    Anime("Mushoku Tensei", "Fantasy", "A second chance at life in a world of magic.", "2021"),
    Anime("KonoSuba", "Comedy", "A chaotic fantasy adventure full of ridiculous disasters.", "2016"),
    Anime("The Misfit of Demon King Academy", "Action", "A legendary demon king returns to a changed world.", "2020"),
    Anime("Arifureta", "Isekai", "A betrayed student fights his way through a dangerous world.", "2019"),
    Anime("The World's Finest Assassin", "Isekai", "An assassin is reborn with a mission in a fantasy world.", "2021"),
    Anime("The Rising of the Shield Hero", "Action", "A summoned hero must rebuild his reputation.", "2019"),
    Anime("How Not to Summon a Demon Lord", "Comedy", "A gamer finds himself inside the world of his favourite game.", "2018")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VaresaApp() }
    }
}

@Composable
fun VaresaApp() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("varesa_preferences", 0)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
    var genshinEnabled by remember { mutableStateOf(prefs.getBoolean("genshin", true)) }
    var search by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }
    var saved by remember {
        mutableStateOf(
            prefs.getStringSet("saved", emptySet())?.toSet() ?: emptySet()
        )
    }

    val genres = listOf("All", "Action", "Fantasy", "Isekai", "Comedy")

    val background = if (darkMode) DarkBackground else LightBackground
    val panel = if (darkMode) DarkPanel else LightPanel
    val textColor = if (darkMode) Color.White else Color(0xFF24202D)
    val muted = if (darkMode) Color(0xFFAAA5B8) else Color(0xFF777080)

    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme(
            primary = Violet,
            background = background,
            surface = panel,
            onSurface = textColor
        ) else lightColorScheme(
            primary = Color(0xFF8050C8),
            background = background,
            surface = panel,
            onSurface = textColor
        )
    ) {
        Scaffold(
            containerColor = background,
            bottomBar = {
                NavigationBar(
                    containerColor = panel
                ) {
                    val names = listOf("Discover", "Gallery", "Videos", "Saved", "Settings")
                    val symbols = listOf("⌕", "▧", "▷", "♡", "⚙")

                    names.forEachIndexed { index, name ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Text(symbols[index], fontSize = 21.sp)
                            },
                            label = {
                                Text(name, fontSize = 10.sp)
                            },
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
                        saved = saved,
                        onSave = { title ->
                            saved = if (title in saved) saved - title else saved + title
                            prefs.edit().putStringSet("saved", saved).apply()
                        },
                        panel = panel,
                        textColor = textColor,
                        muted = muted
                    )

                    1 -> GalleryPage(
                        genshinEnabled = genshinEnabled,
                        panel = panel,
                        textColor = textColor,
                        muted = muted
                    )

                    2 -> VideosPage(
                        panel = panel,
                        textColor = textColor,
                        muted = muted
                    )

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
fun PageTitle(title: String, subtitle: String, textColor: Color, muted: Color) {
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
    saved: Set<String>,
    onSave: (String) -> Unit,
    panel: Color,
    textColor: Color,
    muted: Color
) {
    val context = LocalContext.current

    val filtered = animeList.filter {
        (selectedGenre == "All" || it.genre == selectedGenre) &&
        (it.title.contains(search, true) || it.description.contains(search, true))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp)
    ) {
        item {
            PageTitle("VARESA", "Your personal anime universe", textColor, muted)
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search anime...", color = muted) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text("EXPLORE GENRES", color = Violet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }

        item {
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
            Spacer(Modifier.height(18.dp))
        }

        item {
            Button(
                onClick = {
                    val choice = animeList.random()
                    onSearch(choice.title)
                    onGenre("All")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Violet)
            ) {
                Text("✦  Surprise Me", color = Color(0xFF21152E))
            }
            Spacer(Modifier.height(18.dp))
            Text("DISCOVER ANIME", color = Violet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }

        items(filtered) { anime ->
            AnimeCard(
                anime = anime,
                saved = anime.title in saved,
                onSave = { onSave(anime.title) },
                panel = panel,
                textColor = textColor,
                muted = muted,
                onOpen = {
                    val url = "https://www.google.com/search?q=${Uri.encode(anime.title + " anime")}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            )
        }

        if (filtered.isEmpty()) {
            item {
                Text("No matches found. Try another search.", color = muted)
            }
        }
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
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(panel, RoundedCornerShape(17.dp))
            .clickable { onOpen() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(anime.title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${anime.genre}  •  ${anime.year}", color = Violet, fontSize = 11.sp)
            }
            TextButton(onClick = onSave) {
                Text(if (saved) "♥ Saved" else "♡ Save", color = Violet)
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(anime.description, color = muted, fontSize = 13.sp)
        Text("Tap to explore  ›", color = Violet, fontSize = 12.sp, modifier = Modifier.padding(top = 9.dp))
    }
}

@Composable
fun GalleryPage(
    genshinEnabled: Boolean,
    panel: Color,
    textColor: Color,
    muted: Color
) {
    val context = LocalContext.current
    val topics = buildList {
        add("Anime artwork")
        add("Anime screenshots")
        add("Official promotional art")
        add("Fan artwork")
        if (genshinEnabled) add("Genshin Impact artwork")
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item {
            PageTitle("Gallery", "Explore artwork and memorable scenes", textColor, muted)
            Text(
                "IMAGE COLLECTIONS",
                color = Violet,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
        }

        items(topics) { topic ->
            SimpleCard(topic, panel, textColor, muted) {
                val query = Uri.encode(topic)
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://commons.wikimedia.org/w/index.php?search=$query"))
                )
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Text(
                "These links open source searches. An in-app image feed and fullscreen viewer are still being developed.",
                color = muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun VideosPage(panel: Color, textColor: Color, muted: Color) {
    val context = LocalContext.current
    val topics = listOf(
        "Anime",
        "Anime fight scenes",
        "Anime edits",
        "Anime trailers",
        "Overpowered anime protagonists",
        "Isekai anime moments",
        "Genshin Impact Varesa"
    )

    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item {
            PageTitle("Videos", "Trailers, edits and unforgettable moments", textColor, muted)
            Text("EXPLORE VIDEO SEARCHES", color = Violet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
        }

        items(topics) { topic ->
            SimpleCard(topic, panel, textColor, muted) {
                val query = Uri.encode(topic)
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
                )
            }
        }
    }
}

@Composable
fun SavedPage(
    saved: Set<String>,
    onRemove: (String) -> Unit,
    panel: Color,
    textColor: Color,
    muted: Color
) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item {
            PageTitle("Saved", "Your personal favourites", textColor, muted)
        }

        if (saved.isEmpty()) {
            item {
                SimpleCard(
                    "Nothing saved yet. Visit Discover and tap Save on an anime.",
                    panel,
                    textColor,
                    muted
                ) {}
            }
        } else {
            items(saved.toList()) { title ->
                SimpleCard(title, panel, textColor, muted) {
                    onRemove(title)
                }
            }
            item {
                Text("Tap a saved item to remove it.", color = muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SettingsPage(
    darkMode: Boolean,
    onDarkMode: (Boolean) -> Unit,
    genshinEnabled: Boolean,
    onGenshin: (Boolean) -> Unit,
    panel: Color,
    textColor: Color,
    muted: Color
) {
    Column(
        Modifier.fillMaxSize().padding(20.dp)
    ) {
        PageTitle("Settings", "Make Varesa yours", textColor, muted)

        Text("APPEARANCE", color = Violet, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        SettingSwitch(
            title = "Dark theme",
            description = "Use the darker Varesa appearance",
            checked = darkMode,
            onChange = onDarkMode,
            textColor = textColor,
            muted = muted
        )

        HorizontalDivider(color = muted.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

        Text("CONTENT", color = Violet, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        SettingSwitch(
            title = "Genshin Impact",
            description = "Include Genshin in gallery searches",
            checked = genshinEnabled,
            onChange = onGenshin,
            textColor = textColor,
            muted = muted
        )

        Spacer(Modifier.height(18.dp))
        Text("ABOUT", color = Violet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("VARESA", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Version 1.0 foundation", color = muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Varesa is designed to be lightweight. It does not run a permanent background service.",
            color = muted,
            fontSize = 13.sp
        )
    }
}

@Composable
fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    textColor: Color,
    muted: Color
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = textColor, fontWeight = FontWeight.Medium)
            Text(description, color = muted, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SimpleCard(
    title: String,
    panel: Color,
    textColor: Color,
    muted: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(panel, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = textColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("›", color = Violet, fontSize = 23.sp)
    }
}
