package com.boodschappen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.boodschappen.app.data.local.Category
import com.boodschappen.app.data.local.ShoppingItem
import com.boodschappen.app.ui.theme.*
import com.boodschappen.app.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    viewModel: ShoppingViewModel,
    itemId: Long?,
    onNavigateBack: () -> Unit
) {
    val isEditing = itemId != null
    val isDark    by viewModel.isDarkTheme.collectAsState()

    var existingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var name         by remember { mutableStateOf("") }
    var quantity     by remember { mutableStateOf("1") }
    var unit         by remember { mutableStateOf("") }
    var selectedCat  by remember { mutableStateOf(Category.OVERIG) }
    var note         by remember { mutableStateOf("") }
    var imageUrl     by remember { mutableStateOf<String?>(null) }
    var brand        by remember { mutableStateOf<String?>(null) }
    var barcode      by remember { mutableStateOf<String?>(null) }

    val scanState by viewModel.scanState.collectAsState()

    LaunchedEffect(itemId) {
        if (itemId != null) {
            viewModel.getItemById(itemId)?.let {
                existingItem = it; name = it.name; quantity = it.quantity
                unit = it.unit; selectedCat = Category.fromName(it.category)
                note = it.note; imageUrl = it.imageUrl; brand = it.brand; barcode = it.barcode
            }
        }
    }
    LaunchedEffect(scanState) {
        val s = scanState
        if (s is com.boodschappen.app.viewmodel.ScanState.Found && !isEditing) {
            name = s.product.getBestName() ?: ""
            brand = s.product.brands?.split(",")?.firstOrNull()?.trim()
            imageUrl = s.product.getBestImage()
            barcode = s.barcode
            viewModel.resetScanState()
        }
    }

    val nameValid = name.isNotBlank()
    var showCatPicker  by remember { mutableStateOf(false) }
    var showUnitPicker by remember { mutableStateOf(false) }
    val catColor = categoryColor(selectedCat.displayName, isDark)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBackground(isDark))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .glassCard(isDark, RoundedCornerShape(24.dp)),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDark) Color(0x22FFFFFF) else Color(0x99FFFFFF)
                    ),
                    title = {
                        Text(
                            if (isEditing) "✏️ Bewerken" else "➕ Item toevoegen",
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF1A1040)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Terug",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ── Product image preview ─────────────────────────────────────
                if (imageUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .glassCard(isDark, RoundedCornerShape(24.dp))
                    ) {
                        AsyncImage(
                            model = imageUrl, contentDescription = name,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                        )
                        // Glass overlay top-right close button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { imageUrl = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White,
                                modifier = Modifier.size(16.dp))
                        }
                        if (brand != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(brand!!, style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }

                // ── Name field ────────────────────────────────────────────────
                GlassTextField(
                    value           = name,
                    onValueChange   = { name = it },
                    label           = "Productnaam *",
                    placeholder     = "bijv. Melk, Brood, Appels...",
                    leadingEmoji    = "🏷️",
                    isDark          = isDark,
                    trailingIcon    = if (name.isNotBlank()) {
                        { IconButton(onClick = { name = "" }) {
                            Icon(Icons.Default.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }}
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                // ── Quantity + Unit ───────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassTextField(
                        value         = quantity,
                        onValueChange = { quantity = it },
                        label         = "Aantal",
                        isDark        = isDark,
                        modifier      = Modifier.weight(0.45f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction    = ImeAction.Next
                        )
                    )

                    ExposedDropdownMenuBox(
                        expanded = showUnitPicker,
                        onExpandedChange = { showUnitPicker = it },
                        modifier = Modifier.weight(0.55f)
                    ) {
                        GlassTextField(
                            value         = unit.ifBlank { "stuk" },
                            onValueChange = {},
                            label         = "Eenheid",
                            isDark        = isDark,
                            readOnly      = true,
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(showUnitPicker) },
                            modifier      = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = showUnitPicker, onDismissRequest = { showUnitPicker = false }) {
                            listOf("stuk","kg","gram","liter","ml","pak","fles","blik","zak","doos").forEach { u ->
                                DropdownMenuItem(
                                    text    = { Text(u) },
                                    onClick = { unit = if (u == "stuk") "" else u; showUnitPicker = false }
                                )
                            }
                        }
                    }
                }

                // ── Category picker ───────────────────────────────────────────
                ExposedDropdownMenuBox(
                    expanded = showCatPicker,
                    onExpandedChange = { showCatPicker = it }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isDark) catColor.copy(alpha = 0.12f)
                                else catColor.copy(alpha = 0.08f)
                            )
                            .border(1.dp, catColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        GlassTextField(
                            value         = "${selectedCat.emoji} ${selectedCat.displayName}",
                            onValueChange = {},
                            label         = "Categorie",
                            isDark        = isDark,
                            readOnly      = true,
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(showCatPicker) },
                            modifier      = Modifier.menuAnchor().fillMaxWidth(),
                            overrideBg    = Color.Transparent
                        )
                    }
                    ExposedDropdownMenu(expanded = showCatPicker, onDismissRequest = { showCatPicker = false }) {
                        Category.entries.forEach { cat ->
                            val cc = categoryColor(cat.displayName, isDark)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(8.dp).clip(CircleShape).background(cc)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text("${cat.emoji} ${cat.displayName}",
                                            color = if (selectedCat == cat) cc else MaterialTheme.colorScheme.onSurface)
                                    }
                                },
                                onClick = { selectedCat = cat; showCatPicker = false },
                                leadingIcon = if (selectedCat == cat) {
                                    { Icon(Icons.Default.Check, null, tint = cc) }
                                } else null
                            )
                        }
                    }
                }

                // ── Note field ────────────────────────────────────────────────
                GlassTextField(
                    value         = note,
                    onValueChange = { note = it },
                    label         = "Notitie (optioneel)",
                    placeholder   = "bijv. Biologisch, Aanbieding...",
                    isDark        = isDark,
                    maxLines      = 3,
                    leadingIcon   = {
                        Icon(Icons.AutoMirrored.Outlined.Notes, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )

                // ── Barcode badge ─────────────────────────────────────────────
                if (barcode != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Cyan80.copy(alpha = 0.15f) else Cyan40.copy(alpha = 0.10f))
                            .border(1.dp, if (isDark) Cyan80.copy(alpha = 0.3f) else Cyan40.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.QrCode, null,
                            tint = if (isDark) Cyan80 else Cyan40, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Barcode", style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Cyan80 else Cyan40, fontWeight = FontWeight.SemiBold)
                            Text(barcode!!, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Save button with gradient ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (nameValid) Brush.linearGradient(
                                listOf(
                                    if (isDark) Violet80 else Violet40,
                                    if (isDark) Emerald80 else Emerald40
                                )
                            ) else Brush.linearGradient(
                                listOf(Color.Gray.copy(0.3f), Color.Gray.copy(0.3f))
                            )
                        )
                        .clickable(enabled = nameValid) {
                            val item = if (isEditing && existingItem != null) {
                                existingItem!!.copy(
                                    name = name.trim(), quantity = quantity.trim(),
                                    unit = unit.trim(), category = selectedCat.displayName,
                                    note = note.trim(), imageUrl = imageUrl,
                                    brand = brand, barcode = barcode
                                )
                            } else {
                                ShoppingItem(
                                    name = name.trim(), quantity = quantity.trim(),
                                    unit = unit.trim(), category = selectedCat.displayName,
                                    note = note.trim(), imageUrl = imageUrl,
                                    brand = brand, barcode = barcode
                                )
                            }
                            if (isEditing) viewModel.updateItem(item) else viewModel.addItem(item)
                            onNavigateBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Add,
                            null,
                            tint = if (nameValid) Color.White else Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (isEditing) "Opslaan" else "Toevoegen",
                            style  = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color  = if (nameValid) Color.White else Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Glass text field ──────────────────────────────────────────────────────────

@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isDark: Boolean,
    leadingEmoji: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    readOnly: Boolean = false,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    overrideBg: Color? = null
) {
    OutlinedTextField(
        value          = value,
        onValueChange  = onValueChange,
        label          = { Text(label, style = MaterialTheme.typography.labelMedium) },
        placeholder    = if (placeholder.isNotBlank()) { { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) } } else null,
        leadingIcon    = leadingEmoji?.let { emoji ->
            { Text(emoji, fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp)) }
        } ?: leadingIcon,
        trailingIcon   = trailingIcon,
        modifier       = modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(16.dp),
        readOnly       = readOnly,
        maxLines       = maxLines,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor   = overrideBg ?: (if (isDark) Dark700 else Color.White.copy(alpha = 0.8f)),
            unfocusedContainerColor = overrideBg ?: (if (isDark) Dark800.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)),
            focusedBorderColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            unfocusedBorderColor    = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFD4CCFF),
            focusedLabelColor       = MaterialTheme.colorScheme.primary,
            cursorColor             = MaterialTheme.colorScheme.primary
        )
    )
}
