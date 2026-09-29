package com.example.game.renderer

import androidx.compose.ui.graphics.Color

data class BallSkin(
    val id: String,
    val name: String,
    val unlockCost: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val specularColor: Color,
    val patternType: PatternType,
    val description: String
) {
    enum class PatternType {
        GRID_CYBER,
        MAGMA_RINGS,
        MARBLE_SWIRL,
        EIGHT_BALL,
        METALLIC_GOLD,
        PRISM_CRYSTAL
    }

    companion object {
        val ALL_SKINS = listOf(
            BallSkin(
                id = "cyber",
                name = "Cyber Matrix",
                unlockCost = 0,
                primaryColor = Color(0xFF0284C7),
                secondaryColor = Color(0xFF0369A1),
                accentColor = Color(0xFF38BDF8),
                specularColor = Color(0xFFE0F2FE),
                patternType = PatternType.GRID_CYBER,
                description = "High-tech grid glowing with neon energy"
            ),
            BallSkin(
                id = "magma",
                name = "Solar Flare",
                unlockCost = 15,
                primaryColor = Color(0xFFDC2626),
                secondaryColor = Color(0xFF991B1B),
                accentColor = Color(0xFFF97316),
                specularColor = Color(0xFFFEF08A),
                patternType = PatternType.MAGMA_RINGS,
                description = "Molten core pulsating with intense thermal power"
            ),
            BallSkin(
                id = "marble",
                name = "Cosmic Marble",
                unlockCost = 30,
                primaryColor = Color(0xFF581C87),
                secondaryColor = Color(0xFF3B0764),
                accentColor = Color(0xFFA855F7),
                specularColor = Color(0xFFF3E8FF),
                patternType = PatternType.MARBLE_SWIRL,
                description = "Swirling deep nebula with ethereal starlight veins"
            ),
            BallSkin(
                id = "eightball",
                name = "Classic 8-Ball",
                unlockCost = 45,
                primaryColor = Color(0xFF18181B),
                secondaryColor = Color(0xFF09090B),
                accentColor = Color(0xFFF4F4F5),
                specularColor = Color(0xFFFFFFFF),
                patternType = PatternType.EIGHT_BALL,
                description = "Ultra-glossy billiard classic with bold number 8"
            ),
            BallSkin(
                id = "gold",
                name = "Aero Gold",
                unlockCost = 60,
                primaryColor = Color(0xFFD97706),
                secondaryColor = Color(0xFFB45309),
                accentColor = Color(0xFFFDE047),
                specularColor = Color(0xFFFEF9C3),
                patternType = PatternType.METALLIC_GOLD,
                description = "Solid 24-karat mirror-polished aerodynamic gold"
            ),
            BallSkin(
                id = "prism",
                name = "Prism Shimmer",
                unlockCost = 80,
                primaryColor = Color(0xFF0D9488),
                secondaryColor = Color(0xFF0F766E),
                accentColor = Color(0xFF2DD4BF),
                specularColor = Color(0xFFCCFBF1),
                patternType = PatternType.PRISM_CRYSTAL,
                description = "Refractive crystalline prism breaking light into spectra"
            )
        )

        fun getSkin(id: String): BallSkin {
            return ALL_SKINS.find { it.id == id } ?: ALL_SKINS[0]
        }
    }
}
