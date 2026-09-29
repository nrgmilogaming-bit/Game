package com.example.ui

enum class StreamPlatform(
    val title: String,
    val subtitle: String,
    val url: String,
    val fallbackAppScheme: String
) {
    YOUTUBE(
        title = "YouTube",
        subtitle = "@stefangaineygamingofficial",
        url = "https://www.youtube.com/@stefangaineygamingofficial",
        fallbackAppScheme = "vnd.youtube://www.youtube.com/@stefangaineygamingofficial"
    ),
    TWITCH(
        title = "Twitch",
        subtitle = "twitch.tv/stefangainey",
        url = "https://www.twitch.tv/stefangainey",
        fallbackAppScheme = "twitch://stream/stefangainey"
    )
}
