package com.shelflife.ai.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// Crisp Minimalist Slate Design System Baseline (Scandinavian Clean Utilitarian)
// As specified in stitch_shelflife_ai_pantry_tracker/crisp_minimalist_slate/DESIGN.md
// ============================================================================

// Base Canvas & Surface Hierarchy
val BackgroundCanvas = Color(0xFFF1F5F9)          // #F1F5F9 Cool Light Slate Base
val SurfaceWhite = Color(0xFFFFFFFF)              // #FFFFFF Pure White for Cards & Surfaces
val SurfaceSubtle = Color(0xFFF8FAFC)             // #F8FAFC Recessed Containers & Chips
val SurfaceBorder = Color(0xFFE2E8F0)             // #E2E8F0 1px Subtle Delineation Border
val SurfaceBorderHover = Color(0xFFCBD5E1)

// Typography & Content Shades
val SlateNavy = Color(0xFF0F172A)                 // #0F172A Primary Headings & Bold Text
val SlateText = Color(0xFF334155)                 // #334155 Body Text & Secondary Titles
val SlateMuted = Color(0xFF64748B)                // #64748B Tertiary Copy, Timestamps, Muted Labels
val SlateSubtle = Color(0xFF94A3B8)

// Brand Primary & Semantic Freshness States
val EmeraldPrimary = Color(0xFF10B981)            // #10B981 Dynamic Emerald Green (Primary Brand & Fresh >5d)
val EmeraldDark = Color(0xFF059669)               // #059669 Primary Button Hover / Active
val EmeraldSoft = Color(0xFFECFDF5)               // rgba(16, 185, 129, 0.12) Soft Fresh Tint
val EmeraldBorder = Color(0xFFA7F3D0)
val OnEmerald = Color(0xFFFFFFFF)

// Warning Urgency (2 to 5 Days)
val WarningAmber = Color(0xFFF59E0B)              // #F59E0B Amber Gold
val WarningAmberDark = Color(0xFFD97706)
val WarningAmberSoft = Color(0xFFFFFBEB)          // rgba(245, 158, 11, 0.12) Soft Amber Tint
val WarningAmberBorder = Color(0xFFFDE68A)

// Critical Urgency (< 48 Hours)
val CriticalCrimson = Color(0xFFEF4444)           // #EF4444 Coral Crimson Alert
val CriticalCrimsonDark = Color(0xFFDC2626)
val CriticalCrimsonSoft = Color(0xFFFEF2F2)       // rgba(239, 68, 68, 0.12) Soft Crimson Tint
val CriticalCrimsonBorder = Color(0xFFFECACA)

// Secondary Accents (Blue & Purple for Badges)
val BlueSoft = Color(0xFFEFF6FF)
val BlueText = Color(0xFF3B82F6)

// ============================================================================
// Backward-Compatibility Aliases for Seamless View Binding
// ============================================================================
val CanvasNavy = SlateNavy
val DarkBackground = BackgroundCanvas
val SurfaceCard = SurfaceWhite
val SurfaceCard80 = SurfaceWhite
val GlassBorder = SurfaceBorder
val GlassBorderActive = SurfaceBorderHover

val TextPrimary = SlateNavy
val TextSecondary = SlateText
val TextMuted = SlateMuted

val NeonMint = EmeraldPrimary
val NeonMintDark = EmeraldDark
val NeonMintContainer = EmeraldSoft
val OnNeonMint = OnEmerald

val FreshEmerald = EmeraldPrimary
val FreshEmeraldContainer = EmeraldSoft
val FreshEmeraldBorder = EmeraldBorder

val WarningAmberContainer = WarningAmberSoft

val CriticalCoral = CriticalCrimson
val CriticalCoralContainer = CriticalCrimsonSoft
val CriticalCoralBorder = CriticalCrimsonBorder

val ExpiredDarkRed = Color(0xFF991B1B)
val ExpiredDarkRedContainer = Color(0x26991B1B)

val MagentaPrimary = EmeraldPrimary
val LemonYellow = WarningAmber
val SilkBlue = EmeraldPrimary

// Gradients
val FreshGradient = Brush.horizontalGradient(
    colors = listOf(Color(0x2610B981), Color(0x0A10B981))
)
val WarningGradient = Brush.horizontalGradient(
    colors = listOf(Color(0x26F59E0B), Color(0x0AF59E0B))
)
val CriticalGradient = Brush.horizontalGradient(
    colors = listOf(Color(0x26EF4444), Color(0x0AEF4444))
)
val GlassBorderTricolor = Brush.linearGradient(
    colors = listOf(EmeraldPrimary, WarningAmber, EmeraldPrimary)
)
val TricolorGradient = GlassBorderTricolor
