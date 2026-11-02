package com.shelflife.ai.presentation.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.presentation.chat.ChatScreen
import com.shelflife.ai.presentation.chat.ChatViewModel
import com.shelflife.ai.presentation.dashboard.DashboardViewModel
import com.shelflife.ai.presentation.grocery.OrderGroceryScreen
import com.shelflife.ai.presentation.home.HomeScreen
import com.shelflife.ai.presentation.pantry.PantryScreen
import com.shelflife.ai.presentation.recipe.RecipeScreen
import com.shelflife.ai.presentation.recipe.RecipeViewModel
import com.shelflife.ai.presentation.scanner.AddItemBottomSheet
import com.shelflife.ai.presentation.scanner.AddItemScreen
import com.shelflife.ai.presentation.scanner.ScannerScreen
import com.shelflife.ai.presentation.scanner.ScannerViewModel
import com.shelflife.ai.presentation.settings.SettingsScreen
import com.shelflife.ai.presentation.theme.EmeraldPrimary
import com.shelflife.ai.presentation.theme.EmeraldSoft
import com.shelflife.ai.presentation.theme.SlateMuted
import com.shelflife.ai.presentation.theme.SlateNavy
import com.shelflife.ai.presentation.theme.SurfaceBorder
import com.shelflife.ai.presentation.theme.SurfaceWhite

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Scoped shared ViewModel across Pantry, Home, AddItem, and Settings
    val dashboardViewModel: DashboardViewModel = hiltViewModel()

    // Pop-up form window state (ModalBottomSheet) directly over Home, Pantry, and Scanner
    var showAddItemSheet by remember { mutableStateOf(false) }
    var itemToEditForSheet by remember { mutableStateOf<PantryItem?>(null) }
    var initialScannedData by remember { mutableStateOf<Triple<String?, String?, Long?>?>(null) }

    // Hide bottom bar on full-screen flows (like AddItem)
    val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    // Universal Pop-up Window for Adding & Editing items with zero navigation crash risk
    if (showAddItemSheet) {
        AddItemBottomSheet(
            itemToEdit = itemToEditForSheet,
            initialName = initialScannedData?.first.orEmpty(),
            initialBarcode = initialScannedData?.second,
            initialExpiryTimestamp = initialScannedData?.third,
            onDismiss = {
                showAddItemSheet = false
                itemToEditForSheet = null
                initialScannedData = null
                dashboardViewModel.clearEditingItem()
            },
            onSave = { name, category, expiryTimestamp, barcode, quantity, mfgDateTimestamp, expiryMode, imageUri ->
                if (itemToEditForSheet != null) {
                    dashboardViewModel.updatePantryItem(
                        itemToEditForSheet!!.copy(
                            name = name,
                            category = Category.fromString(category),
                            expiryTimestamp = expiryTimestamp,
                            barcode = barcode,
                            quantity = quantity,
                            mfgDateTimestamp = mfgDateTimestamp,
                            expiryMode = expiryMode,
                            imageUri = imageUri ?: itemToEditForSheet!!.imageUri
                        )
                    )
                } else {
                    dashboardViewModel.addPantryItem(
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
                showAddItemSheet = false
                itemToEditForSheet = null
                initialScannedData = null
                dashboardViewModel.clearEditingItem()
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.border(width = 1.dp, color = SurfaceBorder),
                    containerColor = SurfaceWhite,
                    contentColor = SlateNavy,
                    tonalElevation = 8.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(imageVector = screen.icon, contentDescription = screen.title)
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
                                indicatorColor = EmeraldSoft,
                                unselectedIconColor = SlateMuted,
                                unselectedTextColor = SlateMuted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Home Screen (User greeting, Eco Cockpit, Quick Action Banners, Tips)
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToAddItem = {
                        dashboardViewModel.clearEditingItem()
                        itemToEditForSheet = null
                        initialScannedData = null
                        showAddItemSheet = true
                    },
                    onNavigateToPantry = {
                        navController.navigate(Screen.Pantry.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToScanner = {
                        navController.navigate(Screen.Scanner.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToGrocery = {
                        navController.navigate(Screen.OrderGrocery.route)
                    },
                    onNavigateToRecipes = {
                        navController.navigate(Screen.Recipes.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToChat = {
                        navController.navigate(Screen.AiChat.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // 2. Pantry Screen (Dedicated Inventory, Search, Filter pills, Edit, Delete)
            composable(Screen.Pantry.route) {
                PantryScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToAddItem = {
                        dashboardViewModel.clearEditingItem()
                        itemToEditForSheet = null
                        initialScannedData = null
                        showAddItemSheet = true
                    },
                    onNavigateToEditItem = { item ->
                        dashboardViewModel.startEditingItem(item)
                        itemToEditForSheet = item
                        initialScannedData = null
                        showAddItemSheet = true
                    },
                    onNavigateToScanner = {
                        navController.navigate(Screen.Scanner.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // 3. Gemini AI Chat Screen (Live conversational pantry database assistant)
            composable(Screen.AiChat.route) {
                val chatViewModel: ChatViewModel = hiltViewModel()
                ChatScreen(viewModel = chatViewModel)
            }

            // 4. Scanner Screen (Live CameraX AR + ML Kit + Gemini 1.5 Flash Vision)
            composable(Screen.Scanner.route) {
                val scannerViewModel: ScannerViewModel = hiltViewModel()
                ScannerScreen(
                    viewModel = scannerViewModel,
                    onNavigateToAddItem = { name, barcode, expiry ->
                        dashboardViewModel.clearEditingItem()
                        itemToEditForSheet = null
                        initialScannedData = Triple(name, barcode, expiry)
                        showAddItemSheet = true
                    }
                )
            }

            // 5. AI Zero-Waste Chef (Powered by Gemini 1.5 Flash + Local Fallback)
            composable(Screen.Recipes.route) {
                val recipeViewModel: RecipeViewModel = hiltViewModel()
                RecipeScreen(
                    viewModel = recipeViewModel,
                    onNavigateToScanner = {
                        navController.navigate(Screen.Scanner.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // 6. Settings & Room DB Management
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToGrocery = {
                        navController.navigate(Screen.OrderGrocery.route)
                    }
                )
            }

            // 7. Order Grocery Screen (Blinkit, Swiggy Instamart, Zepto, Amazon Fresh)
            composable(Screen.OrderGrocery.route) {
                OrderGroceryScreen(
                    viewModel = dashboardViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // 8. Fallback Add / Edit Item Route
            composable(Screen.AddItem.route) {
                val itemToEdit by dashboardViewModel.editingItem.collectAsState()

                val previousEntry = navController.previousBackStackEntry
                val itemId = previousEntry?.savedStateHandle?.get<Long>("itemId") ?: -1L
                val nameArg = previousEntry?.savedStateHandle?.get<String>("name")
                val barcodeArg = previousEntry?.savedStateHandle?.get<String>("barcode")
                val expiryArg = previousEntry?.savedStateHandle?.get<Long>("expiry")

                androidx.compose.runtime.LaunchedEffect(itemId) {
                    if (itemId != -1L && itemToEdit == null) {
                        dashboardViewModel.loadItemToEdit(itemId)
                    }
                }

                AddItemScreen(
                    viewModel = dashboardViewModel,
                    itemToEdit = itemToEdit,
                    initialScannedName = nameArg,
                    initialScannedBarcode = barcodeArg,
                    initialScannedExpiry = expiryArg,
                    onNavigateBack = {
                        dashboardViewModel.clearEditingItem()
                        navController.popBackStack()
                    },
                    onItemSaved = {
                        dashboardViewModel.clearEditingItem()
                        if (!navController.popBackStack(Screen.Pantry.route, false)) {
                            navController.navigate(Screen.Pantry.route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Convenient navigation helper passing item metadata safely via savedStateHandle.
 */
fun NavController.navigateToAddItem(
    itemId: Long = -1L,
    name: String? = null,
    barcode: String? = null,
    expiry: Long? = null
) {
    currentBackStackEntry?.savedStateHandle?.set("itemId", itemId)
    currentBackStackEntry?.savedStateHandle?.set("name", name)
    currentBackStackEntry?.savedStateHandle?.set("barcode", barcode)
    currentBackStackEntry?.savedStateHandle?.set("expiry", expiry)
    navigate(Screen.AddItem.route) {
        launchSingleTop = true
    }
}
