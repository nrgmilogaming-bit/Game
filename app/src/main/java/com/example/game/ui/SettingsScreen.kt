package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.viewmodel.GameViewModel

@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val sound = progress?.soundEnabled ?: true
    val haptics = progress?.hapticsEnabled ?: true
    val sensitivity = progress?.sensitivity ?: 1.0f
    val showGuide = progress?.showTouchGuide ?: true

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "SETTINGS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Physics & Controls Calibration",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Controls Explanation Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x330284C7)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4438BDF8))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Invisible Thumb Control",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Place your thumb anywhere on the screen and glide. Pushing up drives momentum forward into the track; moving left/right steers smoothly into curves. Releasing lets the ball roll with natural inertia and realistic physics.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Audio & Haptics Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Sound FX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Sound Effects & Audio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Procedural rolling rumble & chimes", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = sound,
                            onCheckedChange = { checked ->
                                viewModel.updateSettings(checked, haptics, sensitivity, showGuide)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8))
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Haptics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = Color(0xFFF472B6))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Haptic Feedback", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Vibrations on bumps, pads & gems", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = haptics,
                            onCheckedChange = { checked ->
                                viewModel.updateSettings(sound, checked, sensitivity, showGuide)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF472B6))
                        )
                    }
                }
            }

            // Steering & Guide Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Faint Ring Guide Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFFFACC15))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Faint Thumb Vector", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(if (showGuide) "Subtle ring at touch point" else "100% invisible pure touch controls", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = showGuide,
                            onCheckedChange = { checked ->
                                viewModel.updateSettings(sound, haptics, sensitivity, checked)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFACC15))
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Sensitivity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Steering Sensitivity", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = String.format("%.1fx", sensitivity),
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }

                        Slider(
                            value = sensitivity,
                            onValueChange = { newSens ->
                                viewModel.updateSettings(sound, haptics, newSens, showGuide)
                            },
                            valueRange = 0.5f..2.5f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF0284C7)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gentle (0.5x)", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                            Text("Normal (1.0x)", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                            Text("Hyper (2.5x)", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
