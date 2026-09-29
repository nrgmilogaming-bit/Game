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
import com.example.game.physics.BumperElement
import com.example.game.physics.CheckpointElement
import com.example.game.physics.GemElement
import com.example.game.physics.GoalPortalElement
import com.example.game.physics.HazardSpinnerElement
import com.example.game.physics.JumpPadElement
import com.example.game.physics.LevelConfig
import com.example.game.physics.PlatformElement
import com.example.game.physics.RampElement
import com.example.game.physics.SpeedPadElement
import com.example.game.physics.TrackElement
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Renderer3D {

    private val ballRenderer = BallRenderer()
    val particleSystem = ParticleSystem()

    private sealed class RenderItem(val depth: Float) {
        abstract fun render(scope: DrawScope, camera: Camera3D, w: Float, h: Float)

        class PlatformItem(val el: PlatformElement, depth: Float) : RenderItem(depth) {
            override fun render(scope: DrawScope, camera: Camera3D, w: Float, h: Float) {
                drawPlatform(scope, camera, el, w, h)
            }
        }

        class RampItem(val el: RampElement, depth: Float) : RenderItem(depth) {
            override fun render(scope: DrawScope, camera: Camera3D, w: Float, h: Float) {
                drawRamp(scope, camera, el, w, h)
            }
        }

        class SpeedPadItem(val el: SpeedPadElement, depth: Float, val animTime: Float) : RenderItem(depth) {
            override fun render(scope: DrawScope, camera: Camera3D, w: Float, h: Float) {
                drawSpeedPad(scope, camera, el, animTime, w, h)
            }
        }

        class JumpPadItem(val el: JumpPadElement, depth: Float, val animTime: Float) : RenderItem(depth) {
            override fun render(scope: DrawScope, camera: Camera3D, w: Float, h: Float) {
                drawJumpPad(scope, camera, el, animTime, w, h)
            }
        }

        class BumperItem(val el: BumperElement, depth: Float) : RenderItem(depth) {
            override fun render(scope: DrawScope, camera: Camera3D, w: Float, h: Float) {
                drawBumper(scope, camera, el, w, h)
            }
        }

        class HazardItem(val el: HazardSpinnerElement, depth: Float) : RenderItem(depth) {
            override fun render(scope: DrawScope, camera: Camera3D, w: Float, h: Float) {
                drawHazard(scope, camera, el, w, h)
            }
        }

        class GemItem(val el: GemElement, depth: Float) : RenderItem(depth) {
            override fun render(scope: DrawScope, camera: Camera3D, w: Float, h: Float) {
                drawGem(scope, camera, el, w, h)
            }
        }

        class CheckpointItem(val el: CheckpointElement, depth: Float, val animTime: Float) : RenderItem(depth) {
            override fun render(scope: DrawScope, camera: Camera3D, w: Float, h: Float) {
                drawCheckpoint(scope, camera, el, animTime, w, h)
            }
        }

        class PortalItem(val el: GoalPortalElement, depth: Float, val animTime: Float) : RenderItem(depth) {
            override fun render(scope: DrawScope, camera: Camera3D, w: Float, h: Float) {
                drawGoalPortal(scope, camera, el, animTime, w, h)
            }
        }

        class BallItem(
            val ballPos: Vec3,
            val radius: Float,
            val orientation: Quaternion,
            val skin: BallSkin,
            val speedFraction: Float,
            val isGrounded: Boolean,
            val renderer: BallRenderer,
            depth: Float
        ) : RenderItem(depth) {
            override fun render(scope: DrawScope, camera: Camera3D, w: Float, h: Float) {
                renderer.renderBall(
                    scope = scope,
                    camera = camera,
                    ballPos = ballPos,
                    ballRadius = radius,
                    ballOrientation = orientation,
                    skin = skin,
                    speedFraction = speedFraction,
                    isGrounded = isGrounded,
                    screenWidth = w,
                    screenHeight = h
                )
            }
        }
    }

    fun renderScene(
        scope: DrawScope,
        camera: Camera3D,
        level: LevelConfig,
        elements: List<TrackElement>,
        ballPos: Vec3,
        ballRadius: Float,
        ballOrientation: Quaternion,
        skin: BallSkin,
        speedFraction: Float,
        isGrounded: Boolean,
        animTime: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        // 1. Draw Sky Backdrop & Infinite Horizon Grid
        drawSky(scope, level, screenWidth, screenHeight)

        // 2. Collect and Depth-Sort 3D Render Items
        val items = ArrayList<RenderItem>(elements.size + 2)

        for (el in elements) {
            val d = (el.position - camera.position).length()
            when (el) {
                is PlatformElement -> items.add(RenderItem.PlatformItem(el, d))
                is RampElement -> items.add(RenderItem.RampItem(el, d))
                is SpeedPadElement -> items.add(RenderItem.SpeedPadItem(el, d, animTime))
                is JumpPadElement -> items.add(RenderItem.JumpPadItem(el, d, animTime))
                is BumperElement -> items.add(RenderItem.BumperItem(el, d))
                is HazardSpinnerElement -> items.add(RenderItem.HazardItem(el, d))
                is GemElement -> if (!el.collected) items.add(RenderItem.GemItem(el, d))
                is CheckpointElement -> items.add(RenderItem.CheckpointItem(el, d, animTime))
                is GoalPortalElement -> items.add(RenderItem.PortalItem(el, d, animTime))
            }
        }

        // Add ball item
        val ballDepth = (ballPos - camera.position).length()
        items.add(
            RenderItem.BallItem(
                ballPos = ballPos,
                radius = ballRadius,
                orientation = ballOrientation,
                skin = skin,
                speedFraction = speedFraction,
                isGrounded = isGrounded,
                renderer = ballRenderer,
                depth = ballDepth
            )
        )

        // Sort descending (painter's algorithm: farthest rendered first)
        items.sortByDescending { it.depth }

        // Render sorted 3D items
        for (item in items) {
            item.render(scope, camera, screenWidth, screenHeight)
        }

        // 3. Render Particles on top
        particleSystem.render(scope, camera, screenWidth, screenHeight)
    }

    private fun drawSky(scope: DrawScope, level: LevelConfig, w: Float, h: Float) {
        // Gradient sky
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(level.skyColorTop, level.skyColorBottom),
                startY = 0f,
                endY = h
            ),
            size = Size(w, h)
        )

        // Distant horizon glow band
        val horizonY = h * 0.45f
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    level.gridColor.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                startY = horizonY - 60f,
                endY = horizonY + 80f
            ),
            topLeft = Offset(0f, horizonY - 60f),
            size = Size(w, 140f)
        )
    }

    companion object {
        private fun drawPlatform(scope: DrawScope, camera: Camera3D, el: PlatformElement, w: Float, h: Float) {
            val currPos = el.position + (el.moveAxis * (sin(el.currentOffset) * el.moveRange))
            val hw = el.size.x * 0.5f
            val hh = el.size.y * 0.5f
            val hl = el.size.z * 0.5f

            // 8 Box Vertices
            // Top face: v4, v5, v6, v7
            val p000 = camera.project(currPos + Vec3(-hw, -hh, -hl), w, h)
            val p100 = camera.project(currPos + Vec3(hw, -hh, -hl), w, h)
            val p101 = camera.project(currPos + Vec3(hw, -hh, hl), w, h)
            val p001 = camera.project(currPos + Vec3(-hw, -hh, hl), w, h)

            val p010 = camera.project(currPos + Vec3(-hw, hh, -hl), w, h)
            val p110 = camera.project(currPos + Vec3(hw, hh, -hl), w, h)
            val p111 = camera.project(currPos + Vec3(hw, hh, hl), w, h)
            val p011 = camera.project(currPos + Vec3(-hw, hh, hl), w, h)

            // Front face (towards -Z if camera is behind, or towards +Z)
            // Draw side bevels (darker shaded)
            val sideColor = el.color.copy(alpha = 0.95f)
            val darkSideColor = Color(
                red = el.color.red * 0.6f,
                green = el.color.green * 0.6f,
                blue = el.color.blue * 0.6f,
                alpha = 1.0f
            )

            // Bottom front face
            if (p011.isVisible && p111.isVisible && p101.isVisible && p001.isVisible) {
                val frontPath = Path().apply {
                    moveTo(p011.screenX, p011.screenY)
                    lineTo(p111.screenX, p111.screenY)
                    lineTo(p101.screenX, p101.screenY)
                    lineTo(p001.screenX, p001.screenY)
                    close()
                }
                scope.drawPath(frontPath, darkSideColor)
            }

            // Left/Right side face
            if (camera.position.x < currPos.x) {
                // Left face visible
                if (p010.isVisible && p011.isVisible && p001.isVisible && p000.isVisible) {
                    val leftPath = Path().apply {
                        moveTo(p010.screenX, p010.screenY)
                        lineTo(p011.screenX, p011.screenY)
                        lineTo(p001.screenX, p001.screenY)
                        lineTo(p000.screenX, p000.screenY)
                        close()
                    }
                    scope.drawPath(leftPath, sideColor)
                }
            } else {
                // Right face visible
                if (p110.isVisible && p111.isVisible && p101.isVisible && p100.isVisible) {
                    val rightPath = Path().apply {
                        moveTo(p110.screenX, p110.screenY)
                        lineTo(p111.screenX, p111.screenY)
                        lineTo(p101.screenX, p101.screenY)
                        lineTo(p100.screenX, p100.screenY)
                        close()
                    }
                    scope.drawPath(rightPath, sideColor)
                }
            }

            // Top Face (Platform rolling surface)
            if (p010.isVisible && p110.isVisible && p111.isVisible && p011.isVisible) {
                val topPath = Path().apply {
                    moveTo(p010.screenX, p010.screenY)
                    lineTo(p110.screenX, p110.screenY)
                    lineTo(p111.screenX, p111.screenY)
                    lineTo(p011.screenX, p011.screenY)
                    close()
                }
                // Fill surface
                scope.drawPath(topPath, el.color)

                // Neon edge glow border
                scope.drawPath(
                    path = topPath,
                    color = el.borderColor,
                    style = Stroke(width = 2.5f)
                )

                // Inner track centerline guide
                val midStart = camera.project(currPos + Vec3(0f, hh + 0.02f, -hl), w, h)
                val midEnd = camera.project(currPos + Vec3(0f, hh + 0.02f, hl), w, h)
                if (midStart.isVisible && midEnd.isVisible) {
                    scope.drawLine(
                        color = el.borderColor.copy(alpha = 0.35f),
                        start = Offset(midStart.screenX, midStart.screenY),
                        end = Offset(midEnd.screenX, midEnd.screenY),
                        strokeWidth = 2.0f
                    )
                }
            }
        }

        private fun drawRamp(scope: DrawScope, camera: Camera3D, el: RampElement, w: Float, h: Float) {
            val hw = el.size.x * 0.5f
            val hl = el.size.z * 0.5f
            val y0 = el.position.y - el.elevationDeltaY * 0.5f
            val y1 = el.position.y + el.elevationDeltaY * 0.5f

            val p00 = camera.project(Vec3(el.position.x - hw, y0, el.position.z - hl), w, h)
            val p10 = camera.project(Vec3(el.position.x + hw, y0, el.position.z - hl), w, h)
            val p11 = camera.project(Vec3(el.position.x + hw, y1, el.position.z + hl), w, h)
            val p01 = camera.project(Vec3(el.position.x - hw, y1, el.position.z + hl), w, h)

            if (p00.isVisible && p10.isVisible && p11.isVisible && p01.isVisible) {
                val path = Path().apply {
                    moveTo(p00.screenX, p00.screenY)
                    lineTo(p10.screenX, p10.screenY)
                    lineTo(p11.screenX, p11.screenY)
                    lineTo(p01.screenX, p01.screenY)
                    close()
                }
                scope.drawPath(path, el.color)
                scope.drawPath(path, el.accentColor, style = Stroke(width = 3f))

                // Ramp chevron guides pointing up slope
                val mid0 = camera.project(Vec3(el.position.x, y0 + 0.05f, el.position.z - hl * 0.5f), w, h)
                val mid1 = camera.project(Vec3(el.position.x, (y0 + y1) * 0.5f + 0.05f, el.position.z), w, h)
                val mid2 = camera.project(Vec3(el.position.x, y1 + 0.05f, el.position.z + hl * 0.5f), w, h)
                if (mid0.isVisible && mid1.isVisible && mid2.isVisible) {
                    val chevronColor = el.accentColor.copy(alpha = 0.7f)
                    scope.drawLine(
                        chevronColor,
                        Offset(mid0.screenX - 12f, mid0.screenY + 6f),
                        Offset(mid0.screenX, mid0.screenY - 6f),
                        strokeWidth = 3f
                    )
                    scope.drawLine(
                        chevronColor,
                        Offset(mid0.screenX, mid0.screenY - 6f),
                        Offset(mid0.screenX + 12f, mid0.screenY + 6f),
                        strokeWidth = 3f
                    )

                    scope.drawLine(
                        chevronColor,
                        Offset(mid1.screenX - 14f, mid1.screenY + 7f),
                        Offset(mid1.screenX, mid1.screenY - 7f),
                        strokeWidth = 3f
                    )
                    scope.drawLine(
                        chevronColor,
                        Offset(mid1.screenX, mid1.screenY - 7f),
                        Offset(mid1.screenX + 14f, mid1.screenY + 7f),
                        strokeWidth = 3f
                    )
                }
            }
        }

        private fun drawSpeedPad(scope: DrawScope, camera: Camera3D, el: SpeedPadElement, animTime: Float, w: Float, h: Float) {
            val proj = camera.project(el.position, w, h)
            if (!proj.isVisible) return

            val size = camera.projectRadius(2.2f, proj.depth)
            if (size < 4f) return

            // Glowing speed plate
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.7f), Color.Transparent),
                    center = Offset(proj.screenX, proj.screenY),
                    radius = size
                ),
                radius = size,
                center = Offset(proj.screenX, proj.screenY)
            )

            // Animated pulsing chevrons moving forward along pad
            val pulse = (animTime * 3f) % 1f
            for (step in 0..2) {
                val zOffset = ((step / 3f + pulse) % 1f - 0.5f) * 2.0f
                val chevronWorld = el.position + Vec3(0f, 0.05f, zOffset)
                val cProj = camera.project(chevronWorld, w, h)
                if (cProj.isVisible) {
                    val cw = camera.projectRadius(0.8f, cProj.depth)
                    val ch = camera.projectRadius(0.4f, cProj.depth)
                    val chevronAlpha = (1f - (zOffset + 1f) * 0.5f).coerceIn(0.2f, 1f)

                    val path = Path().apply {
                        moveTo(cProj.screenX - cw, cProj.screenY + ch)
                        lineTo(cProj.screenX, cProj.screenY - ch)
                        lineTo(cProj.screenX + cw, cProj.screenY + ch)
                    }
                    scope.drawPath(path, Color(0xFF38BDF8).copy(alpha = chevronAlpha), style = Stroke(width = 3.5f))
                }
            }
        }

        private fun drawJumpPad(scope: DrawScope, camera: Camera3D, el: JumpPadElement, animTime: Float, w: Float, h: Float) {
            val proj = camera.project(el.position, w, h)
            if (!proj.isVisible) return

            val radius = camera.projectRadius(1.6f, proj.depth)
            if (radius < 4f) return

            // Glowing launch ring
            val pulse = (sin(animTime * 6f) * 0.2f + 0.8f)
            scope.drawCircle(
                color = Color(0xFFA855F7).copy(alpha = 0.35f),
                radius = radius * pulse,
                center = Offset(proj.screenX, proj.screenY)
            )
            scope.drawCircle(
                color = Color(0xFFF472B6),
                radius = radius * 0.75f,
                center = Offset(proj.screenX, proj.screenY),
                style = Stroke(width = 3f)
            )
            // Inner yellow spring core
            scope.drawCircle(
                color = Color(0xFFFACC15),
                radius = radius * 0.35f,
                center = Offset(proj.screenX, proj.screenY)
            )
        }

        private fun drawBumper(scope: DrawScope, camera: Camera3D, el: BumperElement, w: Float, h: Float) {
            val baseProj = camera.project(el.position, w, h)
            val topProj = camera.project(el.position + Vec3(0f, 1.2f, 0f), w, h)
            if (!baseProj.isVisible && !topProj.isVisible) return

            val r = camera.projectRadius(el.radius, topProj.depth)
            if (r < 3f) return

            // Bumper body pillar
            scope.drawLine(
                color = Color(0xFF475569),
                start = Offset(baseProj.screenX, baseProj.screenY),
                end = Offset(topProj.screenX, topProj.screenY),
                strokeWidth = r * 1.6f
            )

            // Bouncy dome top
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF43F5E), Color(0xFF9F1239)),
                    center = Offset(topProj.screenX - r * 0.2f, topProj.screenY - r * 0.2f),
                    radius = r
                ),
                radius = r,
                center = Offset(topProj.screenX, topProj.screenY)
            )
            scope.drawCircle(
                color = Color(0xFFFDA4AF),
                radius = r,
                center = Offset(topProj.screenX, topProj.screenY),
                style = Stroke(width = 2.5f)
            )
        }

        private fun drawHazard(scope: DrawScope, camera: Camera3D, el: HazardSpinnerElement, w: Float, h: Float) {
            val centerProj = camera.project(el.position, w, h)
            val angle = el.currentAngle
            val dir = Vec3(cos(angle), 0f, sin(angle))

            val p1 = camera.project(el.position - (dir * el.length), w, h)
            val p2 = camera.project(el.position + (dir * el.length), w, h)

            if (p1.isVisible || p2.isVisible) {
                // Hazard Laser Blade
                scope.drawLine(
                    color = Color(0xFFEF4444).copy(alpha = 0.4f),
                    start = Offset(p1.screenX, p1.screenY),
                    end = Offset(p2.screenX, p2.screenY),
                    strokeWidth = 9f
                )
                scope.drawLine(
                    color = Color(0xFFF87171),
                    start = Offset(p1.screenX, p1.screenY),
                    end = Offset(p2.screenX, p2.screenY),
                    strokeWidth = 4f
                )
                scope.drawLine(
                    color = Color.White,
                    start = Offset(p1.screenX, p1.screenY),
                    end = Offset(p2.screenX, p2.screenY),
                    strokeWidth = 1.5f
                )
            }

            // Pivot hub
            if (centerProj.isVisible) {
                val r = camera.projectRadius(0.5f, centerProj.depth)
                scope.drawCircle(
                    color = Color(0xFF18181B),
                    radius = r.coerceAtLeast(4f),
                    center = Offset(centerProj.screenX, centerProj.screenY)
                )
                scope.drawCircle(
                    color = Color(0xFFEF4444),
                    radius = (r * 0.6f).coerceAtLeast(2f),
                    center = Offset(centerProj.screenX, centerProj.screenY)
                )
            }
        }

        private fun drawGem(scope: DrawScope, camera: Camera3D, el: GemElement, w: Float, h: Float) {
            // Hover bobbing + 3D rotation
            val bobY = sin(el.animOffset) * 0.2f
            val pos = el.position + Vec3(0f, bobY, 0f)
            val proj = camera.project(pos, w, h)
            if (!proj.isVisible) return

            val r = camera.projectRadius(0.55f, proj.depth)
            if (r < 2f) return

            val rot = el.animOffset * 1.5f
            val topP = camera.project(pos + Vec3(0f, 0.7f, 0f), w, h)
            val botP = camera.project(pos + Vec3(0f, -0.7f, 0f), w, h)

            // 4 equatorial diamond vertices
            val d0 = camera.project(pos + Vec3(cos(rot) * 0.5f, 0f, sin(rot) * 0.5f), w, h)
            val d1 = camera.project(pos + Vec3(cos(rot + PI.toFloat() * 0.5f) * 0.5f, 0f, sin(rot + PI.toFloat() * 0.5f) * 0.5f), w, h)
            val d2 = camera.project(pos + Vec3(cos(rot + PI.toFloat()) * 0.5f, 0f, sin(rot + PI.toFloat()) * 0.5f), w, h)
            val d3 = camera.project(pos + Vec3(cos(rot + PI.toFloat() * 1.5f) * 0.5f, 0f, sin(rot + PI.toFloat() * 1.5f) * 0.5f), w, h)

            val pts = listOf(d0, d1, d2, d3)
            val gemColors = listOf(
                Color(0xFF38BDF8),
                Color(0xFF0284C7),
                Color(0xFF0369A1),
                Color(0xFF7DD3FC)
            )

            // Draw upper triangular facets
            for (i in 0..3) {
                val next = (i + 1) % 4
                val pCurr = pts[i]
                val pNext = pts[next]
                if (topP.isVisible && pCurr.isVisible && pNext.isVisible) {
                    val tri = Path().apply {
                        moveTo(topP.screenX, topP.screenY)
                        lineTo(pCurr.screenX, pCurr.screenY)
                        lineTo(pNext.screenX, pNext.screenY)
                        close()
                    }
                    scope.drawPath(tri, gemColors[i])
                }
                // Lower triangular facets
                if (botP.isVisible && pCurr.isVisible && pNext.isVisible) {
                    val tri = Path().apply {
                        moveTo(botP.screenX, botP.screenY)
                        lineTo(pCurr.screenX, pCurr.screenY)
                        lineTo(pNext.screenX, pNext.screenY)
                        close()
                    }
                    scope.drawPath(tri, gemColors[i].copy(alpha = 0.85f))
                }
            }

            // Central spark glint
            scope.drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = (r * 0.25f).coerceAtLeast(1.5f),
                center = Offset(proj.screenX, proj.screenY - r * 0.2f)
            )
        }

        private fun drawCheckpoint(scope: DrawScope, camera: Camera3D, el: CheckpointElement, animTime: Float, w: Float, h: Float) {
            val proj = camera.project(el.position, w, h)
            if (!proj.isVisible) return

            val r = camera.projectRadius(2.0f, proj.depth)
            val color = if (el.activated) Color(0xFF10B981) else Color(0xFF38BDF8)

            // Ground ring
            scope.drawCircle(
                color = color.copy(alpha = 0.4f),
                radius = r,
                center = Offset(proj.screenX, proj.screenY),
                style = Stroke(width = 3.5f)
            )

            // Vertical light beam if activated
            if (el.activated) {
                val beamTop = camera.project(el.position + Vec3(0f, 6.0f, 0f), w, h)
                if (beamTop.isVisible) {
                    scope.drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, color.copy(alpha = 0.6f)),
                            startY = beamTop.screenY,
                            endY = proj.screenY
                        ),
                        start = Offset(beamTop.screenX, beamTop.screenY),
                        end = Offset(proj.screenX, proj.screenY),
                        strokeWidth = (r * 0.4f).coerceAtLeast(4f)
                    )
                }
            }
        }

        private fun drawGoalPortal(scope: DrawScope, camera: Camera3D, el: GoalPortalElement, animTime: Float, w: Float, h: Float) {
            val proj = camera.project(el.position, w, h)
            if (!proj.isVisible) return

            val r = camera.projectRadius(el.radius, proj.depth)
            if (r < 4f) return

            // Swirling cosmic portal vortex
            val pulse = sin(animTime * 4f) * 0.15f + 0.85f
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFDE047),
                        Color(0xFFA855F7),
                        Color(0xFF3B82F6),
                        Color.Transparent
                    ),
                    center = Offset(proj.screenX, proj.screenY),
                    radius = r * 1.3f * pulse
                ),
                radius = r * 1.3f * pulse,
                center = Offset(proj.screenX, proj.screenY)
            )

            // Rotating vortex ring particles
            val count = 12
            for (i in 0 until count) {
                val angle = (i * 2 * PI / count + animTime * 2.5f).toFloat()
                val px = el.position.x + cos(angle) * el.radius * 0.95f
                val py = el.position.y + sin(angle) * el.radius * 0.95f
                val pz = el.position.z

                val partProj = camera.project(Vec3(px, py, pz), w, h)
                if (partProj.isVisible) {
                    scope.drawCircle(
                        color = Color.White,
                        radius = (r * 0.12f).coerceAtLeast(2f),
                        center = Offset(partProj.screenX, partProj.screenY)
                    )
                }
            }
        }
    }
}
