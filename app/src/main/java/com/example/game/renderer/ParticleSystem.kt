package com.example.game.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.game.math.Camera3D
import com.example.game.math.Vec3
import kotlin.random.Random

data class Particle(
    var position: Vec3,
    var velocity: Vec3,
    var color: Color,
    var size: Float,
    var life: Float, // 1.0 down to 0.0
    val maxLife: Float
)

class ParticleSystem {
    private val particles = ArrayList<Particle>(200)

    fun spawnDust(pos: Vec3, ballVel: Vec3) {
        if (particles.size > 150) return
        val vel = Vec3(
            (Random.nextFloat() - 0.5f) * 1.5f - ballVel.x * 0.15f,
            Random.nextFloat() * 1.0f + 0.2f,
            (Random.nextFloat() - 0.5f) * 1.5f - ballVel.z * 0.15f
        )
        particles.add(
            Particle(
                position = pos + Vec3((Random.nextFloat() - 0.5f) * 0.4f, -0.4f, (Random.nextFloat() - 0.5f) * 0.4f),
                velocity = vel,
                color = Color.White.copy(alpha = 0.45f),
                size = Random.nextFloat() * 4f + 3f,
                life = 1f,
                maxLife = 0.45f
            )
        )
    }

    fun spawnGemSparkles(pos: Vec3, gemColor: Color = Color(0xFF38BDF8)) {
        for (i in 0..16) {
            val vel = Vec3(
                (Random.nextFloat() - 0.5f) * 5f,
                Random.nextFloat() * 5f + 1f,
                (Random.nextFloat() - 0.5f) * 5f
            )
            particles.add(
                Particle(
                    position = pos,
                    velocity = vel,
                    color = gemColor,
                    size = Random.nextFloat() * 6f + 4f,
                    life = 1f,
                    maxLife = 0.7f
                )
            )
        }
    }

    fun spawnSpeedTrail(pos: Vec3, trailColor: Color = Color(0xFF06B6D4)) {
        if (particles.size > 180) return
        particles.add(
            Particle(
                position = pos + Vec3((Random.nextFloat() - 0.5f) * 0.5f, (Random.nextFloat() - 0.5f) * 0.3f, (Random.nextFloat() - 0.5f) * 0.5f),
                velocity = Vec3((Random.nextFloat() - 0.5f) * 0.5f, 0.5f, (Random.nextFloat() - 0.5f) * 0.5f),
                color = trailColor,
                size = Random.nextFloat() * 5f + 4f,
                life = 1f,
                maxLife = 0.4f
            )
        )
    }

    fun spawnVictoryConfetti(portalPos: Vec3) {
        val colors = listOf(
            Color(0xFFF59E0B),
            Color(0xFF38BDF8),
            Color(0xFFA855F7),
            Color(0xFF10B981),
            Color(0xFFEC4899),
            Color(0xFFF43F5E)
        )
        for (i in 0..40) {
            val vel = Vec3(
                (Random.nextFloat() - 0.5f) * 8f,
                Random.nextFloat() * 8f + 4f,
                (Random.nextFloat() - 0.5f) * 8f
            )
            particles.add(
                Particle(
                    position = portalPos,
                    velocity = vel,
                    color = colors[Random.nextInt(colors.size)],
                    size = Random.nextFloat() * 7f + 5f,
                    life = 1f,
                    maxLife = 1.2f
                )
            )
        }
    }

    fun update(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life -= dt / p.maxLife
            if (p.life <= 0f) {
                iter.remove()
                continue
            }
            p.position = p.position + (p.velocity * dt)
            p.velocity = Vec3(p.velocity.x * 0.95f, p.velocity.y - 9.8f * dt, p.velocity.z * 0.95f)
        }
    }

    fun render(scope: DrawScope, camera: Camera3D, screenWidth: Float, screenHeight: Float) {
        for (p in particles) {
            val proj = camera.project(p.position, screenWidth, screenHeight)
            if (proj.isVisible && proj.depth > 0.3f) {
                val scale = p.life.coerceIn(0f, 1f)
                val alpha = (scale * p.color.alpha).coerceIn(0f, 1f)
                val radius = (p.size * scale) / (proj.depth * 0.15f + 0.85f)
                scope.drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = radius.coerceAtLeast(1.5f),
                    center = Offset(proj.screenX, proj.screenY)
                )
            }
        }
    }

    fun clear() {
        particles.clear()
    }
}
