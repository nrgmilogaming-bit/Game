package com.example.game.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.math.Camera3D
import com.example.game.math.Quaternion
import com.example.game.math.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BallRenderer {

    // Cache sample points on unit circles for rotated surface rings
    private val circlePoints: List<Vec3> = (0..24).map { i ->
        val rad = (i * 2 * PI / 24).toFloat()
        Vec3(cos(rad), 0f, sin(rad))
    }

    private val meridianPoints: List<Vec3> = (0..24).map { i ->
        val rad = (i * 2 * PI / 24).toFloat()
        Vec3(0f, cos(rad), sin(rad))
    }

    fun renderBall(
        scope: DrawScope,
        camera: Camera3D,
        ballPos: Vec3,
        ballRadius: Float,
        ballOrientation: Quaternion,
        skin: BallSkin,
        speedFraction: Float,
        isGrounded: Boolean,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val proj = camera.project(ballPos, screenWidth, screenHeight)
        if (!proj.isVisible) return

        val screenRadius = camera.projectRadius(ballRadius, proj.depth)
        if (screenRadius < 2f) return

        // 1. Draw Ball Ground Shadow
        // Find approximate ground plane height beneath ball
        val groundY = ballPos.y - (if (isGrounded) ballRadius else 1.5f)
        val shadowPos = Vec3(ballPos.x, groundY + 0.05f, ballPos.z)
        val shadowProj = camera.project(shadowPos, screenWidth, screenHeight)

        if (shadowProj.isVisible) {
            val heightAboveGround = (ballPos.y - (groundY + ballRadius)).coerceAtLeast(0f)
            val shadowScale = (1.0f / (1.0f + heightAboveGround * 0.4f)).coerceIn(0.2f, 1.0f)
            val shadowRadiusX = screenRadius * shadowScale * 1.1f
            val shadowRadiusY = screenRadius * shadowScale * 0.45f
            val shadowAlpha = (0.45f * shadowScale).coerceIn(0.1f, 0.45f)

            scope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent),
                    center = Offset(shadowProj.screenX, shadowProj.screenY),
                    radius = shadowRadiusX
                ),
                topLeft = Offset(shadowProj.screenX - shadowRadiusX, shadowProj.screenY - shadowRadiusY),
                size = Size(shadowRadiusX * 2, shadowRadiusY * 2)
            )
        }

        // 2. Draw 3D Spherical Shaded Body
        // Light direction: from upper-left-towards camera
        val lightOffset = Offset(
            proj.screenX - screenRadius * 0.35f,
            proj.screenY - screenRadius * 0.38f
        )

        // Radial shading giving rich 3D sphere volume
        val sphereBrush = Brush.radialGradient(
            colors = listOf(
                skin.accentColor,
                skin.primaryColor,
                skin.secondaryColor,
                Color.Black.copy(alpha = 0.85f)
            ),
            center = lightOffset,
            radius = screenRadius * 1.25f
        )

        scope.drawCircle(
            brush = sphereBrush,
            radius = screenRadius,
            center = Offset(proj.screenX, proj.screenY)
        )

        // 3. Render 3D Rotated Surface Features (Shows Ball Rolling!)
        renderBallSurfacePatterns(
            scope = scope,
            camera = camera,
            ballPos = ballPos,
            ballRadius = ballRadius,
            ballOrientation = ballOrientation,
            skin = skin,
            center = Offset(proj.screenX, proj.screenY),
            screenRadius = screenRadius,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )

        // 4. Glossy Specular Highlight (Phong specular reflection)
        val specRadius = screenRadius * 0.28f
        val specOffset = Offset(
            proj.screenX - screenRadius * 0.36f,
            proj.screenY - screenRadius * 0.38f
        )

        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    skin.specularColor.copy(alpha = 0.9f),
                    skin.specularColor.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = specOffset,
                radius = specRadius
            ),
            topLeft = Offset(specOffset.x - specRadius, specOffset.y - specRadius * 0.7f),
            size = Size(specRadius * 2, specRadius * 1.4f)
        )

        // Secondary subtle rim reflection at bottom-right
        val rimRadius = screenRadius * 0.2f
        val rimOffset = Offset(
            proj.screenX + screenRadius * 0.4f,
            proj.screenY + screenRadius * 0.4f
        )
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(skin.accentColor.copy(alpha = 0.35f), Color.Transparent),
                center = rimOffset,
                radius = rimRadius
            ),
            radius = rimRadius,
            center = rimOffset
        )
    }

    private fun renderBallSurfacePatterns(
        scope: DrawScope,
        camera: Camera3D,
        ballPos: Vec3,
        ballRadius: Float,
        ballOrientation: Quaternion,
        skin: BallSkin,
        center: Offset,
        screenRadius: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        when (skin.patternType) {
            BallSkin.PatternType.GRID_CYBER -> {
                // Two perpendicular rings rotated in 3D
                drawRotatedRing(
                    scope = scope,
                    camera = camera,
                    ballPos = ballPos,
                    ballRadius = ballRadius * 0.98f,
                    orientation = ballOrientation,
                    localRingPoints = circlePoints,
                    strokeColor = skin.accentColor.copy(alpha = 0.8f),
                    strokeWidth = (screenRadius * 0.08f).coerceAtLeast(1.5f),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
                drawRotatedRing(
                    scope = scope,
                    camera = camera,
                    ballPos = ballPos,
                    ballRadius = ballRadius * 0.98f,
                    orientation = ballOrientation,
                    localRingPoints = meridianPoints,
                    strokeColor = skin.accentColor.copy(alpha = 0.8f),
                    strokeWidth = (screenRadius * 0.08f).coerceAtLeast(1.5f),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }

            BallSkin.PatternType.MAGMA_RINGS -> {
                // Latitude rings
                drawRotatedRing(
                    scope = scope,
                    camera = camera,
                    ballPos = ballPos,
                    ballRadius = ballRadius * 0.98f,
                    orientation = ballOrientation,
                    localRingPoints = circlePoints,
                    strokeColor = skin.accentColor.copy(alpha = 0.9f),
                    strokeWidth = (screenRadius * 0.12f).coerceAtLeast(2f),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }

            BallSkin.PatternType.MARBLE_SWIRL -> {
                drawRotatedRing(
                    scope = scope,
                    camera = camera,
                    ballPos = ballPos,
                    ballRadius = ballRadius * 0.98f,
                    orientation = ballOrientation,
                    localRingPoints = circlePoints,
                    strokeColor = Color.White.copy(alpha = 0.65f),
                    strokeWidth = (screenRadius * 0.07f).coerceAtLeast(1.5f),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }

            BallSkin.PatternType.EIGHT_BALL -> {
                // Classic white circle badge on the 8 ball
                val badgeLocalDir = Vec3(0f, 0f, 1f)
                val badgeWorldDir = ballOrientation.rotate(badgeLocalDir)
                val badgePos = ballPos + (badgeWorldDir * (ballRadius * 0.98f))
                val badgeProj = camera.project(badgePos, screenWidth, screenHeight)

                // Check if badge is on front-facing hemisphere towards camera
                val toCamera = (camera.position - badgePos).normalized()
                if (badgeWorldDir.dot(toCamera) > 0.15f && badgeProj.isVisible) {
                    val badgeRadius = screenRadius * 0.38f
                    scope.drawCircle(
                        color = Color.White,
                        radius = badgeRadius,
                        center = Offset(badgeProj.screenX, badgeProj.screenY)
                    )
                    // Draw the number 8: two black stacked circles
                    val innerRad = badgeRadius * 0.38f
                    scope.drawCircle(
                        color = Color.Black,
                        radius = innerRad,
                        center = Offset(badgeProj.screenX, badgeProj.screenY - innerRad * 0.8f),
                        style = Stroke(width = innerRad * 0.5f)
                    )
                    scope.drawCircle(
                        color = Color.Black,
                        radius = innerRad * 1.15f,
                        center = Offset(badgeProj.screenX, badgeProj.screenY + innerRad * 0.8f),
                        style = Stroke(width = innerRad * 0.5f)
                    )
                }
            }

            BallSkin.PatternType.METALLIC_GOLD -> {
                // Gold bands
                drawRotatedRing(
                    scope = scope,
                    camera = camera,
                    ballPos = ballPos,
                    ballRadius = ballRadius * 0.98f,
                    orientation = ballOrientation,
                    localRingPoints = circlePoints,
                    strokeColor = Color(0xFFFEF08A).copy(alpha = 0.75f),
                    strokeWidth = (screenRadius * 0.08f).coerceAtLeast(1.5f),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }

            BallSkin.PatternType.PRISM_CRYSTAL -> {
                // Prism faceted rings
                drawRotatedRing(
                    scope = scope,
                    camera = camera,
                    ballPos = ballPos,
                    ballRadius = ballRadius * 0.98f,
                    orientation = ballOrientation,
                    localRingPoints = circlePoints,
                    strokeColor = Color(0xFF67E8F9).copy(alpha = 0.8f),
                    strokeWidth = (screenRadius * 0.09f).coerceAtLeast(2f),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
                drawRotatedRing(
                    scope = scope,
                    camera = camera,
                    ballPos = ballPos,
                    ballRadius = ballRadius * 0.98f,
                    orientation = ballOrientation,
                    localRingPoints = meridianPoints,
                    strokeColor = Color(0xFFF472B6).copy(alpha = 0.75f),
                    strokeWidth = (screenRadius * 0.07f).coerceAtLeast(1.5f),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }
        }
    }

    private fun drawRotatedRing(
        scope: DrawScope,
        camera: Camera3D,
        ballPos: Vec3,
        ballRadius: Float,
        orientation: Quaternion,
        localRingPoints: List<Vec3>,
        strokeColor: Color,
        strokeWidth: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val path = Path()
        var hasStarted = false

        for (pt in localRingPoints) {
            val rotLocal = orientation.rotate(pt)
            // Check surface normal against camera direction (only draw visible front hemisphere of sphere)
            val worldPt = ballPos + (rotLocal * ballRadius)
            val toCam = (camera.position - worldPt).normalized()
            val isFacingCam = rotLocal.dot(toCam) > -0.05f

            if (isFacingCam) {
                val proj = camera.project(worldPt, screenWidth, screenHeight)
                if (proj.isVisible) {
                    if (!hasStarted) {
                        path.moveTo(proj.screenX, proj.screenY)
                        hasStarted = true
                    } else {
                        path.lineTo(proj.screenX, proj.screenY)
                    }
                }
            } else {
                hasStarted = false
            }
        }

        if (hasStarted) {
            scope.drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
