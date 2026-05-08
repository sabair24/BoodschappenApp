package com.boodschappen.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boodschappen.app.data.local.Category
import com.boodschappen.app.data.local.ShoppingItem
import com.boodschappen.app.ui.theme.*
import com.boodschappen.app.viewmodel.ShoppingViewModel
import com.boodschappen.app.viewmodel.SortMode
import com.boodschappen.app.viewmodel.SyncMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingViewModel,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    onScanBarcode: () -> Unit
) {
    val uiState       by viewModel.uiState.collectAsState()
    val syncMode      by viewModel.syncMode.collectAsState()
    val isDark        by viewModel.isDarkTheme.collectAsState()
    val snackbar      = remember { SnackbarHostState() }
    val scope         = rememberCoroutineScope()
    val context       = LocalContext.current

    var showDeleteDialog        by remember { mutableStateOf(false) }
    var showDeleteCheckedDialog by remember { mutableStateOf(false) }
    var showShareSheet          by remember { mutableStateOf(false) }
    var searchActive            by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { snackbar.showSnackbar(it, duration = SnackbarDuration.Short) }
    }

    fun handleDelete(item: ShoppingItem) {
        viewModel.deleteItem(item, silent = true)
        scope.launch {
            val result = snackbar.showSnackbar(
                message     = "${item.name} verwijderd",
                actionLabel = "Ongedaan",
                duration    = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreItem(item)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBackground(isDark))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopBar(
                    isDark           = isDark,
                    syncMode         = syncMode,
                    itemCount        = uiState.items.count { !it.isChecked },
                    showChecked      = uiState.showChecked,
                    searchActive     = searchActive,
                    sortMode         = uiState.sortMode,
                    onToggleVisible  = { viewModel.toggleShowChecked() },
                    onToggleTheme    = { viewModel.toggleTheme() },
                    onToggleSearch   = {
                        searchActive = !searchActive
                        if (!searchActive) viewModel.setSearchQuery("")
                    },
                    onShare          = { showShareSheet = true },
                    onExport         = { viewModel.shareList(context) },
                    onDeleteChecked  = { showDeleteCheckedDialog = true },
                    onDeleteAll      = { showDeleteDialog = true },
                    onSortChange     = { viewModel.setSortMode(it) }
                )
            },
            floatingActionButton = {
                GradientFabs(isDark = isDark, onAdd = onAddItem, onScan = onScanBarcode)
            }
        ) { padding ->

            if (uiState.items.isEmpty() && uiState.searchQuery.isBlank()) {
                EmptyState(
                    modifier    = Modifier.fillMaxSize().padding(padding),
                    isDark      = isDark,
                    onAdd       = onAddItem,
                    onScan      = onScanBarcode
                )
            } else {
                Column(Modifier.fillMaxSize().padding(padding)) {

                    // ── Search bar ────────────────────────────────────────────
                    AnimatedVisibility(
                        visible = searchActive,
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut()
                    ) {
                        SearchInputBar(
                            query         = uiState.searchQuery,
                            onQueryChange = viewModel::setSearchQuery,
                            onClose       = { searchActive = false; viewModel.setSearchQuery("") },
                            isDark        = isDark
                        )
                    }

                    // ── Category filter ───────────────────────────────────────
                    CategoryChips(
                        selected  = uiState.filterCategory,
                        isDark    = isDark,
                        onSelect  = viewModel::setFilterCategory
                    )

                    // ── Progress ──────────────────────────────────────────────
                    val checked = uiState.items.count { it.isChecked }
                    val total   = uiState.items.size
                    if (total > 0) {
                        GradientProgress(checked = checked, total = total, isDark = isDark)
                        Spacer(Modifier.height(4.dp))
                    }

                    // ── Empty search result ───────────────────────────────────
                    if (uiState.items.isEmpty() && uiState.searchQuery.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔍", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Geen resultaten voor \"${uiState.searchQuery}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // ── List ──────────────────────────────────────────────────
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 4.dp, bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (uiState.sortMode == SortMode.CATEGORY) {
                            val grouped = uiState.items.groupBy { it.category }
                            grouped.forEach { (category, items) ->
                                item(key = "header_$category") {
                                    CategoryHeader(
                                        category = category,
                                        isDark   = isDark,
                                        count    = items.count { !it.isChecked }
                                    )
                                }
                                items(items, key = { it.id }) { item ->
                                    AnimatedVisibility(
                                        visible = true,
                                        enter   = fadeIn() + slideInVertically { it / 2 },
                                        exit    = fadeOut() + slideOutHorizontally()
                                    ) {
                                        GlassItemRow(
                                            item        = item,
                                            isDark      = isDark,
                                            onCheck     = { viewModel.toggleChecked(item) },
                                            onEdit      = { onEditItem(item.id) },
                                            onDelete    = { handleDelete(item) },
                                            onIncrement = {
                                                item.quantity.toDoubleOrNull()?.let { q ->
                                                    viewModel.updateItem(item.copy(quantity = formatQty(q + 1)))
                                                }
                                            },
                                            onDecrement = {
                                                item.quantity.toDoubleOrNull()?.let { q ->
                                                    if (q > 1) viewModel.updateItem(item.copy(quantity = formatQty(q - 1)))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Flat list for NAME / DATE_ADDED sort
                            items(uiState.items, key = { it.id }) { item ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter   = fadeIn() + slideInVertically { it / 2 },
                                    exit    = fadeOut() + slideOutHorizontally()
                                ) {
                                    GlassItemRow(
                                        item        = item,
                                        isDark      = isDark,
                                        onCheck     = { viewModel.toggleChecked(item) },
                                        onEdit      = { onEditItem(item.id) },
                                        onDelete    = { handleDelete(item) },
                                        onIncrement = {
                                            item.quantity.toDoubleOrNull()?.let { q ->
                                                viewModel.updateItem(item.copy(quantity = formatQty(q + 1)))
                                            }
                                        },
                                        onDecrement = {
                                            item.quantity.toDoubleOrNull()?.let { q ->
                                                if (q > 1) viewModel.updateItem(item.copy(quantity = formatQty(q - 1)))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShareSheet) ShareSheet(viewModel = viewModel, onDismiss = { showShareSheet = false })

    if (showDeleteDialog) {
        GlassDialog(
            title     = "Alles verwijderen?",
            body      = "Weet je zeker dat je de hele lijst wilt wissen?",
            isDark    = isDark,
            onConfirm = { viewModel.deleteCheckedItems(); showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false }
        )
    }
    if (showDeleteCheckedDialog) {
        GlassDialog(
            title     = "Afgestreept verwijderen?",
            body      = "Alle afgestreepte items worden verwijderd.",
            isDark    = isDark,
            onConfirm = { viewModel.deleteCheckedItems(); showDeleteCheckedDialog = false },
            onDismiss = { showDeleteCheckedDialog = false }
        )
    }
}

private fun formatQty(value: Double): String {
    val long = value.toLong()
    return if (value == long.toDouble()) long.toString() else value.toString()
}

// ── Search input bar ──────────────────────────────────────────────────────────

@Composable
private fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    isDark: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value           = query,
            onValueChange   = onQueryChange,
            placeholder     = { Text("Zoek in je lijst...") },
            leadingIcon     = {
                Icon(Icons.Default.Search, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon    = if (query.isNotBlank()) {
                { IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }}
            } else null,
            modifier        = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            shape           = RoundedCornerShape(16.dp),
            singleLine      = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = if (isDark) Dark700 else Color.White.copy(alpha = 0.9f),
                unfocusedContainerColor = if (isDark) Dark800.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f),
                focusedBorderColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                unfocusedBorderColor    = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFD4CCFF),
                focusedLabelColor       = MaterialTheme.colorScheme.primary,
                cursorColor             = MaterialTheme.colorScheme.primary
            )
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, "Sluiten",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    isDark: Boolean,
    syncMode: SyncMode,
    itemCount: Int,
    showChecked: Boolean,
    searchActive: Boolean,
    sortMode: SortMode,
    onToggleVisible: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleSearch: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDeleteChecked: () -> Unit,
    onDeleteAll: () -> Unit,
    onSortChange: (SortMode) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .glassCard(isDark, RoundedCornerShape(24.dp)),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isDark) Color(0x22FFFFFF) else Color(0x99FFFFFF)
        ),
        title = {
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    "🛒 Boodschappen",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (isDark) Color.White else Color(0xFF1A1040)
                )
                if (itemCount > 0) {
                    Text(
                        "$itemCount te halen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {
            if (syncMode is SyncMode.Shared) {
                LiveBadge(code = (syncMode as SyncMode.Shared).code, isDark = isDark, onClick = onShare)
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onToggleSearch) {
                Icon(
                    if (searchActive) Icons.Filled.Search else Icons.Outlined.Search,
                    "Zoeken",
                    tint = if (searchActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onToggleTheme) {
                Icon(
                    if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    "Thema",
                    tint = if (isDark) Amber80 else Violet40
                )
            }
            IconButton(onClick = onToggleVisible) {
                Icon(
                    if (showChecked) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    "Filter",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    if (syncMode is SyncMode.Shared) Icons.Filled.PeopleAlt else Icons.Outlined.PeopleAlt,
                    "Delen",
                    tint = if (syncMode is SyncMode.Shared)
                        if (isDark) Emerald80 else Emerald40
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, "Menu", tint = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(
                expanded         = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text        = { Text("Lijst exporteren") },
                    leadingIcon = { Icon(Icons.Outlined.Share, null) },
                    onClick     = { menuExpanded = false; onExport() }
                )
                DropdownMenuItem(
                    text        = { Text("Afgestreept verwijderen") },
                    leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                    onClick     = { menuExpanded = false; onDeleteChecked() }
                )
                DropdownMenuItem(
                    text        = { Text("Alles verwijderen", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(Icons.Outlined.DeleteSweep, null,
                            tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = { menuExpanded = false; onDeleteAll() }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "  Sorteren",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
                SortMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                mode.label,
                                fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal,
                                color = if (sortMode == mode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (sortMode == mode) Icons.Filled.RadioButtonChecked
                                else Icons.Outlined.RadioButtonUnchecked,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = if (sortMode == mode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { menuExpanded = false; onSortChange(mode) }
                    )
                }
            }
        }
    )
}

// ── Live sync badge ───────────────────────────────────────────────────────────

@Composable
fun LiveBadge(code: String, isDark: Boolean, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dot"
    )
    Surface(
        onClick    = onClick,
        shape      = RoundedCornerShape(20.dp),
        color      = if (isDark) Emerald20.copy(alpha = 0.6f) else Emerald90,
        modifier   = Modifier.padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).background(
                if (isDark) Emerald80.copy(alpha = alpha) else Emerald40.copy(alpha = alpha),
                CircleShape
            ))
            Spacer(Modifier.width(6.dp))
            Text(
                code,
                style      = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color      = if (isDark) Emerald80 else Emerald40
            )
        }
    }
}

// ── Gradient progress ─────────────────────────────────────────────────────────

@Composable
private fun GradientProgress(checked: Int, total: Int, isDark: Boolean) {
    val progress = if (total > 0) checked.toFloat() / total else 0f
    val animProg by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label         = "progress"
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "$checked / $total gedaan",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${(progress * 100).toInt()}%",
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isDark) Dark600 else Violet90)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animProg)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                if (isDark) Violet80 else Violet40,
                                if (isDark) Emerald80 else Emerald40
                            )
                        )
                    )
            )
        }
    }
}

// ── Category header ───────────────────────────────────────────────────────────

@Composable
fun CategoryHeader(category: String, count: Int, isDark: Boolean) {
    val cat   = Category.fromName(category)
    val color = categoryColor(category, isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isDark) 0.2f else 0.15f)),
            contentAlignment = Alignment.Center
        ) { Text(cat.emoji, fontSize = 16.sp) }

        Spacer(Modifier.width(10.dp))
        Text(
            cat.displayName,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = color
        )
        Spacer(Modifier.weight(1f))
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = if (isDark) 0.2f else 0.15f))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    "$count",
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
            }
        }
    }
}

// ── Glass item row ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassItemRow(
    item: ShoppingItem,
    isDark: Boolean,
    onCheck: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onIncrement: () -> Unit = {},
    onDecrement: () -> Unit = {}
) {
    val catColor     = categoryColor(item.category, isDark)
    val qtyNum       = item.quantity.toDoubleOrNull()
    val showQtyBadge = item.quantity.isNotBlank() && item.quantity != "1"

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val bg by animateColorAsState(
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                    Rose20 else Color.Transparent,
                label = "swipe"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, null, tint = Rose80)
                }
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (item.isChecked) cardCheckedBackground(isDark)
                    else cardBackground(isDark)
                )
                .glassCard(isDark, RoundedCornerShape(20.dp))
                .clickable(onClick = onEdit)
        ) {
            // Colored left accent bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(
                        topStart = 20.dp, bottomStart = 20.dp,
                        topEnd = 2.dp, bottomEnd = 2.dp
                    ))
                    .background(
                        if (item.isChecked) catColor.copy(alpha = 0.3f) else catColor
                    )
            )

            Row(
                modifier = Modifier.padding(
                    start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked         = item.isChecked,
                    onCheckedChange = { onCheck() },
                    colors = CheckboxDefaults.colors(
                        checkedColor   = catColor,
                        checkmarkColor = if (isDark) Dark900 else Color.White,
                        uncheckedColor = catColor.copy(alpha = 0.5f)
                    )
                )

                if (item.imageUrl != null) {
                    coil.compose.AsyncImage(
                        model              = item.imageUrl,
                        contentDescription = item.name,
                        modifier           = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Dark700 else Violet90),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(Modifier.width(10.dp))
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text           = item.name,
                        style          = MaterialTheme.typography.bodyLarge,
                        fontWeight     = if (!item.isChecked) FontWeight.SemiBold else FontWeight.Normal,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                        color          = if (item.isChecked)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (item.brand != null || (item.quantity.isNotBlank() && item.quantity != "1")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            item.brand?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.quantity.isNotBlank() && item.quantity != "1")
                                    Text(
                                        " · ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                            }
                            if (item.quantity.isNotBlank() && item.quantity != "1") {
                                Text(
                                    "${item.quantity}${if (item.unit.isNotBlank()) " ${item.unit}" else ""}",
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = catColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (item.note.isNotBlank()) {
                        Text(
                            item.note,
                            style   = MaterialTheme.typography.labelSmall,
                            color   = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                // ── Quantity badge with +/- controls ──────────────────────────
                if (showQtyBadge) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (qtyNum != null && qtyNum > 1) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(catColor.copy(alpha = if (isDark) 0.20f else 0.12f))
                                    .clickable(onClick = onDecrement),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Remove, null,
                                    modifier = Modifier.size(14.dp),
                                    tint     = catColor
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(catColor.copy(alpha = if (isDark) 0.2f else 0.12f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${item.quantity}${if (item.unit.isNotBlank()) "\n${item.unit}" else ""}",
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color      = catColor,
                                textAlign  = TextAlign.Center
                            )
                        }

                        if (qtyNum != null) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(catColor.copy(alpha = if (isDark) 0.20f else 0.12f))
                                    .clickable(onClick = onIncrement),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add, null,
                                    modifier = Modifier.size(14.dp),
                                    tint     = catColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Category filter chips ─────────────────────────────────────────────────────

@Composable
fun CategoryChips(selected: String?, isDark: Boolean, onSelect: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ModernChip(
                label    = "Alles",
                selected = selected == null,
                isDark   = isDark,
                color    = if (isDark) Violet80 else Violet40,
                onClick  = { onSelect(null) }
            )
        }
        items(Category.entries) { cat ->
            val color = categoryColor(cat.displayName, isDark)
            ModernChip(
                label    = "${cat.emoji} ${cat.displayName}",
                selected = selected == cat.displayName,
                isDark   = isDark,
                color    = color,
                onClick  = { onSelect(if (selected == cat.displayName) null else cat.displayName) }
            )
        }
    }
}

@Composable
private fun ModernChip(
    label: String,
    selected: Boolean,
    isDark: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (selected) color.copy(alpha = if (isDark) 0.30f else 0.20f)
                else if (isDark) Dark700 else Color.White.copy(alpha = 0.7f)
            )
            .border(
                1.dp,
                if (selected) color.copy(alpha = 0.7f)
                else if (isDark) Dark600 else Color(0xFFE0D9FF),
                RoundedCornerShape(50.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Default.Check, null,
                    modifier = Modifier.size(14.dp),
                    tint     = color)
                Spacer(Modifier.width(4.dp))
            }
            Text(
                label,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color      = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── FABs with gradient ────────────────────────────────────────────────────────

@Composable
private fun GradientFabs(isDark: Boolean, onAdd: () -> Unit, onScan: () -> Unit) {
    Column(horizontalAlignment = Alignment.End) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (isDark) Cyan80 else Cyan40,
                            if (isDark) Emerald80 else Emerald40
                        )
                    )
                )
                .clickable(onClick = onScan),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.QrCodeScanner, "Scan",
                tint     = if (isDark) Dark900 else Color.White,
                modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (isDark) Violet80 else Violet40,
                            if (isDark) Rose80.copy(alpha = 0.8f) else Rose40.copy(alpha = 0.8f)
                        )
                    )
                )
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, "Toevoegen",
                tint     = Color.White,
                modifier = Modifier.size(30.dp))
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    onAdd: () -> Unit,
    onScan: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (isDark) Violet30.copy(alpha = 0.6f) else Violet90,
                            Color.Transparent
                        )
                    )
                )
                .glassCard(isDark, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("🛒", fontSize = 58.sp) }

        Spacer(Modifier.height(28.dp))

        Text(
            "Je lijst is leeg",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Voeg items toe of scan een barcode\nom producten snel te herkennen",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(36.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onScan,
                border  = BorderStroke(1.dp, if (isDark) Cyan80 else Cyan40),
                shape   = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.QrCodeScanner, null, Modifier.size(18.dp),
                    tint = if (isDark) Cyan80 else Cyan40)
                Spacer(Modifier.width(8.dp))
                Text("Scannen", color = if (isDark) Cyan80 else Cyan40)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isDark) Violet80 else Violet40,
                                if (isDark) Rose80.copy(alpha = 0.7f) else Rose40.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Toevoegen", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Glass dialog ──────────────────────────────────────────────────────────────

@Composable
fun GlassDialog(
    title: String,
    body: String,
    isDark: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = if (isDark) Dark700 else Color.White,
        shape            = RoundedCornerShape(28.dp),
        title = {
            Text(title, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Rose40, Rose20)))
                    .clickable(onClick = onConfirm)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Verwijderen", color = Color.White, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuleren", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
