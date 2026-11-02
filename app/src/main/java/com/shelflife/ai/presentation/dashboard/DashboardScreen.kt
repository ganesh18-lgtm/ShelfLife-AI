package com.shelflife.ai.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.presentation.components.EcoCockpitRow
import com.shelflife.ai.presentation.components.ShelfLifeTopBar
import com.shelflife.ai.presentation.scanner.AddItemBottomSheet
import com.shelflife.ai.presentation.theme.BackgroundCanvas
import com.shelflife.ai.presentation.theme.CriticalCrimson
import com.shelflife.ai.presentation.theme.CriticalCrimsonSoft
import com.shelflife.ai.presentation.theme.EmeraldDark
import com.shelflife.ai.presentation.theme.EmeraldPrimary
import com.shelflife.ai.presentation.theme.EmeraldSoft
import com.shelflife.ai.presentation.theme.SlateMuted
import com.shelflife.ai.presentation.theme.SlateNavy
import com.shelflife.ai.presentation.theme.SlateSubtle
import com.shelflife.ai.presentation.theme.SlateText
import com.shelflife.ai.presentation.theme.SurfaceBorder
import com.shelflife.ai.presentation.theme.SurfaceSubtle
import com.shelflife.ai.presentation.theme.SurfaceWhite
import com.shelflife.ai.presentation.theme.WarningAmberDark

/**
 * Replicated Pantry Dashboard Screen matching pantry_dashboard/code.html:
 * [Top App Bar] -> [Micro-Stats Eco Cockpit] -> [Search & Filter Pills] -> [Pantry Stock List] -> [Zero-State Demo] -> [FAB]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddSheet by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<PantryItem?>(null) }
    var itemToEdit by remember { mutableStateOf<PantryItem?>(null) }
    var isZeroStateExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCanvas),
        containerColor = BackgroundCanvas,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ShelfLifeTopBar(
                subtitle = "Home",
                onSearchClick = null,
                onProfileClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    itemToEdit = null
                    showAddSheet = true
                },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New",
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = "Add New",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Micro-Stats / Eco Cockpit Row (Zero Waste 94%, Saved $42.50, CO2 Offset 8.2kg)
            EcoCockpitRow(
                totalItems = uiState.totalCount,
                freshCount = uiState.freshCount,
                criticalCount = uiState.criticalCount
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Search Field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                placeholder = {
                    Text(
                        text = "Search pantry items...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = SlateMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SlateMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = SlateMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite,
                    focusedTextColor = SlateNavy,
                    unfocusedTextColor = SlateNavy
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Urgency Horizontal Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Chip
                FilterPill(
                    label = "All (${uiState.totalCount})",
                    isSelected = uiState.selectedUrgency == null,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    onClick = { viewModel.setUrgencyFilter(null) }
                )

                // Critical Chip
                FilterPill(
                    label = "Critical (${uiState.criticalCount})",
                    isSelected = uiState.selectedUrgency == ExpiryUrgency.CRITICAL,
                    dotColor = CriticalCrimson,
                    onClick = {
                        viewModel.setUrgencyFilter(
                            if (uiState.selectedUrgency == ExpiryUrgency.CRITICAL) null else ExpiryUrgency.CRITICAL
                        )
                    }
                )

                // Warning / Expiring Soon Chip
                FilterPill(
                    label = "Expiring Soon (${uiState.warningCount})",
                    isSelected = uiState.selectedUrgency == ExpiryUrgency.WARNING,
                    dotColor = WarningAmberDark,
                    onClick = {
                        viewModel.setUrgencyFilter(
                            if (uiState.selectedUrgency == ExpiryUrgency.WARNING) null else ExpiryUrgency.WARNING
                        )
                    }
                )

                // Fresh Chip
                FilterPill(
                    label = "Fresh (${uiState.freshCount})",
                    isSelected = uiState.selectedUrgency == ExpiryUrgency.FRESH,
                    dotColor = EmeraldPrimary,
                    onClick = {
                        viewModel.setUrgencyFilter(
                            if (uiState.selectedUrgency == ExpiryUrgency.FRESH) null else ExpiryUrgency.FRESH
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Pantry Stock Header with Attention Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pantry Stock",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SlateNavy
                    )
                    Text(
                        text = "${uiState.items.size} ITEMS LOGGED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = SlateMuted
                    )
                }

                if (uiState.criticalCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(CriticalCrimsonSoft)
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(9999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(CriticalCrimson)
                            )
                            Text(
                                text = "${uiState.criticalCount} attention",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = CriticalCrimson
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Inventory Items List or Zero-State
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = EmeraldPrimary,
                            strokeWidth = 3.dp
                        )
                    }
                }
                uiState.items.isEmpty() -> {
                    ZeroStateView(
                        onQuickScan = onNavigateToScanner,
                        onAddManual = {
                            itemToEdit = null
                            showAddSheet = true
                        }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 96.dp)
                    ) {
                        items(
                            items = uiState.items,
                            key = { it.id }
                        ) { item ->
                            PantryItemCard(
                                item = item,
                                onQuantityChange = { delta -> viewModel.updateQuantity(item, delta) },
                                onDelete = { viewModel.deleteItem(item) },
                                onEditClick = {
                                    selectedItemForDetail = item
                                }
                            )
                        }

                        // Collapsible Zero-State Demo Preview from model design
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            CollapsibleFridgeDemoCard(
                                isExpanded = isZeroStateExpanded,
                                onToggle = { isZeroStateExpanded = !isZeroStateExpanded },
                                onQuickScan = onNavigateToScanner
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Item Details & Storage Intelligence
    selectedItemForDetail?.let { item ->
        ItemDetailBottomSheet(
            item = item,
            onDismiss = { selectedItemForDetail = null },
            onEditClick = {
                itemToEdit = item
                showAddSheet = true
            },
            onDeleteClick = {
                viewModel.deleteItem(item)
                selectedItemForDetail = null
            }
        )
    }

    // Modal Bottom Sheet for Item Creation / Editing
    if (showAddSheet) {
        AddItemBottomSheet(
            itemToEdit = itemToEdit,
            onDismiss = {
                showAddSheet = false
                itemToEdit = null
            },
            onSave = { name, category, expiryTimestamp, barcode, quantity, mfgDateTimestamp, expiryMode, imageUri ->
                if (itemToEdit != null) {
                    val updated = itemToEdit!!.copy(
                        name = name,
                        category = com.shelflife.ai.domain.model.Category.fromString(category),
                        expiryTimestamp = expiryTimestamp,
                        barcode = barcode,
                        quantity = quantity,
                        mfgDateTimestamp = mfgDateTimestamp,
                        expiryMode = expiryMode,
                        imageUri = imageUri ?: itemToEdit!!.imageUri
                    )
                    viewModel.updatePantryItem(updated)
                } else {
                    viewModel.addPantryItem(
                        name = name,
                        category = category,
                        expiryTimestamp = expiryTimestamp,
                        barcode = barcode,
                        quantity = quantity,
                        mfgDateTimestamp = mfgDateTimestamp,
                        expiryMode = expiryMode,
                        imageUri = imageUri
                    )
                }
            }
        )
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    icon: (@Composable () -> Unit)? = null,
    dotColor: Color? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(if (isSelected) EmeraldPrimary else SurfaceWhite)
            .border(
                width = 1.dp,
                color = if (isSelected) EmeraldPrimary else SurfaceBorder,
                shape = RoundedCornerShape(9999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                icon()
            } else if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else dotColor)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) Color.White else SlateNavy
            )
        }
    }
}

@Composable
private fun ZeroStateView(
    onQuickScan: () -> Unit,
    onAddManual: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceSubtle)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Kitchen,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Pantry completely clear!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = SlateNavy
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your shelves are pristine. Tap the scanner reticle below or add a grocery item to populate fresh stock.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = SlateMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onQuickScan,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quick Scan Mode", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onAddManual,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceSubtle,
                            contentColor = SlateNavy
                        )
                    ) {
                        Text("Add Manual", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleFridgeDemoCard(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onQuickScan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Kitchen, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Zero-State Preview",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = SlateNavy
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = SlateMuted
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your shelves are pristine. Use AR vision to scan products and expiry dates with zero data entry friction.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = SlateMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onQuickScan,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSoft,
                            contentColor = EmeraldDark
                        )
                    ) {
                        Text("Launch AR Scanner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
