package com.boodschappen.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boodschappen.app.data.local.Category
import com.boodschappen.app.data.local.ShoppingItem
import com.boodschappen.app.viewmodel.ShoppingViewModel
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
    val uiState by viewModel.uiState.collectAsState()
    val syncMode by viewModel.syncMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteCheckedDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Boodschappen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        val unchecked = uiState.items.count { !it.isChecked }
                        if (uiState.items.isNotEmpty()) {
                            Text(
                                "$unchecked te halen",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Live sync indicator
                    if (syncMode is SyncMode.Shared) {
                        LiveBadge(
                            code = (syncMode as SyncMode.Shared).code,
                            onClick = { showShareSheet = true }
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { viewModel.toggleShowChecked() }) {
                        Icon(
                            if (uiState.showChecked) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Filter"
                        )
                    }
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(
                            if (syncMode is SyncMode.Shared) Icons.Filled.PeopleAlt else Icons.Outlined.PeopleAlt,
                            contentDescription = "Delen",
                            tint = if (syncMode is SyncMode.Shared)
                                MaterialTheme.colorScheme.primary
                            else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { viewModel.shareList(context) }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Export")
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Afgestreept verwijderen") },
                            leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                            onClick = { menuExpanded = false; showDeleteCheckedDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Alles verwijderen") },
                            leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null) },
                            onClick = { menuExpanded = false; showDeleteDialog = true }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onScanBarcode,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) { Icon(Icons.Outlined.QrCodeScanner, "Scannen") }
                FloatingActionButton(
                    onClick = onAddItem,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Default.Add, "Toevoegen") }
            }
        }
    ) { padding ->

        if (uiState.items.isEmpty()) {
            EmptyListPlaceholder(
                modifier = Modifier.fillMaxSize().padding(padding),
                onAddItem = onAddItem,
                onScanBarcode = onScanBarcode
            )
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                CategoryFilterRow(selectedCategory = uiState.filterCategory, onSelectCategory = viewModel::setFilterCategory)

                val checkedCount = uiState.items.count { it.isChecked }
                val totalCount = uiState.items.size
                if (totalCount > 0) {
                    LinearProgressIndicator(
                        progress = { if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }

                val grouped = uiState.items.groupBy { it.category }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    grouped.forEach { (category, items) ->
                        item { CategoryHeader(category = category, count = items.count { !it.isChecked }) }
                        items(items, key = { it.id }) { item ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                ShoppingItemRow(
                                    item = item,
                                    onChecked = { viewModel.toggleChecked(item) },
                                    onEdit = { onEditItem(item.id) },
                                    onDelete = { viewModel.deleteItem(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Share bottom sheet ─────────────────────────────────────────────────────
    if (showShareSheet) {
        ShareSheet(viewModel = viewModel, onDismiss = { showShareSheet = false })
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Alles verwijderen?") },
            text = { Text("Weet je zeker dat je de hele lijst wilt wissen?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { viewModel.deleteCheckedItems() }
                    showDeleteDialog = false
                }) { Text("Verwijderen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Annuleren") } }
        )
    }
    if (showDeleteCheckedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCheckedDialog = false },
            title = { Text("Afgestreept verwijderen?") },
            text = { Text("Alle afgestreepte items worden verwijderd.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCheckedItems()
                    showDeleteCheckedDialog = false
                }) { Text("Verwijderen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteCheckedDialog = false }) { Text("Annuleren") } }
        )
    }
}

// ── Live sync badge in toolbar ─────────────────────────────────────────────────

@Composable
fun LiveBadge(code: String, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "dot"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF4CAF50).copy(alpha = 0.15f)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(7.dp).background(Color(0xFF4CAF50).copy(alpha = alpha), CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                code,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
        }
    }
}

// ── Category header ────────────────────────────────────────────────────────────

@Composable
fun CategoryHeader(category: String, count: Int) {
    val cat = Category.fromName(category)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(cat.emoji, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(cat.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.weight(1f))
        if (count > 0) {
            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text("$count", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

// ── Item row met swipe-to-delete ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onChecked: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                label = "swipe_bg"
            )
            Box(
                Modifier.fillMaxSize().background(color).padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, "Verwijderen", tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp).clickable { onEdit() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (item.isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (item.isChecked) 0.dp else 2.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onChecked() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                if (item.imageUrl != null) {
                    coil.compose.AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (!item.isChecked) FontWeight.Medium else FontWeight.Normal,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                        color = if (item.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (item.brand != null || (item.quantity.isNotBlank() && item.quantity != "1")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.brand != null) {
                                Text(item.brand, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (item.quantity.isNotBlank() && item.quantity != "1") Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (item.quantity.isNotBlank() && item.quantity != "1") {
                                Text("${item.quantity}${if (item.unit.isNotBlank()) " ${item.unit}" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (item.note.isNotBlank()) Text(item.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                if (item.quantity.isNotBlank() && item.quantity != "1") {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "${item.quantity}${if (item.unit.isNotBlank()) "\n${item.unit}" else ""}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ── Category filter chips ──────────────────────────────────────────────────────

@Composable
fun CategoryFilterRow(selectedCategory: String?, onSelectCategory: (String?) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text("Alles") },
                leadingIcon = if (selectedCategory == null) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
            )
        }
        items(Category.entries) { cat ->
            FilterChip(
                selected = selectedCategory == cat.displayName,
                onClick = { onSelectCategory(if (selectedCategory == cat.displayName) null else cat.displayName) },
                label = { Text("${cat.emoji} ${cat.displayName}") },
                leadingIcon = if (selectedCategory == cat.displayName) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
            )
        }
    }
}

// ── Lege lijst placeholder ─────────────────────────────────────────────────────

@Composable
fun EmptyListPlaceholder(modifier: Modifier = Modifier, onAddItem: () -> Unit, onScanBarcode: () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(120.dp).background(
                brush = Brush.radialGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))),
                shape = CircleShape
            ),
            contentAlignment = Alignment.Center
        ) { Text("🛒", fontSize = 52.sp) }
        Spacer(Modifier.height(24.dp))
        Text("Je lijst is leeg", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Voeg items toe of scan een barcode\nom producten snel te herkennen", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onScanBarcode) {
                Icon(Icons.Outlined.QrCodeScanner, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scannen")
            }
            Button(onClick = onAddItem) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Toevoegen")
            }
        }
    }
}
