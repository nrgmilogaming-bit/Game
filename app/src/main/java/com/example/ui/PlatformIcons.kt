package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun YouTubeIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color(0xFFFF0033)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Rounded rectangle background
        val rectHeight = h * 0.7f
        val rectTop = (h - rectHeight) / 2f
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, rectTop),
            size = Size(w, rectHeight),
            cornerRadius = CornerRadius(w * 0.22f, h * 0.22f)
        )

        // Center play triangle
        val trianglePath = Path().apply {
            moveTo(w * 0.40f, h * 0.33f)
            lineTo(w * 0.68f, h * 0.50f)
            lineTo(w * 0.40f, h * 0.67f)
            close()
        }
        drawPath(trianglePath, Color.White)
    }
}

@Composable
fun TwitchIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color(0xFF9146FF)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Twitch speech bubble shape
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.12f)
            lineTo(w * 0.88f, h * 0.12f)
            lineTo(w * 0.88f, h * 0.68f)
            lineTo(w * 0.66f, h * 0.68f)
            lineTo(w * 0.50f, h * 0.88f)
            lineTo(w * 0.38f, h * 0.68f)
            lineTo(w * 0.12f, h * 0.68f)
            close()
        }
        drawPath(path, tint)

        // Eyes (two vertical slots)
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.35f, h * 0.32f),
            size = Size(w * 0.09f, h * 0.22f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.56f, h * 0.32f),
            size = Size(w * 0.09f, h * 0.22f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }
}
