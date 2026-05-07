package com.boodschappen.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boodschappen.app.data.local.Category
import com.boodschappen.app.data.local.ShoppingDatabase
import com.boodschappen.app.data.local.ShoppingItem
import com.boodschappen.app.data.remote.AppVersion
import com.boodschappen.app.data.remote.FirestoreRepository
import com.boodschappen.app.data.remote.UpdateRepository
import com.boodschappen.app.data.remote.OpenFoodFactsApi
import com.boodschappen.app.data.remote.ProductDto
import com.boodschappen.app.data.repository.ShoppingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// ── Sync state ────────────────────────────────────────────────────────────────

sealed class SyncMode {
    object Local : SyncMode()
    data class Shared(val code: String) : SyncMode()
}

sealed class ShareUiState {
    object Idle : ShareUiState()
    object Loading : ShareUiState()
    data class Active(val code: String) : ShareUiState()
    data class Error(val message: String) : ShareUiState()
}

// ── General UI state ──────────────────────────────────────────────────────────

data class UiState(
    val items: List<ShoppingItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val filterCategory: String? = null,
    val showChecked: Boolean = true
)

// ── Update state ──────────────────────────────────────────────────────────────

sealed class UpdateState {
    object Idle : UpdateState()
    data class Available(val version: AppVersion) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    object ReadyToInstall : UpdateState()
    data class Error(val message: String) : UpdateState()
}

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Found(val product: ProductDto, val barcode: String) : ScanState()
    data class NotFound(val barcode: String) : ScanState()
    data class Error(val message: String) : ScanState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("boodschappen_prefs", Context.MODE_PRIVATE)

    private val localRepo: ShoppingRepository
    private val firestoreRepo  = FirestoreRepository()
    private val updateRepo     = UpdateRepository(application)

    // ── Update ────────────────────────────────────────────────────────────────
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun checkForUpdate(currentVersionCode: Int) {
        viewModelScope.launch {
            val latest = updateRepo.fetchLatestVersion() ?: return@launch
            if (latest.versionCode > currentVersionCode) {
                _updateState.value = UpdateState.Available(latest)
            }
        }
    }

    fun downloadAndInstall(url: String) {
        _updateState.value = UpdateState.Downloading(0)
        viewModelScope.launch {
            val file = updateRepo.downloadApk(url) { progress ->
                _updateState.value = UpdateState.Downloading(progress)
            }
            if (file != null) {
                _updateState.value = UpdateState.ReadyToInstall
                updateRepo.installApk(file)
            } else {
                _updateState.value = UpdateState.Error("Download mislukt, probeer opnieuw")
            }
        }
    }

    fun dismissUpdate() { _updateState.value = UpdateState.Idle }

    // ── Theme ─────────────────────────────────────────────────────────────────
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val new = !_isDarkTheme.value
        _isDarkTheme.value = new
        prefs.edit().putBoolean("dark_theme", new).apply()
    }

    // Items kept in memory while in shared mode
    private val _sharedItems = MutableStateFlow<List<ShoppingItem>>(emptyList())

    private var roomObserveJob: Job? = null
    private var mqttSyncJob: Job? = null

    // ── Sync mode ─────────────────────────────────────────────────────────────
    private val savedCode = prefs.getString("list_code", null)
    private val _syncMode = MutableStateFlow<SyncMode>(
        if (savedCode != null) SyncMode.Shared(savedCode) else SyncMode.Local
    )
    val syncMode: StateFlow<SyncMode> = _syncMode.asStateFlow()

    // ── Share sheet state ─────────────────────────────────────────────────────
    private val _shareUiState = MutableStateFlow<ShareUiState>(
        if (savedCode != null) ShareUiState.Active(savedCode) else ShareUiState.Idle
    )
    val shareUiState: StateFlow<ShareUiState> = _shareUiState.asStateFlow()

    // ── Item list ─────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Scan state ────────────────────────────────────────────────────────────
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // ── Snackbar ──────────────────────────────────────────────────────────────
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        val db = ShoppingDatabase.getDatabase(application)
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
        val api = Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(client).addConverterFactory(GsonConverterFactory.create()).build()
            .create(OpenFoodFactsApi::class.java)
        localRepo = ShoppingRepository(db.shoppingDao(), api)

        observeSyncMode()
    }

    // ── Mode switching ────────────────────────────────────────────────────────

    private fun observeSyncMode() {
        viewModelScope.launch {
            _syncMode.collect { mode ->
                when (mode) {
                    is SyncMode.Local -> {
                        mqttSyncJob?.cancel(); mqttSyncJob = null
                        roomObserveJob?.cancel()
                        roomObserveJob = viewModelScope.launch {
                            localRepo.allItems.collect { items -> updateDisplayedItems(items) }
                        }
                    }
                    is SyncMode.Shared -> {
                        roomObserveJob?.cancel(); roomObserveJob = null
                        startMqttSync(mode.code)
                    }
                }
            }
        }
    }

    private fun startMqttSync(code: String) {
        mqttSyncJob?.cancel()
        mqttSyncJob = viewModelScope.launch {
            firestoreRepo.getItemsFlow(code).catch { /* stay on last state */ }.collect { items ->
                _sharedItems.value = items
                updateDisplayedItems(items)
            }
        }
    }

    private fun updateDisplayedItems(rawItems: List<ShoppingItem>) {
        _uiState.update { state ->
            val filtered = if (state.filterCategory != null)
                rawItems.filter { it.category == state.filterCategory }
            else rawItems
            val displayed = if (!state.showChecked) filtered.filter { !it.isChecked } else filtered
            state.copy(items = displayed)
        }
    }

    // ── Share / Join ──────────────────────────────────────────────────────────

    fun createSharedList() {
        _shareUiState.value = ShareUiState.Loading
        viewModelScope.launch {
            try {
                val code = FirestoreRepository.generateCode()
                val currentItems = _uiState.value.items
                _sharedItems.value = currentItems
                firestoreRepo.createList(code, currentItems)
                prefs.edit().putString("list_code", code).apply()
                _syncMode.value = SyncMode.Shared(code)
                _shareUiState.value = ShareUiState.Active(code)
                _snackbarMessage.emit("Gedeelde lijst aangemaakt: $code")
            } catch (e: Exception) {
                _shareUiState.value = ShareUiState.Error("Kon lijst niet aanmaken: ${e.message}")
            }
        }
    }

    fun joinSharedList(code: String) {
        val trimmed = code.trim().uppercase()
        if (trimmed.length != 6) {
            _shareUiState.value = ShareUiState.Error("Code moet 6 tekens zijn")
            return
        }
        _shareUiState.value = ShareUiState.Loading
        viewModelScope.launch {
            try {
                if (!firestoreRepo.listExists(trimmed)) {
                    _shareUiState.value = ShareUiState.Error("Lijst \"$trimmed\" niet gevonden")
                    return@launch
                }
                prefs.edit().putString("list_code", trimmed).apply()
                _syncMode.value = SyncMode.Shared(trimmed)
                _shareUiState.value = ShareUiState.Active(trimmed)
                _snackbarMessage.emit("Verbonden met lijst $trimmed")
            } catch (e: Exception) {
                _shareUiState.value = ShareUiState.Error("Verbinding mislukt: ${e.message}")
            }
        }
    }

    fun stopSharing() {
        mqttSyncJob?.cancel(); mqttSyncJob = null
        prefs.edit().remove("list_code").apply()
        _sharedItems.value = emptyList()
        _syncMode.value = SyncMode.Local
        _shareUiState.value = ShareUiState.Idle
    }

    fun resetShareError() {
        _shareUiState.value = ShareUiState.Idle
    }

    // ── CRUD — routes to Room or MQTT depending on mode ───────────────────────

    fun addItem(item: ShoppingItem) {
        viewModelScope.launch {
            when (val mode = _syncMode.value) {
                is SyncMode.Local -> {
                    localRepo.addItem(item)
                    _snackbarMessage.emit("${item.name} toegevoegd")
                }
                is SyncMode.Shared -> {
                    val newItem = item.copy(id = FirestoreRepository.newItemId())
                    val updated = _sharedItems.value + newItem
                    _sharedItems.value = updated
                    updateDisplayedItems(updated)
                    try {
                        firestoreRepo.publishList(mode.code, updated)
                        _snackbarMessage.emit("${item.name} toegevoegd")
                    } catch (e: Exception) {
                        _snackbarMessage.emit("Fout: ${e.message}")
                    }
                }
            }
        }
    }

    fun updateItem(item: ShoppingItem) {
        viewModelScope.launch {
            when (val mode = _syncMode.value) {
                is SyncMode.Local -> localRepo.updateItem(item)
                is SyncMode.Shared -> {
                    val updated = _sharedItems.value.map { if (it.id == item.id) item else it }
                    _sharedItems.value = updated
                    updateDisplayedItems(updated)
                    runCatching { firestoreRepo.publishList(mode.code, updated) }
                }
            }
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            when (val mode = _syncMode.value) {
                is SyncMode.Local -> {
                    localRepo.deleteItem(item)
                    _snackbarMessage.emit("${item.name} verwijderd")
                }
                is SyncMode.Shared -> {
                    val updated = _sharedItems.value.filter { it.id != item.id }
                    _sharedItems.value = updated
                    updateDisplayedItems(updated)
                    try {
                        firestoreRepo.publishList(mode.code, updated)
                        _snackbarMessage.emit("${item.name} verwijderd")
                    } catch (e: Exception) {
                        _snackbarMessage.emit("Fout: ${e.message}")
                    }
                }
            }
        }
    }

    fun toggleChecked(item: ShoppingItem) = updateItem(item.copy(isChecked = !item.isChecked))

    fun deleteCheckedItems() {
        viewModelScope.launch {
            when (val mode = _syncMode.value) {
                is SyncMode.Local -> {
                    localRepo.deleteCheckedItems()
                    _snackbarMessage.emit("Afgestreepte items verwijderd")
                }
                is SyncMode.Shared -> {
                    val updated = _sharedItems.value.filter { !it.isChecked }
                    val removed = _sharedItems.value.size - updated.size
                    _sharedItems.value = updated
                    updateDisplayedItems(updated)
                    try {
                        firestoreRepo.publishList(mode.code, updated)
                        _snackbarMessage.emit("$removed items verwijderd")
                    } catch (e: Exception) {
                        _snackbarMessage.emit("Fout: ${e.message}")
                    }
                }
            }
        }
    }

    fun setFilterCategory(category: String?) = _uiState.update { it.copy(filterCategory = category) }
    fun toggleShowChecked() = _uiState.update { it.copy(showChecked = !it.showChecked) }

    // ── Barcode scan ──────────────────────────────────────────────────────────

    fun lookupBarcode(barcode: String) {
        _scanState.value = ScanState.Scanning
        viewModelScope.launch {
            localRepo.lookupBarcode(barcode).fold(
                onSuccess = { _scanState.value = ScanState.Found(it, barcode) },
                onFailure = { _scanState.value = ScanState.NotFound(barcode) }
            )
        }
    }

    fun resetScanState() { _scanState.value = ScanState.Idle }

    // ── Share list text ───────────────────────────────────────────────────────

    fun shareList(context: Context) {
        viewModelScope.launch {
            val items = _uiState.value.items.filter { !it.isChecked }
            if (items.isEmpty()) { _snackbarMessage.emit("Niets te delen"); return@launch }
            val grouped = items.groupBy { it.category }
            val sb = StringBuilder("🛒 Boodschappenlijst\n═══════════════════")
            grouped.forEach { (cat, catItems) ->
                val emoji = Category.fromName(cat).emoji
                sb.append("\n\n$emoji $cat")
                catItems.forEach { item ->
                    val qty = if (item.quantity.isNotBlank() && item.quantity != "1")
                        " (${item.quantity}${if (item.unit.isNotBlank()) " ${item.unit}" else ""})" else ""
                    sb.append("\n  • ${item.name}$qty")
                }
            }
            val mode = _syncMode.value
            if (mode is SyncMode.Shared) {
                sb.append("\n\n─────────────────────")
                sb.append("\n🔗 Live meedoen met code: **${mode.code}**")
                sb.append("\nOpen Boodschappen App → Delen → Code invoeren")
            }
            context.startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                    putExtra(Intent.EXTRA_SUBJECT, "Boodschappenlijst")
                }, "Lijst delen via..."
            ))
        }
    }

    suspend fun getItemById(id: Long): ShoppingItem? = localRepo.getItemById(id)
}
