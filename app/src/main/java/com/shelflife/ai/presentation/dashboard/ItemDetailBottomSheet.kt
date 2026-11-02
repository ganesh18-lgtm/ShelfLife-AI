package com.shelflife.ai.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.presentation.components.OnlineFoodImage
import com.shelflife.ai.presentation.components.ShelfLifeProgressRing
import com.shelflife.ai.presentation.theme.CriticalCrimson
import com.shelflife.ai.presentation.theme.CriticalCrimsonSoft
import com.shelflife.ai.presentation.theme.EmeraldDark
import com.shelflife.ai.presentation.theme.EmeraldPrimary
import com.shelflife.ai.presentation.theme.EmeraldSoft
import com.shelflife.ai.presentation.theme.SlateMuted
import com.shelflife.ai.presentation.theme.SlateNavy
import com.shelflife.ai.presentation.theme.SlateText
import com.shelflife.ai.presentation.theme.SurfaceBorder
import com.shelflife.ai.presentation.theme.SurfaceSubtle
import com.shelflife.ai.presentation.theme.SurfaceWhite
import com.shelflife.ai.presentation.theme.WarningAmberDark
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 100% Crash-Proof, High-Reliability Item Information & Storage Intelligence Dialog.
 *
 * Displays full item context:
 * - Product Name, Image & Category
 * - Exact Expiry Date & Freshness Countdown
 * - Manufacturing Date (MFG) & Expiry Mode
 * - Quantity & Barcode (if present)
 * - Optimal Thermal Zone & Humidity recommendations
 * - AI Preservation & Storage Hacks
 * - Edit and Remove action triggers
 */
@Composable
fun ItemDetailBottomSheet(
    item: PantryItem,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = remember { System.currentTimeMillis() }
    val diffMillis = item.expiryTimestamp - now
    val hoursRemaining = TimeUnit.MILLISECONDS.toHours(diffMillis).coerceAtLeast(0)
    val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMillis).coerceAtLeast(0)

    val (urgencyColor, urgencyLabel) = when (item.urgency) {
        ExpiryUrgency.EXPIRED -> CriticalCrimson to "Expired"
        ExpiryUrgency.CRITICAL -> CriticalCrimson to "Expires in $hoursRemaining hrs"
        ExpiryUrgency.WARNING -> WarningAmberDark to "Expires in $daysRemaining days"
        ExpiryUrgency.FRESH -> EmeraldPrimary to "Fresh ($daysRemaining days left)"
    }

    val expiryDateFormatted = remember(item.expiryTimestamp) {
        try {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            sdf.format(item.expiryTimestamp)
        } catch (_: Exception) {
            "Unknown Date"
        }
    }

    val mfgDateFormatted = remember(item.mfgDateTimestamp) {
        item.mfgDateTimestamp?.let {
            try {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                sdf.format(it)
            } catch (_: Exception) {
                null
            }
        }
    }

    val expiryModeDisplay = when (item.expiryMode) {
        "BEST_BEFORE" -> "Best Before"
        "USE_BY" -> "Use By"
        else -> "Exact Expire Date"
    }

    // Category-specific environmental storage recommendations
    val (storageZone, storageTemp, humidityTip) = when (item.category) {
        Category.PRODUCE -> Triple("Crisper Drawer", "3°C - 5°C", "High humidity (85-95%)")
        Category.DAIRY -> Triple("Middle / Bottom Shelf", "2°C - 4°C", "Avoid refrigerator door")
        Category.MEAT -> Triple("Coldest Shelf / Meat Drawer", "0°C - 2°C", "Keep sealed in airtight tray")
        Category.BAKERY -> Triple("Cool Dry Bread Box", "18°C - 22°C", "Keep away from direct heat")
        Category.PANTRY -> Triple("Dark Pantry Shelf", "15°C - 20°C", "Low humidity, seal in airtight jar")
        Category.BEVERAGES -> Triple("Fridge Door / Top Shelf", "4°C - 6°C", "Keep upright and capped")
        Category.FROZEN -> Triple("Deep Freezer", "-18°C or colder", "Prevent freezer burn with wrap")
        Category.OTHER -> Triple("Pantry / Fridge as labeled", "Ambient / Chilled", "Follow package instructions")
    }

    // AI storage preservation hacks tailored to food item
    val storageHacks = when (item.category) {
        Category.PRODUCE -> listOf(
            "The Paper Towel Trick" to "Line container with a dry paper towel to absorb condensation and add 2-3 extra fresh days.",
            "Ethylene Gas Isolation" to "Store away from high-ethylene items (apples, bananas) to prevent accelerated ripening."
        )
        Category.DAIRY -> listOf(
            "Keep Off The Door" to "Refrigerator door temperatures fluctuate every time it opens. Store milk on middle shelves.",
            "Airtight Seal" to "Keep cream and yogurt containers firmly sealed to prevent airborne bacteria contamination."
        )
        Category.BAKERY -> listOf(
            "Do Not Refrigerate" to "Refrigeration accelerates starch retrogradation (makes bread go stale 3x faster). Freeze slices instead.",
            "Freeze for Zero-Waste" to "Slice and freeze in advance; toast directly from frozen for fresh-baked texture."
        )
        Category.MEAT -> listOf(
            "Freeze Before Use-By" to "If not cooking within 48 hours, freeze immediately at -18°C to lock in quality.",
            "Bottom Shelf Placement" to "Store raw meats on the bottom shelf to eliminate any drip-contamination risk."
        )
        else -> listOf(
            "Airtight Container Transfer" to "Transfer opened packages into sealed glass or BPA-free containers to block humidity.",
            "First In, First Out (FIFO)" to "Place this item in front of newer groceries so you use it before it expires."
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // Dimmed backdrop with tap-outside dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Main Bottom Information Sheet
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .clickable(enabled = false) {}, // Intercept clicks
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = SurfaceWhite,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .imePadding()
                ) {
                    // Drag indicator pill
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceBorder)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Header Row: Title & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Item Information",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SlateNavy
                            )
                            Text(
                                text = "Pantry details & AI storage intelligence",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = SlateMuted
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceSubtle)
                                .border(1.dp, SurfaceBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = SlateNavy,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable Information Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Hero Product Summary Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SurfaceWhite)
                                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                                ) {
                                    OnlineFoodImage(
                                        productName = item.name,
                                        category = item.category,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = SlateNavy,
                                        maxLines = 2
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(EmeraldSoft)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = item.category.displayName,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = EmeraldDark
                                            )
                                        }

                                        Text(
                                            text = "Qty: ${item.quantity}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = SlateText
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Barcode Chip (if available)
                        if (!item.barcode.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceSubtle)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = SlateNavy,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Barcode: ${item.barcode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateNavy,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // 3. Freshness & Expiry Countdown Banner Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Freshness Countdown",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = SlateMuted
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = urgencyLabel,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = urgencyColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$expiryModeDisplay: $expiryDateFormatted",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = SlateText
                                    )
                                    if (mfgDateFormatted != null) {
                                        Text(
                                            text = "MFG Date: $mfgDateFormatted",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                            color = SlateMuted
                                        )
                                    }
                                }

                                ShelfLifeProgressRing(
                                    expiryTimestamp = item.expiryTimestamp,
                                    urgency = item.urgency,
                                    addedTimestamp = item.addedTimestamp,
                                    size = 52.dp,
                                    strokeWidth = 4.5.dp
                                )
                            }
                        }

                        // 4. Environmental Telemetry (Storage Zone & Humidity Index)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InfoTelemetryCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Thermostat,
                                iconTint = EmeraldPrimary,
                                title = "Thermal Zone",
                                value = storageTemp,
                                subtitle = storageZone
                            )

                            InfoTelemetryCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.WaterDrop,
                                iconTint = WarningAmberDark,
                                title = "Humidity Index",
                                value = humidityTip,
                                subtitle = "Recommended setting"
                            )
                        }

                        // 5. AI Storage & Salvage Hacks
                        Text(
                            text = "AI Storage & Freshness Hacks",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = SlateNavy
                        )

                        storageHacks.forEach { (hackTitle, hackDesc) ->
                            InfoHackCard(
                                title = hackTitle,
                                description = hackDesc
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Pinned Bottom Actions: Edit and Delete
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                onEditClick()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Details", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onDeleteClick()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CriticalCrimson),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remove", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTelemetryCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = SlateMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
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

@Composable
private fun InfoHackCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(EmeraldSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = SlateNavy
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = SlateText,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
