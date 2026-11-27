
package app.varesa

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF101016)
private val Panel = Color(0xFF1C1B27)
private val Violet = Color(0xFFAA78FF)
private val Muted = Color(0xFFAAA5B8)

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
    var selectedTab by remember { mutableIntStateOf(0) }
    var darkMode by remember { mutableStateOf(true) }

    val tabs = listOf(
        "Discover", "Gallery", "Videos", "Saved", "Settings"
    )
    val symbols = listOf("⌕", "▧", "▷", "♡", "⚙")

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet,
            background = Background,
            surface = Panel,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Scaffold(
            containerColor = if (darkMode) Background else Color(0xFFF4F0FA),
            bottomBar = {
                NavigationBar(
                    containerColor = if (darkMode) Panel else Color.White
                ) {
                    tabs.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Text(
                                    symbols[index],
                                    fontSize = 21.sp
                                )
                            },
                            label = {
                                Text(label, fontSize = 10.sp)
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (selectedTab) {
                    0 -> DiscoverScreen()
                    1 -> GalleryScreen()
                    2 -> VideosScreen()
                    3 -> SavedScreen()
                    4 -> SettingsScreen(
                        darkMode = darkMode,
                        onThemeChange = { darkMode = it }
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(
            title,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            subtitle,
            fontSize = 13.sp,
            color = Muted
        )
    }
}

@Composable
fun DiscoverScreen() {
    val categories = listOf(
        "⚔  Action", "✨  Fantasy", "🌌  Isekai",
        "💜  Romance", "😂  Comedy", "🔥  OP protagonists"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        item {
            ScreenHeader(
                "VARESA",
                "Your personal anime universe"
            )
        }

        item {
            Text(
                "DISCOVER SOMETHING NEW",
                color = Violet,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            FeatureCard(
                "Your next obsession awaits",
                "Explore anime art, scenes, edits and trailers."
            )
            Spacer(Modifier.height(22.dp))
            Text(
                "EXPLORE GENRES",
                color = Muted,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
        }

        items(categories) { category ->
            SimpleCard(category)
        }

        item {
            Spacer(Modifier.height(12.dp))
            FeatureCard(
                "Surprise Me ✨",
                "Let Varesa choose something unexpected."
            )
        }
    }
}

@Composable
fun GalleryScreen() {
    Column(Modifier.padding(20.dp)) {
        ScreenHeader(
            "Gallery",
            "Anime artwork and memorable scenes"
        )
        FeatureCard(
            "Your visual collection",
            "Image discovery will appear here when content sources are connected."
        )
        Spacer(Modifier.height(16.dp))
        SimpleCard("Genshin Impact artwork")
        SimpleCard("Official promotional art")
        SimpleCard("Anime screenshots")
        SimpleCard("Fan artwork")
    }
}

@Composable
fun VideosScreen() {
    Column(Modifier.padding(20.dp)) {
        ScreenHeader(
            "Videos",
            "Trailers, edits and unforgettable moments"
        )
        FeatureCard(
            "Video discovery",
            "YouTube and other supported video sources will be integrated here."
        )
        Spacer(Modifier.height(16.dp))
        SimpleCard("Anime fight scenes")
        SimpleCard("Anime edits")
        SimpleCard("Trailers and previews")
    }
}

@Composable
fun SavedScreen() {
    Column(Modifier.padding(20.dp)) {
        ScreenHeader(
            "Saved",
            "Your personal favourites"
        )
        FeatureCard(
            "Nothing saved yet",
            "Your favourite images and videos will live here."
        )
    }
}

@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        ScreenHeader(
            "Settings",
            "Make Varesa yours"
        )

        Text(
            "APPEARANCE",
            color = Violet,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Dark theme", color = Color.White)
                Text(
                    "Use the darker Varesa appearance",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = darkMode,
                onCheckedChange = onThemeChange
            )
        }

        HorizontalDivider(color = Panel)

        Text(
            "CONTENT",
            color = Violet,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp)
        )

        SimpleCard("Image sources")
        SimpleCard("Video sources")
        SimpleCard("Genshin Impact content")
        SimpleCard("AI artwork preferences")

        Text(
            "VARESA • VERSION 0.1.0",
            color = Muted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}

@Composable
fun FeatureCard(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
fun SimpleCard(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(Panel, RoundedCornerShape(12.dp))
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text("›", color = Violet, fontSize = 23.sp)
    }
}
