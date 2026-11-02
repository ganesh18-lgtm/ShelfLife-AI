package com.shelflife.ai.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shelflife.ai.presentation.theme.EmeraldDark
import com.shelflife.ai.presentation.theme.EmeraldPrimary
import com.shelflife.ai.presentation.theme.SlateMuted
import com.shelflife.ai.presentation.theme.SlateNavy
import com.shelflife.ai.presentation.theme.SurfaceBorder
import com.shelflife.ai.presentation.theme.SurfaceWhite
import com.shelflife.ai.presentation.theme.WarningAmberDark

/**
 * Micro-stats / Eco Cockpit Row matching model design:
 * [Zero Waste 94%] [Saved $42.50] [CO2 Offset 8.2kg]
 */
@Composable
fun EcoCockpitRow(
    totalItems: Int,
    freshCount: Int,
    criticalCount: Int,
    modifier: Modifier = Modifier
) {
    val zeroWastePct = if (totalItems > 0) {
        ((totalItems - criticalCount).toDouble() / totalItems * 100).toInt().coerceIn(0, 100)
    } else {
        100
    }
    val savedDollars = "%.2f".format((totalItems * 3.5).coerceAtLeast(14.0))
    val co2OffsetKg = "%.1f".format((totalItems * 0.45).coerceAtLeast(2.4))

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tile 1: Zero Waste %
        CockpitTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Eco,
            iconTint = EmeraldPrimary,
            label = "Zero Waste",
            value = "$zeroWastePct%",
            subtext = "+4% this wk",
            subtextTint = EmeraldDark
        )

        // Tile 2: Saved $
        CockpitTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Savings,
            iconTint = EmeraldDark,
            label = "Saved",
            value = "\$$savedDollars",
            subtext = "Oct budget",
            subtextTint = SlateMuted
        )

        // Tile 3: CO2 Offset
        CockpitTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Spa,
            iconTint = WarningAmberDark,
            label = "CO₂ Offset",
            value = "${co2OffsetKg}kg",
            subtext = "$totalItems items kept",
            subtextTint = SlateMuted
        )
    }
}

@Composable
private fun CockpitTile(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    value: String,
    subtext: String,
    subtextTint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(width = 1.dp, color = SurfaceBorder, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = SlateMuted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = SlateNavy
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = subtextTint
            )
        }
    }
}

/**
 * Backward compatibility alias for PantryHealthWidget.
 */
@Composable
fun PantryHealthWidget(
    totalItems: Int,
    freshCount: Int,
    warningCount: Int,
    criticalCount: Int,
    modifier: Modifier = Modifier
) {
    EcoCockpitRow(
        totalItems = totalItems,
        freshCount = freshCount,
        criticalCount = criticalCount,
        modifier = modifier
    )
}
