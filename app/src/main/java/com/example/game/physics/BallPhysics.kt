package com.example.game.physics

import com.example.game.math.Quaternion
import com.example.game.math.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CollisionEvent(
    val type: EventType,
    val element: TrackElement? = null,
    val intensity: Float = 1f
) {
    enum class EventType {
        GROUND_HIT,
        WALL_BOUNCE,
        BUMPER_HIT,
        SPEED_BOOST,
        JUMP_PAD,
        GEM_COLLECTED,
        CHECKPOINT_REACHED,
        HAZARD_HIT,
        GOAL_REACHED,
        FELL_OFF
    }
}

class BallPhysics(
    var position: Vec3 = Vec3(0f, 2f, 0f),
    val radius: Float = 0.65f
) {
    var velocity: Vec3 = Vec3.ZERO
    var orientation: Quaternion = Quaternion.IDENTITY
    var isGrounded: Boolean = false
    var currentFriction: Float = 0.965f
    var speedMultiplier: Float = 1.0f
    private var boostTimer: Float = 0f

    val speed: Float
        get() = velocity.length()

    val horizontalSpeed: Float
        get() = sqrt(velocity.x * velocity.x + velocity.z * velocity.z)

    var lastCheckpointPos: Vec3 = Vec3(0f, 2f, 0f)

    fun reset(startPos: Vec3) {
        position = startPos
        velocity = Vec3.ZERO
        orientation = Quaternion.IDENTITY
        isGrounded = false
        boostTimer = 0f
        speedMultiplier = 1f
        lastCheckpointPos = startPos
    }

    fun respawnAtCheckpoint() {
        position = lastCheckpointPos + Vec3(0f, 1.5f, 0f)
        velocity = Vec3.ZERO
        isGrounded = false
    }

    /**
     * Physics update step
     * @param dt delta time in seconds
     * @param inputForce (X, Z) steering force vector from invisible thumb control (camera-relative)
     * @param sensitivity user sensitivity multiplier
     * @param elements track elements
     */
    fun update(
        dt: Float,
        inputForce: Vec3,
        sensitivity: Float,
        elements: List<TrackElement>,
        onEvent: (CollisionEvent) -> Unit
    ) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // Handle boost timer
        if (boostTimer > 0f) {
            boostTimer -= clampedDt
            if (boostTimer <= 0f) {
                speedMultiplier = 1.0f
            }
        }

        // Apply input drive force
        val driveForce = if (isGrounded) 32f else 12f
        val accel = inputForce * (driveForce * sensitivity * speedMultiplier)
        velocity = velocity + (accel * clampedDt)

        // Gravity
        val gravity = -24.0f
        velocity = Vec3(velocity.x, velocity.y + gravity * clampedDt, velocity.z)

        // Max speed cap
        val maxHorizontal = 24.0f * speedMultiplier
        val hSpeed = horizontalSpeed
        if (hSpeed > maxHorizontal) {
            val scale = maxHorizontal / hSpeed
            velocity = Vec3(velocity.x * scale, velocity.y, velocity.z * scale)
        }

        // Update moving platforms and obstacles
        for (el in elements) {
            when (el) {
                is PlatformElement -> {
                    if (el.moveRange > 0f && el.moveSpeed > 0f) {
                        el.currentOffset += el.moveSpeed * clampedDt
                        // Oscillate
                    }
                }
                is HazardSpinnerElement -> {
                    el.currentAngle += el.rotationSpeed * clampedDt
                }
                is GemElement -> {
                    el.animOffset += clampedDt * 3.5f
                }
                else -> {}
            }
        }

        // Tentative new position
        var newPos = position + (velocity * clampedDt)
        var groundedThisFrame = false
        currentFriction = 0.965f

        // Platform & Surface collisions
        for (el in elements) {
            when (el) {
                is PlatformElement -> {
                    val currPlatformPos = el.position + (el.moveAxis * (sin(el.currentOffset) * el.moveRange))
                    val halfW = el.size.x * 0.5f
                    val halfH = el.size.y * 0.5f
                    val halfL = el.size.z * 0.5f

                    val minX = currPlatformPos.x - halfW
                    val maxX = currPlatformPos.x + halfW
                    val topY = currPlatformPos.y + halfH
                    val bottomY = currPlatformPos.y - halfH
                    val minZ = currPlatformPos.z - halfL
                    val maxZ = currPlatformPos.z + halfL

                    // Check if ball is horizontally within platform
                    val isWithinX = newPos.x >= minX - radius * 0.6f && newPos.x <= maxX + radius * 0.6f
                    val isWithinZ = newPos.z >= minZ - radius * 0.6f && newPos.z <= maxZ + radius * 0.6f

                    if (isWithinX && isWithinZ) {
                        // Landing on top surface
                        val targetTop = topY + radius
                        if (position.y >= topY + radius * 0.5f && newPos.y <= targetTop + 0.35f && velocity.y <= 0f) {
                            newPos = Vec3(newPos.x, targetTop, newPos.z)
                            if (velocity.y < -3.0f) {
                                onEvent(CollisionEvent(CollisionEvent.EventType.GROUND_HIT, el, -velocity.y / 15f))
                            }
                            velocity = Vec3(velocity.x, 0f, velocity.z)
                            groundedThisFrame = true
                            currentFriction = el.friction
                        }
                    }

                    // Side wall collisions
                    if (newPos.y < topY && newPos.y > bottomY) {
                        // Left/right wall bounce
                        if (newPos.z in minZ..maxZ) {
                            if (position.x <= minX && newPos.x >= minX - radius) {
                                newPos = Vec3(minX - radius, newPos.y, newPos.z)
                                velocity = Vec3(-velocity.x * 0.5f, velocity.y, velocity.z)
                                onEvent(CollisionEvent(CollisionEvent.EventType.WALL_BOUNCE, el, 0.5f))
                            } else if (position.x >= maxX && newPos.x <= maxX + radius) {
                                newPos = Vec3(maxX + radius, newPos.y, newPos.z)
                                velocity = Vec3(-velocity.x * 0.5f, velocity.y, velocity.z)
                                onEvent(CollisionEvent(CollisionEvent.EventType.WALL_BOUNCE, el, 0.5f))
                            }
                        }
                        // Front/back wall bounce
                        if (newPos.x in minX..maxX) {
                            if (position.z <= minZ && newPos.z >= minZ - radius) {
                                newPos = Vec3(newPos.x, newPos.y, minZ - radius)
                                velocity = Vec3(velocity.x, velocity.y, -velocity.z * 0.5f)
                                onEvent(CollisionEvent(CollisionEvent.EventType.WALL_BOUNCE, el, 0.5f))
                            } else if (position.z >= maxZ && newPos.z <= maxZ + radius) {
                                newPos = Vec3(newPos.x, newPos.y, maxZ + radius)
                                velocity = Vec3(velocity.x, velocity.y, -velocity.z * 0.5f)
                                onEvent(CollisionEvent(CollisionEvent.EventType.WALL_BOUNCE, el, 0.5f))
                            }
                        }
                    }
                }

                is RampElement -> {
                    // Ramp slopes along Z
                    val halfW = el.size.x * 0.5f
                    val halfL = el.size.z * 0.5f
                    val minX = el.position.x - halfW
                    val maxX = el.position.x + halfW
                    val minZ = el.position.z - halfL
                    val maxZ = el.position.z + halfL

                    if (newPos.x in minX..maxX && newPos.z in minZ..maxZ) {
                        val t = (newPos.z - minZ) / el.size.z
                        val surfaceY = el.position.y + (t - 0.5f) * el.elevationDeltaY + radius
                        if (position.y >= surfaceY - 0.4f && newPos.y <= surfaceY + 0.4f && velocity.y <= 1f) {
                            newPos = Vec3(newPos.x, surfaceY, newPos.z)
                            // Slope acceleration
                            val slopeAngleAccel = (el.elevationDeltaY / el.size.z) * -16.0f
                            velocity = Vec3(velocity.x, 0f, velocity.z + slopeAngleAccel * clampedDt)
                            groundedThisFrame = true
                            currentFriction = 0.98f
                        }
                    }
                }

                is SpeedPadElement -> {
                    if (newPos.distanceTo(el.position) < radius + 1.2f) {
                        velocity = velocity + (el.direction.normalized() * el.boostForce)
                        speedMultiplier = 1.45f
                        boostTimer = 2.0f
                        onEvent(CollisionEvent(CollisionEvent.EventType.SPEED_BOOST, el, 1.0f))
                    }
                }

                is JumpPadElement -> {
                    if (newPos.distanceTo(el.position) < radius + 1.2f && velocity.y <= 2f) {
                        velocity = Vec3(velocity.x, el.upwardImpulse, velocity.z + el.forwardImpulse)
                        groundedThisFrame = false
                        onEvent(CollisionEvent(CollisionEvent.EventType.JUMP_PAD, el, 1.0f))
                    }
                }

                is BumperElement -> {
                    val dist = Vec3(newPos.x - el.position.x, 0f, newPos.z - el.position.z).length()
                    if (dist < radius + el.radius && (newPos.y - el.position.y) in -0.5f..1.5f) {
                        val pushDir = Vec3(newPos.x - el.position.x, 0f, newPos.z - el.position.z).normalized()
                        velocity = pushDir * el.bounceForce + Vec3(0f, 4f, 0f)
                        newPos = el.position + (pushDir * (radius + el.radius + 0.1f))
                        onEvent(CollisionEvent(CollisionEvent.EventType.BUMPER_HIT, el, 1.0f))
                    }
                }

                is HazardSpinnerElement -> {
                    // Check segment collision from center
                    val rad = el.currentAngle
                    val bladeDir = Vec3(cos(rad), 0f, sin(rad))
                    val toBall = Vec3(newPos.x - el.position.x, 0f, newPos.z - el.position.z)
                    val proj = toBall.dot(bladeDir)
                    if (proj in -el.length..el.length) {
                        val closest = el.position + (bladeDir * proj)
                        if (newPos.distanceTo(closest) < radius + 0.35f && (newPos.y - el.position.y) in -0.4f..1.2f) {
                            val repel = (newPos - closest).normalized()
                            velocity = (repel * 16f) + Vec3(0f, 6f, 0f)
                            onEvent(CollisionEvent(CollisionEvent.EventType.HAZARD_HIT, el, 1.0f))
                        }
                    }
                }

                is GemElement -> {
                    if (!el.collected && newPos.distanceTo(el.position) < radius + 0.9f) {
                        el.collected = true
                        onEvent(CollisionEvent(CollisionEvent.EventType.GEM_COLLECTED, el, 1.0f))
                    }
                }

                is CheckpointElement -> {
                    if (!el.activated && newPos.distanceTo(el.position) < 2.0f) {
                        el.activated = true
                        lastCheckpointPos = el.position
                        onEvent(CollisionEvent(CollisionEvent.EventType.CHECKPOINT_REACHED, el, 1.0f))
                    }
                }

                is GoalPortalElement -> {
                    if (newPos.distanceTo(el.position) < el.radius + radius) {
                        onEvent(CollisionEvent(CollisionEvent.EventType.GOAL_REACHED, el, 1.0f))
                    }
                }
            }
        }

        // Fall into abyss check
        if (newPos.y < -12.0f) {
            onEvent(CollisionEvent(CollisionEvent.EventType.FELL_OFF, null, 1.0f))
            return
        }

        isGrounded = groundedThisFrame
        position = newPos

        // Surface friction
        if (isGrounded) {
            velocity = Vec3(velocity.x * currentFriction, velocity.y, velocity.z * currentFriction)
        } else {
            // Slight air drag
            velocity = Vec3(velocity.x * 0.992f, velocity.y, velocity.z * 0.992f)
        }

        // Realistic 3D rolling rotation
        val rollDist = horizontalSpeed * clampedDt
        if (rollDist > 0.0001f) {
            val rollAngle = rollDist / radius
            // Axis of rotation perpendicular to motion on ground: axis = UP x (vx, 0, vz) = (-vz, 0, vx)
            val rotAxis = Vec3(-velocity.z, 0f, velocity.x).normalized()
            val deltaRot = Quaternion.fromAxisAngle(rotAxis, rollAngle)
            orientation = (deltaRot * orientation).normalized()
        }
    }
}
