package com.shelflife.ai.presentation.scanner

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shelflife.ai.data.vision.DateParser
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.presentation.dashboard.DashboardViewModel
import com.shelflife.ai.presentation.theme.BackgroundCanvas
import com.shelflife.ai.presentation.theme.CriticalCrimson
import com.shelflife.ai.presentation.theme.EmeraldDark
import com.shelflife.ai.presentation.theme.EmeraldPrimary
import com.shelflife.ai.presentation.theme.EmeraldSoft
import com.shelflife.ai.presentation.theme.SlateMuted
import com.shelflife.ai.presentation.theme.SlateNavy
import com.shelflife.ai.presentation.theme.SlateText
import com.shelflife.ai.presentation.theme.SurfaceBorder
import com.shelflife.ai.presentation.theme.SurfaceSubtle
import com.shelflife.ai.presentation.theme.SurfaceWhite
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

/**
 * Dedicated Full-Page Screen for Creating and Editing Pantry Items.
 *
 * Fully persists all metadata to Room SQLite:
 * - Product Name
 * - Product Image URI (Camera snapshot or Gallery picker)
 * - Manufacturing Date (MFG date)
 * - Expiration Mode ("Best Before", "Use By", "Exact Expire Date")
 * - Dynamic Expiry Date calculation
 * - Category classification & Quantity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: DashboardViewModel,
    itemToEdit: PantryItem? = null,
    initialScannedName: String? = null,
    initialScannedBarcode: String? = null,
    initialScannedExpiry: Long? = null,
    onNavigateBack: () -> Unit,
    onItemSaved: () -> Unit
) {
    val context = LocalContext.current
    val now = remember { System.currentTimeMillis() }
    val oneDayMs = 24L * 3600 * 1000L

    val isEditMode = itemToEdit != null

    var name by remember(itemToEdit) {
        mutableStateOf(itemToEdit?.name ?: initialScannedName.orEmpty())
    }
    var barcode by remember(itemToEdit) {
        mutableStateOf(itemToEdit?.barcode ?: initialScannedBarcode)
    }
    var imageUriString by remember(itemToEdit) {
        mutableStateOf(itemToEdit?.imageUri)
    }
    var capturedBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    // Manufacturing Date (defaults to 7 days ago or edited value)
    var mfgDateTimestamp by remember(itemToEdit) {
        mutableLongStateOf(itemToEdit?.mfgDateTimestamp ?: (now - 7 * oneDayMs))
    }

    // Expiry Mode: "BEST_BEFORE", "USE_BY", "EXACT"
    var expiryMode by remember(itemToEdit) {
        mutableStateOf(itemToEdit?.expiryMode ?: "BEST_BEFORE")
    }

    // Final Expiration Date
    var expiryTimestamp by remember(itemToEdit) {
        mutableLongStateOf(
            itemToEdit?.expiryTimestamp
                ?: initialScannedExpiry
                ?: (now + 14 * oneDayMs)
        )
    }

    var selectedCategory by remember(itemToEdit) {
        mutableStateOf(itemToEdit?.category ?: Category.PRODUCE)
    }
    var quantity by remember(itemToEdit) {
        mutableIntStateOf(itemToEdit?.quantity ?: 1)
    }

    androidx.compose.runtime.LaunchedEffect(itemToEdit) {
        itemToEdit?.let {
            name = it.name
            barcode = it.barcode
            imageUriString = it.imageUri
            mfgDateTimestamp = it.mfgDateTimestamp ?: (now - 7 * oneDayMs)
            expiryMode = it.expiryMode
            expiryTimestamp = it.expiryTimestamp
            selectedCategory = it.category
            quantity = it.quantity
        }
    }

    // Dialog controllers
    var showMfgDatePicker by remember { mutableStateOf(false) }
    var showExpiryDatePicker by remember { mutableStateOf(false) }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUriString = it.toString()
            capturedBitmap = null
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            capturedBitmap = it
            // Save locally to internal storage
            try {
                val file = File(context.filesDir, "item_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                imageUriString = Uri.fromFile(file).toString()
            } catch (e: Exception) {
                imageUriString = null
            }
        }
    }

    val targetCategories = remember {
        listOf(
            "Vegetables" to Category.PRODUCE,
            "Dairy" to Category.DAIRY,
            "Meat" to Category.MEAT,
            "Bakery" to Category.BAKERY,
            "Pantry" to Category.PANTRY,
            "Beverages" to Category.BEVERAGES,
            "Frozen" to Category.FROZEN,
            "Other" to Category.OTHER
        )
    }

    // MFG Date Picker Dialog
    if (showMfgDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = mfgDateTimestamp)
        DatePickerDialog(
            onDismissRequest = { showMfgDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let {
                            mfgDateTimestamp = it
                            // Recalculate expiry if Best Before mode
                            if (expiryMode == "BEST_BEFORE") {
                                expiryTimestamp = it + 180L * oneDayMs
                            }
                        }
                        showMfgDatePicker = false
                    }
                ) {
                    Text("Confirm", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMfgDatePicker = false }) {
                    Text("Cancel", color = SlateMuted)
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    // Expiry Date Picker Dialog
    if (showExpiryDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = expiryTimestamp)
        DatePickerDialog(
            onDismissRequest = { showExpiryDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let {
                            expiryTimestamp = it
                        }
                        showExpiryDatePicker = false
                    }
                ) {
                    Text("Confirm", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryDatePicker = false }) {
                    Text("Cancel", color = SlateMuted)
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCanvas),
        containerColor = BackgroundCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEditMode) "Edit Pantry Item" else "Add New Item",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = SlateNavy
                        )
                        Text(
                            text = "Room SQLite Local Persistence",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = SlateMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SlateNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Image Capture / Selection Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (capturedBitmap != null) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Product",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else if (!imageUriString.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUriString,
                            contentDescription = "Product Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceSubtle)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Add Item Photo for Room Storage",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = SlateNavy
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    cameraLauncher.launch(null)
                                } catch (_: Throwable) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceSubtle,
                                contentColor = SlateNavy
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                try {
                                    galleryLauncher.launch("image/*")
                                } catch (_: Throwable) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceSubtle,
                                contentColor = SlateNavy
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Product Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product / Food Name", color = SlateMuted) },
                placeholder = { Text("e.g. Avocado, Whole Milk, Sourdough Bread", color = SlateMuted.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = SlateNavy,
                    unfocusedTextColor = SlateNavy,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Manufacturing Date (MFG date)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Manufacturing Date (MFG)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlateNavy
                            )
                            Text(
                                text = DateParser.formatDisplayDate(mfgDateTimestamp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldDark
                            )
                        }

                        Button(
                            onClick = { showMfgDatePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceSubtle,
                                contentColor = SlateNavy
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select Date", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick MFG presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MfgPresetChip("Today") { mfgDateTimestamp = now }
                        MfgPresetChip("Yesterday") { mfgDateTimestamp = now - oneDayMs }
                        MfgPresetChip("-3 Days") { mfgDateTimestamp = now - 3 * oneDayMs }
                        MfgPresetChip("-1 Week") { mfgDateTimestamp = now - 7 * oneDayMs }
                        MfgPresetChip("-1 Month") { mfgDateTimestamp = now - 30 * oneDayMs }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Expiration Mode Selection: Best Before / Use By / Exact Expire Date
            Text(
                text = "Expiration Mode",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = SlateNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeSelectionChip(
                    label = "Best Before",
                    subtitle = "Quality shelf-life duration",
                    isSelected = expiryMode == "BEST_BEFORE",
                    onClick = {
                        expiryMode = "BEST_BEFORE"
                        expiryTimestamp = mfgDateTimestamp + 180L * oneDayMs // default +6 months
                    }
                )

                ModeSelectionChip(
                    label = "Use By",
                    subtitle = "Strict consumption date",
                    isSelected = expiryMode == "USE_BY",
                    onClick = {
                        expiryMode = "USE_BY"
                        expiryTimestamp = now + 5L * oneDayMs // default +5 days
                    }
                )

                ModeSelectionChip(
                    label = "Exact Expire Date",
                    subtitle = "Direct printed date",
                    isSelected = expiryMode == "EXACT",
                    onClick = {
                        expiryMode = "EXACT"
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Expiry Duration / Date Picker Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when (expiryMode) {
                                    "BEST_BEFORE" -> "Calculated Expiry (Best Before)"
                                    "USE_BY" -> "Strict Expiry (Use By)"
                                    else -> "Exact Expiry Date"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SlateNavy
                            )
                            Text(
                                text = DateParser.formatDisplayDate(expiryTimestamp),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldDark
                            )
                        }

                        Button(
                            onClick = { showExpiryDatePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceSubtle,
                                contentColor = SlateNavy
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Date", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (expiryMode == "BEST_BEFORE") {
                        Text(
                            text = "Add duration from Manufacturing Date:",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MfgPresetChip("+1 Month") { expiryTimestamp = mfgDateTimestamp + 30L * oneDayMs }
                            MfgPresetChip("+3 Months") { expiryTimestamp = mfgDateTimestamp + 90L * oneDayMs }
                            MfgPresetChip("+6 Months") { expiryTimestamp = mfgDateTimestamp + 180L * oneDayMs }
                            MfgPresetChip("+1 Year") { expiryTimestamp = mfgDateTimestamp + 365L * oneDayMs }
                            MfgPresetChip("+2 Years") { expiryTimestamp = mfgDateTimestamp + 730L * oneDayMs }
                        }
                    } else if (expiryMode == "USE_BY") {
                        Text(
                            text = "Quick freshness duration from today:",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MfgPresetChip("+2 Days") { expiryTimestamp = now + 2L * oneDayMs }
                            MfgPresetChip("+4 Days") { expiryTimestamp = now + 4L * oneDayMs }
                            MfgPresetChip("+7 Days") { expiryTimestamp = now + 7L * oneDayMs }
                            MfgPresetChip("+14 Days") { expiryTimestamp = now + 14L * oneDayMs }
                        }
                    } else {
                        Text(
                            text = "Set exact date printed on packaging above.",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Category Selection
            Text(
                text = "Food Category",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = SlateNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                targetCategories.forEach { (label, category) ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSoft,
                            selectedLabelColor = EmeraldDark,
                            containerColor = SurfaceWhite,
                            labelColor = SlateText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = SurfaceBorder,
                            selectedBorderColor = EmeraldPrimary,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. Quantity Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quantity",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = SlateNavy
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    }

                    Text(
                        text = "$quantity",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SlateNavy,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    IconButton(
                        onClick = { quantity++ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 8. Barcode info if available
            if (!barcode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldSoft)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scanned Barcode: $barcode",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = EmeraldDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 9. Save to Room Database Button
            Button(
                onClick = {
                    val finalName = name.trim().ifBlank { "Pantry Item" }
                    if (isEditMode) {
                        val updated = itemToEdit!!.copy(
                            name = finalName,
                            category = selectedCategory,
                            expiryTimestamp = expiryTimestamp,
                            barcode = barcode,
                            quantity = quantity,
                            imageUri = imageUriString,
                            mfgDateTimestamp = mfgDateTimestamp,
                            expiryMode = expiryMode
                        )
                        viewModel.updatePantryItem(updated) {
                            onItemSaved()
                        }
                    } else {
                        viewModel.addPantryItem(
                            name = finalName,
                            category = selectedCategory.displayName,
                            expiryTimestamp = expiryTimestamp,
                            barcode = barcode,
                            quantity = quantity,
                            imageUri = imageUriString,
                            mfgDateTimestamp = mfgDateTimestamp,
                            expiryMode = expiryMode
                        ) {
                            onItemSaved()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) "Update in Room Database" else "Save to Pantry (SQLite Room)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MfgPresetChip(label: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall, color = SlateNavy) },
        shape = RoundedCornerShape(8.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = SurfaceSubtle
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            borderColor = SurfaceBorder,
            borderWidth = 1.dp,
            enabled = true
        )
    )
}

@Composable
private fun ModeSelectionChip(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) EmeraldSoft else SurfaceWhite)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) EmeraldPrimary else SurfaceBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = if (isSelected) EmeraldDark else SlateNavy
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isSelected) EmeraldDark.copy(alpha = 0.8f) else SlateMuted
            )
        }
    }
}
