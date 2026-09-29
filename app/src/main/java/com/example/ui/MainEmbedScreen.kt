package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TwitchPurple
import com.example.ui.theme.YouTubeRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEmbedScreen() {
    val context = LocalContext.current

    var selectedPlatform by remember { mutableStateOf(StreamPlatform.YOUTUBE) }

    // Store WebViews for both platforms
    var youtubeWebView by remember { mutableStateOf<WebView?>(null) }
    var twitchWebView by remember { mutableStateOf<WebView?>(null) }

    // History navigation state
    var ytCanGoBack by remember { mutableStateOf(false) }
    var ytCanGoForward by remember { mutableStateOf(false) }
    var twitchCanGoBack by remember { mutableStateOf(false) }
    var twitchCanGoForward by remember { mutableStateOf(false) }

    // Full screen custom view state
    var isFullscreen by remember { mutableStateOf(false) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val activeWebView = when (selectedPlatform) {
        StreamPlatform.YOUTUBE -> youtubeWebView
        StreamPlatform.TWITCH -> twitchWebView
    }

    val currentCanGoBack = when (selectedPlatform) {
        StreamPlatform.YOUTUBE -> ytCanGoBack
        StreamPlatform.TWITCH -> twitchCanGoBack
    }

    val currentCanGoForward = when (selectedPlatform) {
        StreamPlatform.YOUTUBE -> ytCanGoForward
        StreamPlatform.TWITCH -> twitchCanGoForward
    }

    // Handle back button
    BackHandler(enabled = isFullscreen || currentCanGoBack) {
        if (isFullscreen) {
            customViewCallback?.onCustomViewHidden()
            isFullscreen = false
            customView = null
            customViewCallback = null
        } else if (currentCanGoBack && activeWebView != null) {
            activeWebView.goBack()
        }
    }

    if (isFullscreen && customView != null) {
        // Render custom video fullscreen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .zIndex(999f)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    FrameLayout(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        (customView?.parent as? ViewGroup)?.removeView(customView)
                        addView(customView)
                    }
                }
            )
        }
        return
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Top header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile branding avatar / badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedPlatform == StreamPlatform.YOUTUBE)
                                    YouTubeRed.copy(alpha = 0.18f)
                                else
                                    TwitchPurple.copy(alpha = 0.18f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedPlatform == StreamPlatform.YOUTUBE) {
                            YouTubeIcon(modifier = Modifier.size(20.dp))
                        } else {
                            TwitchIcon(modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stefan Gainey",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedPlatform.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp
                            ),
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Navigation buttons
                    IconButton(
                        onClick = { activeWebView?.goBack() },
                        enabled = currentCanGoBack,
                        modifier = Modifier.testTag("nav_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (currentCanGoBack) TextPrimary else TextMuted.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { activeWebView?.goForward() },
                        enabled = currentCanGoForward,
                        modifier = Modifier.testTag("nav_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (currentCanGoForward) TextPrimary else TextMuted.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { activeWebView?.reload() },
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedPlatform.url))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("open_external_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open in external app",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Stefan Gainey Link", selectedPlatform.url)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share link",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Two Tabs: YouTube & Twitch
                TabRow(
                    selectedTabIndex = selectedPlatform.ordinal,
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedPlatform.ordinal]),
                            color = if (selectedPlatform == StreamPlatform.YOUTUBE) YouTubeRed else TwitchPurple,
                            height = 3.dp
                        )
                    },
                    divider = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(DarkBorder)
                        )
                    }
                ) {
                    StreamPlatform.entries.forEach { platform ->
                        val isSelected = selectedPlatform == platform
                        val activeColor = if (platform == StreamPlatform.YOUTUBE) YouTubeRed else TwitchPurple
                        val targetColor by animateColorAsState(
                            targetValue = if (isSelected) activeColor else TextMuted,
                            animationSpec = tween(200),
                            label = "tab_color_${platform.name}"
                        )

                        Tab(
                            selected = isSelected,
                            onClick = { selectedPlatform = platform },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("tab_${platform.name.lowercase()}"),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (platform == StreamPlatform.YOUTUBE) {
                                        YouTubeIcon(
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSelected) YouTubeRed else TextMuted
                                        )
                                    } else {
                                        TwitchIcon(
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isSelected) TwitchPurple else TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = platform.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        ),
                                        color = targetColor
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Keep both WebViews in the composition so switching between YouTube and Twitch
            // doesn't lose state, active stream, video timestamp, or chat!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (selectedPlatform == StreamPlatform.YOUTUBE) 1f else 0f)
                    .zIndex(if (selectedPlatform == StreamPlatform.YOUTUBE) 1f else 0f)
            ) {
                EmbeddedWebView(
                    initialUrl = StreamPlatform.YOUTUBE.url,
                    modifier = Modifier.fillMaxSize(),
                    onWebViewCreated = { webView ->
                        youtubeWebView = webView
                    },
                    onFullScreenChange = { fullScreen, view, callback ->
                        isFullscreen = fullScreen
                        customView = view
                        customViewCallback = callback
                    },
                    onCanGoBackChanged = { ytCanGoBack = it },
                    onCanGoForwardChanged = { ytCanGoForward = it }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (selectedPlatform == StreamPlatform.TWITCH) 1f else 0f)
                    .zIndex(if (selectedPlatform == StreamPlatform.TWITCH) 1f else 0f)
            ) {
                EmbeddedWebView(
                    initialUrl = StreamPlatform.TWITCH.url,
                    modifier = Modifier.fillMaxSize(),
                    onWebViewCreated = { webView ->
                        twitchWebView = webView
                    },
                    onFullScreenChange = { fullScreen, view, callback ->
                        isFullscreen = fullScreen
                        customView = view
                        customViewCallback = callback
                    },
                    onCanGoBackChanged = { twitchCanGoBack = it },
                    onCanGoForwardChanged = { twitchCanGoForward = it }
                )
            }
        }
    }
}
