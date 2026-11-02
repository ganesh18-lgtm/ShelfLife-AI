package com.shelflife.ai.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.presentation.components.OnlineFoodImage
import com.shelflife.ai.presentation.components.ShelfLifeProgressRing
import com.shelflife.ai.presentation.theme.CriticalCrimson
import com.shelflife.ai.presentation.theme.CriticalCrimsonSoft
import com.shelflife.ai.presentation.theme.EmeraldDark
import com.shelflife.ai.presentation.theme.EmeraldPrimary
import com.shelflife.ai.presentation.theme.SlateMuted
import com.shelflife.ai.presentation.theme.SlateNavy
import com.shelflife.ai.presentation.theme.SlateSubtle
import com.shelflife.ai.presentation.theme.SurfaceBorder
import com.shelflife.ai.presentation.theme.SurfaceSubtle
import com.shelflife.ai.presentation.theme.SurfaceWhite
import com.shelflife.ai.presentation.theme.WarningAmber
import com.shelflife.ai.presentation.theme.WarningAmberDark
import java.util.concurrent.TimeUnit

/**
 * Inventory Card matching the exact model design from pantry_dashboard/code.html:
 * [Thumbnail with corner badge] [Title + Category + Expiry countdown] [Radial Progress Ring]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryItemCard(
    item: PantryItem,
    onQuantityChange: (delta: Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CriticalCrimsonSoft)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete item",
                    tint = CriticalCrimson,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) {
        val now = System.currentTimeMillis()
        val diffMillis = item.expiryTimestamp - now

        val (urgencyColor, cornerIcon, urgencyText) = when (item.urgency) {
            ExpiryUrgency.EXPIRED -> Triple(
                CriticalCrimson,
                Icons.Default.PriorityHigh,
                "Expired"
            )
            ExpiryUrgency.CRITICAL -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diffMillis).coerceAtLeast(0)
                val text = if (hours < 24) "Expiring in $hours hrs" else "Expiring in ${hours / 24}d"
                Triple(CriticalCrimson, Icons.Default.PriorityHigh, text)
            }
            ExpiryUrgency.WARNING -> {
                val days = TimeUnit.MILLISECONDS.toDays(diffMillis).coerceAtLeast(1)
                Triple(WarningAmberDark, Icons.Default.Schedule, "Expires in $days days")
            }
            ExpiryUrgency.FRESH -> {
                val days = TimeUnit.MILLISECONDS.toDays(diffMillis).coerceAtLeast(5)
                Triple(EmeraldDark, Icons.Default.Check, "Fresh ($days days left)")
            }
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = when (item.urgency) {
                        ExpiryUrgency.CRITICAL, ExpiryUrgency.EXPIRED -> Color(0xFFFCA5A5) // red-200
                        ExpiryUrgency.WARNING -> Color(0xFFFDE68A) // amber-200
                        ExpiryUrgency.FRESH -> SurfaceBorder
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onCardClick?.invoke() ?: onEditClick?.invoke() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Block: Food Thumbnail + Corner Badge + Meta details
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Thumbnail container with urgency corner indicator
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceSubtle)
                            .border(width = 1.dp, color = SurfaceBorder, shape = RoundedCornerShape(12.dp))
                    ) {
                        OnlineFoodImage(
                            productName = item.name,
                            category = item.category,
                            imageUri = item.imageUri,
                            modifier = Modifier.size(52.dp)
                        )

                        // Bottom-right corner urgency icon tag
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp))
                                .background(urgencyColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = cornerIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    // Metadata
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SlateNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Category Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceSubtle)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.category.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = SlateMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Expiry Status line with dot
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(urgencyColor)
                            )
                            Text(
                                text = urgencyText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = urgencyColor
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Storage zone / quantity
                        Text(
                            text = "Qty: ${item.quantity} • Door rack / shelf",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Block: Radial Progress Urgency Ring & Edit Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShelfLifeProgressRing(
                        expiryTimestamp = item.expiryTimestamp,
                        addedTimestamp = item.addedTimestamp,
                        urgency = item.urgency,
                        size = 44.dp,
                        strokeWidth = 3.5.dp
                    )

                    if (onEditClick != null) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceSubtle)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Item",
                                tint = EmeraldDark,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
