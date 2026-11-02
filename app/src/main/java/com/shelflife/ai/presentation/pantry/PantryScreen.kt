package com.shelflife.ai.presentation.pantry

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
import com.shelflife.ai.presentation.components.EmptyPantryLottieAnimation
import com.shelflife.ai.presentation.components.ShelfLifeTopBar
import com.shelflife.ai.presentation.dashboard.DashboardViewModel
import com.shelflife.ai.presentation.dashboard.ItemDetailBottomSheet
import com.shelflife.ai.presentation.dashboard.PantryItemCard
import com.shelflife.ai.presentation.theme.BackgroundCanvas
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

/**
 * Dedicated Pantry Screen for managing, searching, filtering, altering,
 * updating, and deleting all inventory items stored in Room SQLite.
 */
@Composable
fun PantryScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddItem: () -> Unit,
    onNavigateToEditItem: (PantryItem) -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedItemForDetail by remember { mutableStateOf<PantryItem?>(null) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    // Handles swipe-to-delete or remove with a 5-second Undo snackbar
    val handleDeleteWithUndo: (PantryItem) -> Unit = { itemToDelete ->
        viewModel.deleteItemSilently(itemToDelete)
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val snackbarJob = async {
                snackbarHostState.showSnackbar(
                    message = "Deleted \"${itemToDelete.name}\"",
                    actionLabel = "Undo",
                    duration = androidx.compose.material3.SnackbarDuration.Indefinite
                )
            }
            val result = withTimeoutOrNull(5000L) {
                snackbarJob.await()
            }
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                viewModel.restoreItem(itemToDelete)
            } else {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCanvas),
        containerColor = BackgroundCanvas,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    containerColor = SlateNavy,
                    contentColor = Color.White,
                    actionColor = EmeraldPrimary,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        topBar = {
            ShelfLifeTopBar(
                subtitle = "Pantry Inventory",
                profilePhotoUri = userProfile.photoUri,
                onSearchClick = null,
                onProfileClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddItem,
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
                        text = "Add Item",
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

            // 1. Search Field
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
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Filter Pills (All, Critical, Expiring Soon, Fresh)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Chip
                FilterPill(
                    label = "All (${uiState.totalCount})",
                    isSelected = uiState.selectedUrgency == null && uiState.selectedCategory == null,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = if (uiState.selectedUrgency == null && uiState.selectedCategory == null) Color.White else SlateMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    onClick = {
                        viewModel.setUrgencyFilter(null)
                        viewModel.setCategoryFilter(null)
                    }
                )

                // Critical Chip
                FilterPill(
                    label = "Critical (<48h)",
                    isSelected = uiState.selectedUrgency == ExpiryUrgency.CRITICAL,
                    dotColor = CriticalCrimson,
                    onClick = {
                        viewModel.setUrgencyFilter(
                            if (uiState.selectedUrgency == ExpiryUrgency.CRITICAL) null else ExpiryUrgency.CRITICAL
                        )
                    }
                )

                // Warning Chip
                FilterPill(
                    label = "Warning (2-5d)",
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

            // 3. Pantry Stock Header
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

            // 4. Pantry Stock Items List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(36.dp))
                }
            } else if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        EmptyPantryLottieAnimation(size = 140.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Items in Pantry",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SlateNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add your groceries or scan them to start tracking expiration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToAddItem,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Item", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = uiState.items,
                        key = { it.id }
                    ) { item ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            PantryItemCard(
                                item = item,
                                onQuantityChange = { delta ->
                                    viewModel.updateQuantity(item, delta)
                                },
                                onDelete = {
                                    handleDeleteWithUndo(item)
                                },
                                onCardClick = {
                                    selectedItemForDetail = item
                                },
                                onEditClick = {
                                    onNavigateToEditItem(item)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Pop-Up Dialog for Item Information & Storage Intelligence
    selectedItemForDetail?.let { item ->
        ItemDetailBottomSheet(
            item = item,
            onDismiss = { selectedItemForDetail = null },
            onEditClick = {
                selectedItemForDetail = null
                onNavigateToEditItem(item)
            },
            onDeleteClick = {
                handleDeleteWithUndo(item)
                selectedItemForDetail = null
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
