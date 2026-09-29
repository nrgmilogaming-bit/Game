package com.example.game.physics

import androidx.compose.ui.graphics.Color
import com.example.game.math.Vec3

data class LevelConfig(
    val id: Int,
    val name: String,
    val subtitle: String,
    val skyColorTop: Color,
    val skyColorBottom: Color,
    val gridColor: Color,
    val parTimeSeconds: Float,
    val startPosition: Vec3,
    val elements: List<TrackElement>
)

object LevelFactory {

    fun getLevel(id: Int): LevelConfig {
        return when (id) {
            1 -> createLevel1()
            2 -> createLevel2()
            3 -> createLevel3()
            4 -> createLevel4()
            5 -> createLevel5()
            else -> createLevel1()
        }
    }

    private fun createLevel1(): LevelConfig {
        val elements = mutableListOf<TrackElement>()
        var gemId = 0

        // Start platform
        elements.add(PlatformElement(Vec3(0f, 0f, 0f), Vec3(6f, 1f, 10f), Color(0xFF1E293B), Color(0xFF38BDF8)))

        // Bridge forward
        elements.add(PlatformElement(Vec3(0f, 0f, 15f), Vec3(4f, 1f, 16f), Color(0xFF1E293B), Color(0xFF38BDF8)))
        elements.add(GemElement(gemId++, Vec3(0f, 1.2f, 12f)))
        elements.add(GemElement(gemId++, Vec3(0f, 1.2f, 16f)))
        elements.add(GemElement(gemId++, Vec3(0f, 1.2f, 20f)))

        // Speed pad
        elements.add(SpeedPadElement(Vec3(0f, 0.6f, 24f), Vec3(0f, 0f, 1f), 18f))

        // Gentle left turn platform
        elements.add(PlatformElement(Vec3(4f, 0f, 34f), Vec3(12f, 1f, 8f), Color(0xFF1E293B), Color(0xFF38BDF8)))
        elements.add(GemElement(gemId++, Vec3(3f, 1.2f, 34f)))
        elements.add(GemElement(gemId++, Vec3(6f, 1.2f, 34f)))

        // Forward ramp up
        elements.add(RampElement(Vec3(8f, 1.5f, 46f), Vec3(4f, 1f, 14f), elevationDeltaY = 3f, Color(0xFF334155), Color(0xFF06B6D4)))
        elements.add(GemElement(gemId++, Vec3(8f, 3.2f, 46f)))

        // Upper platform
        elements.add(PlatformElement(Vec3(8f, 3f, 60f), Vec3(6f, 1f, 12f), Color(0xFF1E293B), Color(0xFF38BDF8)))
        elements.add(CheckpointElement(1, Vec3(8f, 3.6f, 56f)))
        elements.add(GemElement(gemId++, Vec3(8f, 4.2f, 60f)))

        // Jump pad over a gap!
        elements.add(JumpPadElement(Vec3(8f, 3.6f, 64f), upwardImpulse = 11f, forwardImpulse = 9f))

        // Landing platform
        elements.add(PlatformElement(Vec3(8f, 3f, 82f), Vec3(7f, 1f, 14f), Color(0xFF1E293B), Color(0xFF38BDF8)))
        elements.add(GemElement(gemId++, Vec3(8f, 4.2f, 80f)))
        elements.add(GemElement(gemId++, Vec3(8f, 4.2f, 84f)))

        // Goal Portal
        elements.add(GoalPortalElement(Vec3(8f, 4.5f, 87f), radius = 2.0f))

        return LevelConfig(
            id = 1,
            name = "Neon Valley",
            subtitle = "Master the rolling momentum",
            skyColorTop = Color(0xFF0F172A),
            skyColorBottom = Color(0xFF1E1B4B),
            gridColor = Color(0xFF38BDF8).copy(alpha = 0.25f),
            parTimeSeconds = 25f,
            startPosition = Vec3(0f, 1.5f, 0f),
            elements = elements
        )
    }

    private fun createLevel2(): LevelConfig {
        val elements = mutableListOf<TrackElement>()
        var gemId = 0

        // Start
        elements.add(PlatformElement(Vec3(0f, 0f, 0f), Vec3(6f, 1f, 8f), Color(0xFF0F172A), Color(0xFFA855F7)))
        elements.add(SpeedPadElement(Vec3(0f, 0.6f, 3f), Vec3(0f, 0f, 1f), 22f))

        // High speed straightway
        elements.add(PlatformElement(Vec3(0f, 0f, 18f), Vec3(4f, 1f, 24f), Color(0xFF18181B), Color(0xFFA855F7)))
        elements.add(GemElement(gemId++, Vec3(0f, 1.2f, 12f)))
        elements.add(GemElement(gemId++, Vec3(0f, 1.2f, 18f)))
        elements.add(GemElement(gemId++, Vec3(0f, 1.2f, 24f)))

        // Bumpers to navigate around
        elements.add(BumperElement(Vec3(-1.2f, 0.8f, 20f), radius = 0.9f))
        elements.add(BumperElement(Vec3(1.2f, 0.8f, 24f), radius = 0.9f))

        // Big jump launch ramp
        elements.add(RampElement(Vec3(0f, 1.5f, 36f), Vec3(5f, 1f, 10f), elevationDeltaY = 3.0f, Color(0xFF27272A), Color(0xFFEC4899)))
        elements.add(JumpPadElement(Vec3(0f, 3.2f, 40f), upwardImpulse = 14f, forwardImpulse = 12f))

        // Mid-air gems
        elements.add(GemElement(gemId++, Vec3(0f, 8f, 50f)))
        elements.add(GemElement(gemId++, Vec3(0f, 7f, 56f)))

        // Moving platform bridge!
        elements.add(
            PlatformElement(
                position = Vec3(0f, 2f, 66f),
                size = Vec3(5f, 1f, 10f),
                color = Color(0xFF27272A),
                borderColor = Color(0xFFEC4899),
                moveAxis = Vec3(1f, 0f, 0f),
                moveRange = 3.5f,
                moveSpeed = 1.8f
            )
        )
        elements.add(CheckpointElement(1, Vec3(0f, 2.8f, 66f)))

        // Narrow bridge to goal
        elements.add(PlatformElement(Vec3(0f, 2f, 82f), Vec3(3.5f, 1f, 18f), Color(0xFF18181B), Color(0xFFA855F7)))
        elements.add(GemElement(gemId++, Vec3(0f, 3.2f, 76f)))
        elements.add(GemElement(gemId++, Vec3(0f, 3.2f, 82f)))
        elements.add(GemElement(gemId++, Vec3(0f, 3.2f, 88f)))

        // Goal
        elements.add(PlatformElement(Vec3(0f, 2f, 96f), Vec3(7f, 1f, 8f), Color(0xFF0F172A), Color(0xFFA855F7)))
        elements.add(GoalPortalElement(Vec3(0f, 3.5f, 98f), radius = 2.0f))

        return LevelConfig(
            id = 2,
            name = "Skyline Leap",
            subtitle = "High altitude aerial jumps",
            skyColorTop = Color(0xFF1E1B4B),
            skyColorBottom = Color(0xFF311042),
            gridColor = Color(0xFFA855F7).copy(alpha = 0.25f),
            parTimeSeconds = 30f,
            startPosition = Vec3(0f, 1.5f, 0f),
            elements = elements
        )
    }

    private fun createLevel3(): LevelConfig {
        val elements = mutableListOf<TrackElement>()
        var gemId = 0

        // Start
        elements.add(PlatformElement(Vec3(0f, 0f, 0f), Vec3(6f, 1f, 8f), Color(0xFF0C4A6E), Color(0xFF38BDF8), friction = 0.985f))

        // Downhill icy slope (fast!)
        elements.add(RampElement(Vec3(0f, -2.5f, 14f), Vec3(5f, 1f, 18f), elevationDeltaY = -5.0f, Color(0xFF0284C7), Color(0xFFBAE6FD)))
        elements.add(GemElement(gemId++, Vec3(0f, -1f, 10f)))
        elements.add(GemElement(gemId++, Vec3(0f, -3f, 16f)))

        // Chicane turn platform with low friction ice
        elements.add(PlatformElement(Vec3(0f, -5f, 28f), Vec3(8f, 1f, 8f), Color(0xFF0C4A6E), Color(0xFF38BDF8), friction = 0.985f))
        elements.add(BumperElement(Vec3(0f, -4.2f, 31f), radius = 1.0f))

        // Branching path left and right
        elements.add(PlatformElement(Vec3(-5f, -5f, 38f), Vec3(4f, 1f, 14f), Color(0xFF075985), Color(0xFF38BDF8), friction = 0.985f))
        elements.add(GemElement(gemId++, Vec3(-5f, -3.8f, 36f)))
        elements.add(GemElement(gemId++, Vec3(-5f, -3.8f, 42f)))

        elements.add(PlatformElement(Vec3(5f, -5f, 38f), Vec3(4f, 1f, 14f), Color(0xFF075985), Color(0xFF38BDF8), friction = 0.985f))
        elements.add(GemElement(gemId++, Vec3(5f, -3.8f, 36f)))
        elements.add(SpeedPadElement(Vec3(5f, -4.4f, 38f), Vec3(0f, 0f, 1f), 18f))

        // Convergence platform
        elements.add(PlatformElement(Vec3(0f, -5f, 52f), Vec3(10f, 1f, 10f), Color(0xFF0C4A6E), Color(0xFF38BDF8)))
        elements.add(CheckpointElement(1, Vec3(0f, -4.2f, 48f)))

        // Jump pad over frozen gap
        elements.add(JumpPadElement(Vec3(0f, -4.4f, 55f), upwardImpulse = 13f, forwardImpulse = 11f))
        elements.add(GemElement(gemId++, Vec3(0f, 1f, 65f)))

        // Landing & Goal
        elements.add(PlatformElement(Vec3(0f, -5f, 75f), Vec3(7f, 1f, 16f), Color(0xFF0C4A6E), Color(0xFF38BDF8)))
        elements.add(GemElement(gemId++, Vec3(0f, -3.8f, 74f)))
        elements.add(GoalPortalElement(Vec3(0f, -3.5f, 80f), radius = 2.0f))

        return LevelConfig(
            id = 3,
            name = "Glacier Slopes",
            subtitle = "Zero friction icy curves",
            skyColorTop = Color(0xFF082F49),
            skyColorBottom = Color(0xFF0C4A6E),
            gridColor = Color(0xFF7DD3FC).copy(alpha = 0.3f),
            parTimeSeconds = 28f,
            startPosition = Vec3(0f, 1.5f, 0f),
            elements = elements
        )
    }

    private fun createLevel4(): LevelConfig {
        val elements = mutableListOf<TrackElement>()
        var gemId = 0

        // Start
        elements.add(PlatformElement(Vec3(0f, 0f, 0f), Vec3(6f, 1f, 8f), Color(0xFF292524), Color(0xFFF97316)))

        // Platform with rotating hazard blade
        elements.add(PlatformElement(Vec3(0f, 0f, 14f), Vec3(7f, 1f, 16f), Color(0xFF1C1917), Color(0xFFEF4444)))
        elements.add(HazardSpinnerElement(Vec3(0f, 0.8f, 14f), length = 3.0f, rotationSpeed = 2.0f))
        elements.add(GemElement(gemId++, Vec3(-2.2f, 1.2f, 14f)))
        elements.add(GemElement(gemId++, Vec3(2.2f, 1.2f, 14f)))

        // Second hazard
        elements.add(PlatformElement(Vec3(0f, 0f, 30f), Vec3(7f, 1f, 14f), Color(0xFF1C1917), Color(0xFFEF4444)))
        elements.add(HazardSpinnerElement(Vec3(0f, 0.8f, 30f), length = 3.0f, rotationSpeed = -2.5f))
        elements.add(GemElement(gemId++, Vec3(0f, 1.2f, 30f)))

        // Moving platform
        elements.add(
            PlatformElement(
                position = Vec3(0f, 0f, 44f),
                size = Vec3(5f, 1f, 8f),
                color = Color(0xFF44403C),
                borderColor = Color(0xFFF97316),
                moveAxis = Vec3(1f, 0f, 0f),
                moveRange = 4.0f,
                moveSpeed = 2.2f
            )
        )
        elements.add(CheckpointElement(1, Vec3(0f, 0.8f, 44f)))

        // Bumpers lane
        elements.add(PlatformElement(Vec3(0f, 0f, 60f), Vec3(8f, 1f, 20f), Color(0xFF292524), Color(0xFFF97316)))
        elements.add(BumperElement(Vec3(-2.0f, 0.8f, 56f), radius = 1.0f))
        elements.add(BumperElement(Vec3(2.0f, 0.8f, 60f), radius = 1.0f))
        elements.add(BumperElement(Vec3(-1.5f, 0.8f, 64f), radius = 1.0f))
        elements.add(GemElement(gemId++, Vec3(0f, 1.2f, 58f)))
        elements.add(GemElement(gemId++, Vec3(0f, 1.2f, 62f)))

        // Super speed launch to exit
        elements.add(SpeedPadElement(Vec3(0f, 0.6f, 68f), Vec3(0f, 0f, 1f), 24f))
        elements.add(JumpPadElement(Vec3(0f, 0.6f, 72f), upwardImpulse = 12f, forwardImpulse = 10f))

        // Goal island
        elements.add(PlatformElement(Vec3(0f, 1f, 90f), Vec3(8f, 1f, 10f), Color(0xFF1C1917), Color(0xFFF97316)))
        elements.add(GemElement(gemId++, Vec3(0f, 2.2f, 88f)))
        elements.add(GoalPortalElement(Vec3(0f, 2.5f, 92f), radius = 2.0f))

        return LevelConfig(
            id = 4,
            name = "Hazard Foundry",
            subtitle = "Dodge rotating blades and magma bumpers",
            skyColorTop = Color(0xFF1C1917),
            skyColorBottom = Color(0xFF431407),
            gridColor = Color(0xFFF97316).copy(alpha = 0.25f),
            parTimeSeconds = 35f,
            startPosition = Vec3(0f, 1.5f, 0f),
            elements = elements
        )
    }

    private fun createLevel5(): LevelConfig {
        val elements = mutableListOf<TrackElement>()
        var gemId = 0

        // Start
        elements.add(PlatformElement(Vec3(0f, 0f, 0f), Vec3(6f, 1f, 8f), Color(0xFF18181B), Color(0xFFEAB308)))

        // Long winding thin bridge with gems
        elements.add(PlatformElement(Vec3(0f, 0f, 16f), Vec3(3.0f, 1f, 20f), Color(0xFF27272A), Color(0xFFEAB308)))
        elements.add(GemElement(gemId++, Vec3(0f, 1.2f, 10f)))
        elements.add(GemElement(gemId++, Vec3(0f, 1.2f, 16f)))
        elements.add(GemElement(gemId++, Vec3(0f, 1.2f, 22f)))

        // High ramp ascending into the cosmos
        elements.add(RampElement(Vec3(0f, 3f, 34f), Vec3(3.5f, 1f, 14f), elevationDeltaY = 6.0f, Color(0xFF3F3F46), Color(0xFFFACC15)))
        elements.add(SpeedPadElement(Vec3(0f, 6.2f, 40f), Vec3(0f, 0f, 1f), 20f))

        // Floating island in the sky
        elements.add(PlatformElement(Vec3(0f, 6f, 54f), Vec3(8f, 1f, 12f), Color(0xFF18181B), Color(0xFFEAB308)))
        elements.add(CheckpointElement(1, Vec3(0f, 6.8f, 50f)))
        elements.add(HazardSpinnerElement(Vec3(0f, 6.8f, 54f), length = 3.5f, rotationSpeed = 3.0f))
        elements.add(GemElement(gemId++, Vec3(-2.5f, 7.2f, 54f)))
        elements.add(GemElement(gemId++, Vec3(2.5f, 7.2f, 54f)))

        // Mega Jump Pad over empty abyss
        elements.add(JumpPadElement(Vec3(0f, 6.6f, 58f), upwardImpulse = 16f, forwardImpulse = 14f))

        // Mid-air star cluster
        elements.add(GemElement(gemId++, Vec3(0f, 14f, 70f)))
        elements.add(GemElement(gemId++, Vec3(0f, 12f, 76f)))

        // Destination floating citadel
        elements.add(PlatformElement(Vec3(0f, 6f, 92f), Vec3(10f, 1f, 16f), Color(0xFF18181B), Color(0xFFEAB308)))
        elements.add(BumperElement(Vec3(-2.5f, 6.8f, 88f), radius = 1.0f))
        elements.add(BumperElement(Vec3(2.5f, 6.8f, 88f), radius = 1.0f))
        elements.add(GemElement(gemId++, Vec3(0f, 7.2f, 90f)))

        // Golden Apex Portal
        elements.add(GoalPortalElement(Vec3(0f, 7.5f, 96f), radius = 2.4f))

        return LevelConfig(
            id = 5,
            name = "Cosmic Apex",
            subtitle = "Ascend the celestial highway",
            skyColorTop = Color(0xFF030712),
            skyColorBottom = Color(0xFF1E1B4B),
            gridColor = Color(0xFFFACC15).copy(alpha = 0.3f),
            parTimeSeconds = 40f,
            startPosition = Vec3(0f, 1.5f, 0f),
            elements = elements
        )
    }

    /**
     * Procedural endless track generator
     */
    fun generateEndlessChunk(chunkIndex: Int, startZ: Float): List<TrackElement> {
        val elements = mutableListOf<TrackElement>()
        val baseZ = startZ
        val pattern = chunkIndex % 4

        when (pattern) {
            0 -> {
                // Wide strip with gems & speed pad
                elements.add(PlatformElement(Vec3(0f, 0f, baseZ + 15f), Vec3(6f, 1f, 30f), Color(0xFF1E293B), Color(0xFF38BDF8)))
                elements.add(SpeedPadElement(Vec3(0f, 0.6f, baseZ + 10f), Vec3(0f, 0f, 1f), 18f))
                elements.add(GemElement(chunkIndex * 10, Vec3(0f, 1.2f, baseZ + 18f)))
                elements.add(GemElement(chunkIndex * 10 + 1, Vec3(0f, 1.2f, baseZ + 24f)))
            }
            1 -> {
                // Jump over gap
                elements.add(PlatformElement(Vec3(0f, 0f, baseZ + 8f), Vec3(5f, 1f, 16f), Color(0xFF1E1B4B), Color(0xFFA855F7)))
                elements.add(JumpPadElement(Vec3(0f, 0.6f, baseZ + 14f), upwardImpulse = 12f, forwardImpulse = 10f))
                elements.add(GemElement(chunkIndex * 10 + 2, Vec3(0f, 5f, baseZ + 22f)))
                elements.add(PlatformElement(Vec3(0f, 0f, baseZ + 32f), Vec3(6f, 1f, 16f), Color(0xFF1E1B4B), Color(0xFFA855F7)))
            }
            2 -> {
                // Moving platform
                elements.add(PlatformElement(Vec3(0f, 0f, baseZ + 6f), Vec3(5f, 1f, 12f), Color(0xFF0F172A), Color(0xFF06B6D4)))
                elements.add(
                    PlatformElement(
                        position = Vec3(0f, 0f, baseZ + 22f),
                        size = Vec3(5f, 1f, 12f),
                        color = Color(0xFF0F172A),
                        borderColor = Color(0xFF06B6D4),
                        moveAxis = Vec3(1f, 0f, 0f),
                        moveRange = 3.0f,
                        moveSpeed = 2.0f
                    )
                )
                elements.add(GemElement(chunkIndex * 10 + 3, Vec3(0f, 1.2f, baseZ + 22f)))
                elements.add(PlatformElement(Vec3(0f, 0f, baseZ + 36f), Vec3(5f, 1f, 12f), Color(0xFF0F172A), Color(0xFF06B6D4)))
            }
            3 -> {
                // Hazard & Bumpers
                elements.add(PlatformElement(Vec3(0f, 0f, baseZ + 15f), Vec3(7f, 1f, 30f), Color(0xFF1C1917), Color(0xFFEF4444)))
                elements.add(BumperElement(Vec3(-1.8f, 0.8f, baseZ + 10f), radius = 0.9f))
                elements.add(BumperElement(Vec3(1.8f, 0.8f, baseZ + 18f), radius = 0.9f))
                elements.add(HazardSpinnerElement(Vec3(0f, 0.8f, baseZ + 24f), length = 2.8f, rotationSpeed = 2.5f))
                elements.add(GemElement(chunkIndex * 10 + 4, Vec3(-2f, 1.2f, baseZ + 24f)))
                elements.add(GemElement(chunkIndex * 10 + 5, Vec3(2f, 1.2f, baseZ + 24f)))
            }
        }
        return elements
    }
}
