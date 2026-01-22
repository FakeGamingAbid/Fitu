package com.fitu.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== BASE COLORS ====================
val OrangePrimary = Color(0xFFF6822B)
val OrangeSecondary = Color(0xFFFF6B35)
val OrangeLight = Color(0xFFFFAB91)

val DarkBackground = Color(0xFF0A0A0F)
val SurfaceDark = Color(0xFF1E1E24)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFB0B0B0)

// AI Coach colors
val GreenSuccess = Color(0xFF4CAF50)
val JointOrange = Color(0xFFFF6B00)
val SkeletonWhite = Color(0xFFFFFFFF)
val FeedbackGreen = Color(0xFF00FF00)

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * Centralized color palette for the app.
 * Modern, consistent design system with improved UI/UX.
 */
object AppColors {
    // ==================== PRIMARY BRAND ====================
    val OrangePrimary = Color(0xFFF6822B)
    val OrangeSecondary = Color(0xFFFF6B35)
    val OrangeLight = Color(0xFFFFAB91)
    val OrangeGlow = Color(0xFFF6822B).copy(alpha = 0.3f)
    
    // ==================== BACKGROUNDS ====================
    val BackgroundDark = Color(0xFF0A0A0F)
    val BackgroundElevated = Color(0xFF12121A)
    val BackgroundSheet = Color(0xFF1A1A22)
    val BackgroundCard = Color(0xFF1E1E26)
    val SurfaceDark = Color(0xFF1E1E26)
    val SurfaceLight = Color(0xFF2A2A34)
    val SurfaceHover = Color(0xFF32323E)
    
    // ==================== TEXT ====================
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB8B8C0)
    val TextTertiary = Color(0xFF8A8A94)
    val TextHint = Color(0xFF5A5A64)
    val TextDisabled = Color(0xFF4A4A54)
    
    // ==================== BORDERS & DIVIDERS ====================
    val BorderLight = Color(0xFF3A3A44)
    val BorderSubtle = Color(0xFF2A2A34)
    val DividerColor = Color(0xFFFFFFFF).copy(alpha = 0.08f)
    
    // ==================== GLASSMORPHISM ====================
    val GlassBackground = Color(0xFFFFFFFF).copy(alpha = 0.06f)
    val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.12f)
    val GlassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.15f)
    
    // ==================== STATUS COLORS ====================
    val Success = Color(0xFF4CAF50)
    val SuccessLight = Color(0xFF81C784)
    val Error = Color(0xFFE53935)
    val ErrorLight = Color(0xFFEF5350)
    val Warning = Color(0xFFFFA726)
    val WarningLight = Color(0xFFFFB74D)
    val Info = Color(0xFF29B6F6)
    val InfoLight = Color(0xFF4FC3F7)
    
    // ==================== MACRO COLORS ====================
    val ProteinColor = Color(0xFF66BB6A)      // Fresh green
    val CarbsColor = Color(0xFF42A5F5)        // Bright blue
    val FatsColor = Color(0xFFFFCA28)         // Golden yellow
    val CaloriesColor = Color(0xFFEF5350)     // Warm red
    
    // ==================== MEAL TYPE COLORS ====================
    val BreakfastColor = Color(0xFFFFB74D)    // Warm sunrise orange
    val LunchColor = Color(0xFF4FC3F7)        // Bright sky blue
    val DinnerColor = Color(0xFFBA68C8)       // Evening purple
    val SnacksColor = Color(0xFFFF8A65)       // Coral
    
    // ==================== GRADIENT PRESETS ====================
    val OrangeGradient = listOf(OrangePrimary, OrangeSecondary, OrangeLight)
    val DarkGradient = listOf(BackgroundDark, BackgroundElevated)
    val GlassGradient = listOf(
        Color(0xFFFFFFFF).copy(alpha = 0.1f),
        Color(0xFFFFFFFF).copy(alpha = 0.05f)
    )
    
    // ==================== UTILITY FUNCTIONS ====================
    
    /**
     * Returns a background color for macro chips
     */
    fun chipBackground(color: Color): Color = color.copy(alpha = 0.15f)
    
    /**
     * Returns a glow color for elements
     */
    fun glow(color: Color): Color = color.copy(alpha = 0.25f)
    
    /**
     * Returns a subtle background tint
     */
    fun tint(color: Color): Color = color.copy(alpha = 0.08f)
    
    /**
     * Returns an icon container background
     */
    fun iconBackground(color: Color): Color = color.copy(alpha = 0.15f)
}
