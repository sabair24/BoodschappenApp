package com.boodschappen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.boodschappen.app.data.local.Category
import com.boodschappen.app.data.local.ShoppingItem
import com.boodschappen.app.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    viewModel: ShoppingViewModel,
    itemId: Long?,
    onNavigateBack: () -> Unit
) {
    val isEditing = itemId != null
    var existingItem by remember { mutableStateOf<ShoppingItem?>(null) }

    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.OVERIG) }
    var note by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var brand by remember { mutableStateOf<String?>(null) }
    var barcode by remember { mutableStateOf<String?>(null) }

    val scanState by viewModel.scanState.collectAsState()

    // Load existing item
    LaunchedEffect(itemId) {
        if (itemId != null) {
            viewModel.getItemById(itemId)?.let { item ->
                existingItem = item
                name = item.name
                quantity = item.quantity
                unit = item.unit
                selectedCategory = Category.fromName(item.category)
                note = item.note
                imageUrl = item.imageUrl
                brand = item.brand
                barcode = item.barcode
            }
        }
    }

    // Pre-fill from scan if we have scanned data
    LaunchedEffect(scanState) {
        val state = scanState
        if (state is com.boodschappen.app.viewmodel.ScanState.Found && !isEditing) {
            val product = state.product
            name = product.getBestName() ?: ""
            brand = product.brands?.split(",")?.firstOrNull()?.trim()
            imageUrl = product.getBestImage()
            barcode = state.barcode
            viewModel.resetScanState()
        }
    }

    val nameIsValid = name.isNotBlank()
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showUnitPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Bewerken" else "Item toevoegen",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Terug")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Product image preview
            if (imageUrl != null) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ) {
                            IconButton(
                                onClick = { imageUrl = null },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, "Verwijder afbeelding", modifier = Modifier.size(18.dp))
                            }
                        }
                        if (brand != null) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            ) {
                                Text(
                                    brand!!,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Productnaam *") },
                placeholder = { Text("bijv. Melk, Brood, Appels...") },
                leadingIcon = { Text("🏷️", fontSize = 18.sp) },
                trailingIcon = if (name.isNotBlank()) {
                    { IconButton(onClick = { name = "" }) {
                        Icon(Icons.Default.Clear, null) }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = name.isBlank() && existingItem != null,
                singleLine = true
            )

            // Quantity + Unit row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Aantal") },
                    modifier = Modifier.weight(0.45f),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Tag, null, Modifier.size(18.dp)) }
                )

                ExposedDropdownMenuBox(
                    expanded = showUnitPicker,
                    onExpandedChange = { showUnitPicker = it },
                    modifier = Modifier.weight(0.55f)
                ) {
                    OutlinedTextField(
                        value = unit.ifBlank { "stuk" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Eenheid") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showUnitPicker) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = showUnitPicker,
                        onDismissRequest = { showUnitPicker = false }
                    ) {
                        listOf("stuk", "kg", "gram", "liter", "ml", "pak", "fles", "blik", "zak", "doos").forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u) },
                                onClick = { unit = if (u == "stuk") "" else u; showUnitPicker = false }
                            )
                        }
                    }
                }
            }

            // Category picker
            ExposedDropdownMenuBox(
                expanded = showCategoryPicker,
                onExpandedChange = { showCategoryPicker = it }
            ) {
                OutlinedTextField(
                    value = "${selectedCategory.emoji} ${selectedCategory.displayName}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categorie") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showCategoryPicker) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = showCategoryPicker,
                    onDismissRequest = { showCategoryPicker = false }
                ) {
                    Category.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.emoji} ${cat.displayName}") },
                            onClick = { selectedCategory = cat; showCategoryPicker = false },
                            leadingIcon = if (selectedCategory == cat) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                    }
                }
            }

            // Note field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Notitie (optioneel)") },
                placeholder = { Text("bijv. Biologisch, Aanbieding...") },
                leadingIcon = { Icon(Icons.Outlined.Notes, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                maxLines = 3
            )

            if (barcode != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.QrCode,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Barcode",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                barcode!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val item = if (isEditing && existingItem != null) {
                        existingItem!!.copy(
                            name = name.trim(),
                            quantity = quantity.trim(),
                            unit = unit.trim(),
                            category = selectedCategory.displayName,
                            note = note.trim(),
                            imageUrl = imageUrl,
                            brand = brand,
                            barcode = barcode
                        )
                    } else {
                        ShoppingItem(
                            name = name.trim(),
                            quantity = quantity.trim(),
                            unit = unit.trim(),
                            category = selectedCategory.displayName,
                            note = note.trim(),
                            imageUrl = imageUrl,
                            brand = brand,
                            barcode = barcode
                        )
                    }
                    if (isEditing) viewModel.updateItem(item) else viewModel.addItem(item)
                    onNavigateBack()
                },
                enabled = nameIsValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    if (isEditing) Icons.Default.Check else Icons.Default.Add,
                    null,
                    Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isEditing) "Opslaan" else "Toevoegen",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
