package com.example.game.physics

import androidx.compose.ui.graphics.Color
import com.example.game.math.Vec3

enum class ElementType {
    PLATFORM,
    RAMP,
    SPEED_PAD,
    JUMP_PAD,
    BUMPER,
    HAZARD_SPINNER,
    GEM,
    CHECKPOINT,
    GOAL_PORTAL
}

sealed class TrackElement(
    val type: ElementType,
    open var position: Vec3
)

data class PlatformElement(
    override var position: Vec3,
    val size: Vec3, // width (X), height (Y), length (Z)
    val color: Color = Color(0xFF1E293B),
    val borderColor: Color = Color(0xFF38BDF8),
    val friction: Float = 0.96f,
    val moveAxis: Vec3 = Vec3.ZERO,
    val moveRange: Float = 0f,
    val moveSpeed: Float = 0f,
    var currentOffset: Float = 0f
) : TrackElement(ElementType.PLATFORM, position) {
    fun getAABB(): AABB {
        val currPos = position + (moveAxis * currentOffset)
        val halfW = size.x * 0.5f
        val halfH = size.y * 0.5f
        val halfL = size.z * 0.5f
        return AABB(
            min = Vec3(currPos.x - halfW, currPos.y - halfH, currPos.z - halfL),
            max = Vec3(currPos.x + halfW, currPos.y + halfH, currPos.z + halfL)
        )
    }
}

data class RampElement(
    override var position: Vec3,
    val size: Vec3, // width (X), height (Y), length (Z)
    val elevationDeltaY: Float, // +Y rises towards +Z, -Y descends towards +Z
    val color: Color = Color(0xFF27272A),
    val accentColor: Color = Color(0xFFA855F7)
) : TrackElement(ElementType.RAMP, position)

data class SpeedPadElement(
    override var position: Vec3,
    val direction: Vec3 = Vec3(0f, 0f, 1f),
    val boostForce: Float = 22f
) : TrackElement(ElementType.SPEED_PAD, position)

data class JumpPadElement(
    override var position: Vec3,
    val upwardImpulse: Float = 14f,
    val forwardImpulse: Float = 6f
) : TrackElement(ElementType.JUMP_PAD, position)

data class BumperElement(
    override var position: Vec3,
    val radius: Float = 1.2f,
    val bounceForce: Float = 18f
) : TrackElement(ElementType.BUMPER, position)

data class HazardSpinnerElement(
    override var position: Vec3,
    val length: Float = 4.0f,
    val rotationSpeed: Float = 2.5f,
    var currentAngle: Float = 0f
) : TrackElement(ElementType.HAZARD_SPINNER, position)

data class GemElement(
    val id: Int,
    override var position: Vec3,
    var collected: Boolean = false,
    var animOffset: Float = 0f
) : TrackElement(ElementType.GEM, position)

data class CheckpointElement(
    val id: Int,
    override var position: Vec3,
    var activated: Boolean = false
) : TrackElement(ElementType.CHECKPOINT, position)

data class GoalPortalElement(
    override var position: Vec3,
    val radius: Float = 2.0f
) : TrackElement(ElementType.GOAL_PORTAL, position)

data class AABB(
    val min: Vec3,
    val max: Vec3
) {
    fun contains(p: Vec3): Boolean =
        p.x in min.x..max.x && p.y in min.y..max.y && p.z in min.z..max.z

    fun intersectsSphere(center: Vec3, radius: Float): Boolean {
        var dmin = 0f
        if (center.x < min.x) {
            val d = center.x - min.x
            dmin += d * d
        } else if (center.x > max.x) {
            val d = center.x - max.x
            dmin += d * d
        }

        if (center.y < min.y) {
            val d = center.y - min.y
            dmin += d * d
        } else if (center.y > max.y) {
            val d = center.y - max.y
            dmin += d * d
        }

        if (center.z < min.z) {
            val d = center.z - min.z
            dmin += d * d
        } else if (center.z > max.z) {
            val d = center.z - max.z
            dmin += d * d
        }

        return dmin <= radius * radius
    }
}
