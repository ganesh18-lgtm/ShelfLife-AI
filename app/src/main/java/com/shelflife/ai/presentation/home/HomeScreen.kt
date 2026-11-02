package com.shelflife.ai.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shelflife.ai.presentation.dashboard.DashboardViewModel
import com.shelflife.ai.presentation.theme.BackgroundCanvas
import com.shelflife.ai.presentation.theme.CriticalCrimson
import com.shelflife.ai.presentation.theme.CriticalCrimsonSoft
import com.shelflife.ai.presentation.theme.EmeraldDark
import com.shelflife.ai.presentation.theme.EmeraldPrimary
import com.shelflife.ai.presentation.theme.EmeraldSoft
import com.shelflife.ai.presentation.theme.SlateMuted
import com.shelflife.ai.presentation.theme.SlateNavy
import com.shelflife.ai.presentation.theme.SurfaceBorder
import com.shelflife.ai.presentation.theme.SurfaceSubtle
import com.shelflife.ai.presentation.theme.SurfaceWhite
import com.shelflife.ai.presentation.theme.WarningAmber
import com.shelflife.ai.presentation.theme.WarningAmberDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Modern, uncluttered Home Screen redesigned for visual clarity and ease of use:
 * - Warm personalized greeting with dynamic date & user avatar
 * - Clean Hero Pantry Freshness Bento Card
 * - Direct Primary Action Duo: [Quick Scan] and [Add Manually]
 * - Compact Quick Utilities: [AI Chef Recipes] and [Order Groceries]
 * - Curated Daily Food-Saving Tip
 */
@Composable
fun HomeScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddItem: () -> Unit,
    onNavigateToPantry: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToGrocery: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val displayName = remember(userProfile.name) {
        userProfile.name.trim().split(" ").firstOrNull()?.ifBlank { "Chef" } ?: "Chef"
    }

    val greetingText = remember(displayName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good Morning, $displayName! ☀️"
            in 12..16 -> "Good Afternoon, $displayName! 🌤️"
            in 17..21 -> "Good Evening, $displayName! 🌙"
            else -> "Hello, $displayName! 🌿"
        }
    }

    val todayFormatted = remember {
        val sdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        sdf.format(Date())
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCanvas),
        containerColor = BackgroundCanvas,
        topBar = {
            HomeTopBar(
                greeting = greetingText,
                dateSubtitle = todayFormatted,
                userAvatarUri = userProfile.photoUri,
                onProfileClick = onNavigateToSettings,
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Hero Pantry Health & Freshness Card
            HeroPantryHealthCard(
                totalCount = uiState.totalCount,
                freshCount = uiState.freshCount,
                criticalCount = uiState.criticalCount,
                onViewPantryClick = onNavigateToPantry
            )

            // 2. Primary Action Duo: Quick Scan & Add Manually
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroActionCard(
                    title = "Quick Scan",
                    subtitle = "Barcode & Expiry",
                    icon = Icons.Default.CenterFocusWeak,
                    iconBgColor = EmeraldPrimary,
                    iconTint = Color.White,
                    borderColor = EmeraldPrimary.copy(alpha = 0.4f),
                    onClick = onNavigateToScanner,
                    modifier = Modifier.weight(1f)
                )

                HeroActionCard(
                    title = "Add Item",
                    subtitle = "Manual Entry",
                    icon = Icons.Default.Add,
                    iconBgColor = EmeraldSoft,
                    iconTint = EmeraldDark,
                    borderColor = SurfaceBorder,
                    onClick = onNavigateToAddItem,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. Quick Utilities: AI Chat & AI Chef Recipes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickUtilityPillCard(
                    title = "Gemini AI Chat",
                    subtitle = "Pantry Q&A assistant",
                    icon = Icons.Default.AutoAwesome,
                    iconTint = EmeraldPrimary,
                    iconBg = EmeraldSoft,
                    onClick = onNavigateToChat,
                    modifier = Modifier.weight(1f)
                )

                QuickUtilityPillCard(
                    title = "AI Chef Recipes",
                    subtitle = "Cook with pantry stock",
                    icon = Icons.Default.RestaurantMenu,
                    iconTint = Color(0xFF8B5CF6),
                    iconBg = Color(0xFFF3E8FF),
                    onClick = onNavigateToRecipes,
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Daily Smart Kitchen Preservation Tip
            DailyPreservationTipCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Top App Bar with warm personalized greeting, today's date, and user profile avatar.
 */
@Composable
private fun HomeTopBar(
    greeting: String,
    dateSubtitle: String,
    userAvatarUri: String?,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .border(1.dp, SurfaceBorder)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = SlateNavy,
                maxLines = 1
            )
            Text(
                text = "$dateSubtitle • Zero-Waste Kitchen",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = SlateMuted
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile Avatar Pill
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(2.dp, EmeraldPrimary.copy(alpha = 0.5f), CircleShape)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = userAvatarUri ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80",
                    contentDescription = "User Profile",
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceSubtle)
                    .border(1.dp, SurfaceBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = SlateNavy,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Clean Bento-style Hero Card showing pantry metrics, freshness index, and urgent expiry notifications.
 */
@Composable
private fun HeroPantryHealthCard(
    totalCount: Int,
    freshCount: Int,
    criticalCount: Int,
    onViewPantryClick: () -> Unit
) {
    val freshnessPct = if (totalCount > 0) {
        ((totalCount - criticalCount).toDouble() / totalCount * 100).toInt().coerceIn(0, 100)
    } else {
        100
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Status badge & Freshness score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (criticalCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(CriticalCrimsonSoft)
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(9999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CriticalCrimson,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "$criticalCount Expiring Soon",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = CriticalCrimson
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(EmeraldSoft)
                            .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(9999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                tint = EmeraldDark,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Pantry 100% Fresh",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = EmeraldDark
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onViewPantryClick)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "View Pantry",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = EmeraldDark
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Pantry",
                        tint = EmeraldDark,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // 3-Metric Clean Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricItem(
                    label = "In Pantry",
                    value = "$totalCount",
                    valueColor = SlateNavy
                )
                MetricItem(
                    label = "Optimal",
                    value = "$freshCount",
                    valueColor = EmeraldDark
                )
                MetricItem(
                    label = "Needs Action",
                    value = "$criticalCount",
                    valueColor = if (criticalCount > 0) CriticalCrimson else SlateMuted
                )
                MetricItem(
                    label = "Freshness",
                    value = "$freshnessPct%",
                    valueColor = EmeraldPrimary
                )
            }

            // Attention Alert strip (only when critical items exist)
            if (criticalCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CriticalCrimsonSoft.copy(alpha = 0.5f))
                        .clickable(onClick = onViewPantryClick)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rescue food before it spoils • Tap to view items",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = CriticalCrimson
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = CriticalCrimson,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            color = valueColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = SlateMuted
        )
    }
}

/**
 * Hero Primary Action Card for Quick Scan and Add Item.
 */
@Composable
private fun HeroActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = SlateNavy
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = SlateMuted
                )
            }
        }
    }
}

/**
 * Compact Utility Card for AI Chef and Grocery ordering.
 */
@Composable
private fun QuickUtilityPillCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = SlateNavy,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = SlateMuted,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Curated, single Daily Preservation Tip Card matching modern clean design principles.
 */
@Composable
private fun DailyPreservationTipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = WarningAmberDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Daily Kitchen Tip",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        color = SlateNavy
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceSubtle)
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = "Produce",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = EmeraldDark
                    )
                }
            }

            Text(
                text = "Keep ripe avocados submerged in cool water inside the fridge to prevent oxidation for up to 5 extra days.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                color = SlateNavy.copy(alpha = 0.85f)
            )
        }
    }
}
