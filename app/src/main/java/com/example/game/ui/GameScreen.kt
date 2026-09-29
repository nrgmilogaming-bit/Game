package com.example.game.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.renderer.Renderer3D
import com.example.game.viewmodel.GameMode
import com.example.game.viewmodel.GameStatus
import com.example.game.viewmodel.GameUiState
import com.example.game.viewmodel.GameViewModel
import com.example.game.viewmodel.HapticEvent
import kotlin.math.sin

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val touchInput by viewModel.touchInput.collectAsStateWithLifecycle()
    val view = LocalView.current
    val renderer = remember { Renderer3D() }

    // Haptic events listener
    LaunchedEffect(Unit) {
        viewModel.hapticEvents.collect { event ->
            when (event) {
                HapticEvent.LightTap -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                HapticEvent.MediumImpact -> view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                HapticEvent.HeavyImpact -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                HapticEvent.Success -> {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                }
                HapticEvent.Warning -> {
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                }
            }
        }
    }

    // Trigger frame updates for smooth 60fps rendering in Compose
    var frameTick by remember { mutableStateOf(0L) }
    LaunchedEffect(uiState.status) {
        while (uiState.status == GameStatus.PLAYING) {
            withFrameNanos { time ->
                frameTick = time
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            // Invisible touch control on the entire game canvas:
            // Moving thumb anywhere controls momentum and direction directly!
            .pointerInput(uiState.status) {
                if (uiState.status == GameStatus.PLAYING) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            viewModel.onTouchDown(offset.x, offset.y)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            viewModel.onTouchMove(change.position.x, change.position.y)
                        },
                        onDragEnd = {
                            viewModel.onTouchUp()
                        },
                        onDragCancel = {
                            viewModel.onTouchUp()
                        }
                    )
                }
            }
            .testTag("game_screen")
    ) {
        // 3D Canvas Scene
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read frameTick to trigger continuous redraw
            val currentTick = frameTick

            // Sync particles with ball movement
            if (viewModel.ballPhysics.isGrounded && viewModel.ballPhysics.horizontalSpeed > 4.5f) {
                renderer.particleSystem.spawnDust(
                    viewModel.ballPhysics.position,
                    viewModel.ballPhysics.velocity
                )
            }
            if (viewModel.ballPhysics.speedMultiplier > 1.1f) {
                renderer.particleSystem.spawnSpeedTrail(
                    viewModel.ballPhysics.position,
                    uiState.selectedSkin.accentColor
                )
            }
            renderer.particleSystem.update(0.016f)

            renderer.renderScene(
                scope = this,
                camera = viewModel.camera,
                level = viewModel.currentLevelConfig,
                elements = viewModel.activeTrackElements,
                ballPos = viewModel.ballPhysics.position,
                ballRadius = viewModel.ballPhysics.radius,
                ballOrientation = viewModel.ballPhysics.orientation,
                skin = uiState.selectedSkin,
                speedFraction = (viewModel.ballPhysics.speed / 24f).coerceIn(0f, 1f),
                isGrounded = viewModel.ballPhysics.isGrounded,
                animTime = viewModel.animTime,
                screenWidth = size.width,
                screenHeight = size.height
            )

            // Optional faint thumb guide ring (if enabled by user, otherwise 100% invisible!)
            if (uiState.showTouchGuide && touchInput.isTouching) {
                val startX = touchInput.touchStartX
                val startY = touchInput.touchStartY
                val currX = touchInput.touchCurrentX
                val currY = touchInput.touchCurrentY

                // Base subtle anchor circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = 35f,
                    center = Offset(startX, startY),
                    style = Stroke(width = 2f)
                )

                // Force vector line
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.15f), uiState.selectedSkin.accentColor.copy(alpha = 0.6f))
                    ),
                    start = Offset(startX, startY),
                    end = Offset(currX, currY),
                    strokeWidth = 3f
                )

                // Current thumb position glowing node
                drawCircle(
                    color = uiState.selectedSkin.accentColor.copy(alpha = 0.5f),
                    radius = 16f,
                    center = Offset(currX, currY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 6f,
                    center = Offset(currX, currY)
                )
            }
        }

        // HUD Overlay
        GameHudOverlay(
            uiState = uiState,
            onPauseClick = { viewModel.pauseGame() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Game Over Overlay
        AnimatedVisibility(
            visible = uiState.status == GameStatus.GAME_OVER,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GameOverDialog(
                uiState = uiState,
                onRetry = { viewModel.restartCurrentLevel() },
                onMenu = onNavigateBack
            )
        }

        // Victory Overlay
        AnimatedVisibility(
            visible = uiState.status == GameStatus.LEVEL_COMPLETE,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            VictoryDialog(
                uiState = uiState,
                onNextLevel = { viewModel.nextLevel() },
                onRetry = { viewModel.restartCurrentLevel() },
                onMenu = onNavigateBack
            )
        }

        // Pause Overlay
        AnimatedVisibility(
            visible = uiState.status == GameStatus.PAUSED,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            PauseDialog(
                uiState = uiState,
                onResume = { viewModel.resumeGame() },
                onRestart = { viewModel.restartCurrentLevel() },
                onMenu = onNavigateBack
            )
        }
    }
}

@Composable
private fun GameHudOverlay(
    uiState: GameUiState,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top row: Level info & controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level / Mode title pill
            Surface(
                color = Color(0xCC0F172A),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.mode == GameMode.CAMPAIGN) "LVL ${uiState.currentLevelId}" else "ENDLESS",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.levelName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            // Right side stats & Pause
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gems pill
                Surface(
                    color = Color(0xCC0F172A),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3306B6D4))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "Gems",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.mode == GameMode.CAMPAIGN) "${uiState.gemsCollected}/${uiState.totalGemsInLevel}" else "${uiState.gemsCollected}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Pause button
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0F172A))
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Second row: Timer / Score & Speedometer & Lives
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer or Score
            Surface(
                color = Color(0x88000000),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.mode == GameMode.CAMPAIGN) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val sec = uiState.elapsedTimeSeconds.toInt()
                        val ms = ((uiState.elapsedTimeSeconds - sec) * 100).toInt()
                        Text(
                            text = String.format("%02d:%02d.%02d", sec / 60, sec % 60, ms),
                            color = Color(0xFFFACC15),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "SCORE: ${uiState.endlessScore}m",
                            color = Color(0xFFFACC15),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Speedometer & Lives
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Speedometer
                Surface(
                    color = Color(0x88000000),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${uiState.speedKmh.toInt()} km/h",
                            color = Color(0xFF4ADE80),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }

                if (uiState.mode == GameMode.CAMPAIGN) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Lives hearts
                    Row {
                        repeat(3) { i ->
                            val alive = i < uiState.lives
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (alive) Color(0xFFEF4444) else Color(0x44EF4444),
                                modifier = Modifier.size(16.dp).padding(horizontal = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VictoryDialog(
    uiState: GameUiState,
    onNextLevel: () -> Unit,
    onRetry: () -> Unit,
    onMenu: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .testTag("victory_dialog"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFA0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "COURSE COMPLETED!",
                color = Color(0xFFFDE047),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Text(
                text = uiState.levelName,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Star Rating display
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (s in 1..3) {
                    val earned = s <= uiState.starsEarned
                    Icon(
                        imageVector = if (earned) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star $s",
                        tint = if (earned) Color(0xFFFACC15) else Color(0x44FACC15),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Time & Gems stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x551E293B), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TIME", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val sec = uiState.elapsedTimeSeconds.toInt()
                    val ms = ((uiState.elapsedTimeSeconds - sec) * 100).toInt()
                    Text(
                        text = String.format("%02d:%02d.%02d", sec / 60, sec % 60, ms),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GEMS", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${uiState.gemsCollected} / ${uiState.totalGemsInLevel}",
                        color = Color(0xFF38BDF8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Button(
                onClick = onNextLevel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("next_level_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next Course", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry")
                }
                OutlinedButton(
                    onClick = onMenu,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Menu")
                }
            }
        }
    }
}

@Composable
private fun GameOverDialog(
    uiState: GameUiState,
    onRetry: () -> Unit,
    onMenu: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .testTag("game_over_dialog"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFA1C1917)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FELL INTO THE ABYSS",
                color = Color(0xFFF87171),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (uiState.mode == GameMode.ENDLESS) "Distance: ${uiState.endlessScore}m" else "Try keeping momentum balanced on curves!",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("retry_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Try Again", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onMenu,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Menu")
            }
        }
    }
}

@Composable
private fun PauseDialog(
    uiState: GameUiState,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .testTag("pause_dialog"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFA0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PAUSED",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Invisible thumb control: Drag anywhere on screen to roll",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("resume_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Resume", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restart Course")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onMenu,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Main Menu")
            }
        }
    }
}
