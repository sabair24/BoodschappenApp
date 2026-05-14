package com.boodschappen.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boodschappen.app.data.local.Category
import com.boodschappen.app.data.local.ShoppingDatabase
import com.boodschappen.app.data.local.ShoppingItem
import com.boodschappen.app.util.guessCategoryFromName
import com.boodschappen.app.data.remote.AppVersion
import com.boodschappen.app.data.remote.ClaudeApiService
import com.boodschappen.app.data.remote.FirestoreRepository
import com.boodschappen.app.data.remote.UpdateRepository
import com.boodschappen.app.data.remote.OpenFoodFactsApi
import com.boodschappen.app.data.remote.ProductDto
import com.boodschappen.app.data.remote.UpcItemDbApi
import com.boodschappen.app.data.repository.AiRepository
import com.boodschappen.app.data.repository.ShoppingRepository
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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

enum class SortMode(val label: String) {
    CATEGORY("Óp categorie"),
    NAME("Op naam A–Z"),
    DATE_ADDED("Nieuwste eerst")
}

data class UiState(
    val items: List<ShoppingItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val filterCategory: String? = null,
    val showChecked: Boolean = true,
    val searchQuery: String = "",
    val sortMode: SortMode = SortMode.CATEGORY
)

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
    data class NotFound(val barcode: String, val error: String? = null) : ScanState()
    data class Error(val message: String) : ScanState()
}

sealed class NameSearchState {
    object Idle : NameSearchState()
    object Searching : NameSearchState()
    data class Found(val product: ProductDto) : NameSearchState()
    object NotFound : NameSearchState()
}

sealed class AiCategoryState {
    object Idle : AiCategoryState()
    object Loading : AiCategoryState()
    data class Suggested(val category: Category) : AiCategoryState()
}

sealed class RecipeState {
    object Idle : RecipeState()
    object Loading : RecipeState()
    data class Ready(val ingredients: List<String>) : RecipeState()
    data class Error(val message: String) : RecipeState()
}

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("boodschappen_prefs", Context.MODE_PRIVATE)

    private val localRepo: ShoppingRepository
    private val upcApi: UpcItemDbApi
    private lateinit var offClient: OkHttpClient
    private val firestoreRepo  = FirestoreRepository()
    private val updateRepo     = UpdateRepository(application)
    private val aiRepo         = AiRepository(ClaudeApiService())

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

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val new = !_isDarkTheme.value
        _isDarkTheme.value = new
        prefs.edit().putBoolean("dark_theme", new).apply()
    }

    private val _sharedItems = MutableStateFlow<List<ShoppingItem>>(emptyList())
    private var roomObserveJob: Job? = null
    private var mqttSyncJob: Job? = null

    private val savedCode = prefs.getString("list_code", null)
    private val _syncMode = MutableStateFlow<SyncMode>(
        if (savedCode != null) SyncMode.Shared(savedCode) else SyncMode.Local
    )
    val syncMode: StateFlow<SyncMode> = _syncMode.asStateFlow()

    private val _shareUiState = MutableStateFlow<ShareUiState>(
        if (savedCode != null) ShareUiState.Active(savedCode) else ShareUiState.Idle
    )
    val shareUiState: StateFlow<ShareUiState> = _shareUiState.asStateFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private var rawItemsCache: List<ShoppingItem> = emptyList()

    private val _availableCategories = MutableStateFlow<Set<String>>(emptySet())
    val availableCategories: StateFlow<Set<String>> = _availableCategories.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _nameSearchState = MutableStateFlow<NameSearchState>(NameSearchState.Idle)
    val nameSearchState: StateFlow<NameSearchState> = _nameSearchState.asStateFlow()
    private var nameSearchJob: Job? = null

    private val _aiCategoryState = MutableStateFlow<AiCategoryState>(AiCategoryState.Idle)
    val aiCategoryState: StateFlow<AiCategoryState> = _aiCategoryState.asStateFlow()
    private var aiCategoryJob: Job? = null

    private val _aiSuggestions = MutableStateFlow<List<String>>(emptyList())
    val aiSuggestions: StateFlow<List<String>> = _aiSuggestions.asStateFlow()

    private val _recipeState = MutableStateFlow<RecipeState>(RecipeState.Idle)
    val recipeState: StateFlow<RecipeState> = _recipeState.asStateFlow()

    private val _recentItems = MutableStateFlow<List<String>>(emptyList())
    val recentItems: StateFlow<List<String>> = _recentItems.asStateFlow()

    private val _favoriteNames = MutableStateFlow<Set<String>>(emptySet())
    val favoriteNames: StateFlow<Set<String>> = _favoriteNames.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        val db = ShoppingDatabase.getDatabase(application)
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "BoodschappenApp/3.3 (Android; +https://github.com/sabair24/boodschappenapp)")
                        .header("Accept", "application/json")
                        .build()
                )
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        offClient = client
        val lenientGson = GsonBuilder().setLenient().create()
        val api = Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(client).addConverterFactory(GsonConverterFactory.create(lenientGson)).build()
            .create(OpenFoodFactsApi::class.java)
        localRepo = ShoppingRepository(db.shoppingDao(), api)
        val upcClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        upcApi = Retrofit.Builder()
            .baseUrl("https://api.upcitemdb.com/")
            .client(upcClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UpcItemDbApi::class.java)
        observeSyncMode()
        loadRecentAndFavorites()
        viewModelScope.launch { reCategorizeOverigItems() }
    }

    private suspend fun reCategorizeOverigItems() {
        val items = localRepo.allItems.first()
        items.filter { it.category == Category.OVERIG.displayName }.forEach { item ->
            guessCategoryFromName(item.name)?.let { guessed ->
                localRepo.updateItem(item.copy(category = guessed.displayName))
            }
        }
    }

    private fun loadRecentAndFavorites() {
        val raw = prefs.getString("recent_items", "") ?: ""
        _recentItems.value = raw.split("|||").filter { it.isNotBlank() }.take(20)
        _favoriteNames.value = prefs.getStringSet("favorite_items", emptySet()) ?: emptySet()
    }

    fun addToRecent(name: String) {
        val trimmed = name.trim().ifBlank { return }
        val list = _recentItems.value.toMutableList().apply {
            remove(trimmed); add(0, trimmed)
        }.take(20)
        _recentItems.value = list
        prefs.edit().putString("recent_items", list.joinToString("|||")).apply()
    }

    fun toggleFavorite(name: String) {
        val trimmed = name.trim().ifBlank { return }
        val set = _favoriteNames.value.toMutableSet()
        if (!set.add(trimmed)) set.remove(trimmed)
        val limited = set.take(20).toSet()
        _favoriteNames.value = limited
        prefs.edit().putStringSet("favorite_items", limited).apply()
    }

    fun searchProductByName(query: String) {
        nameSearchJob?.cancel()
        if (query.length < 3) { _nameSearchState.value = NameSearchState.Idle; return }
        _nameSearchState.value = NameSearchState.Searching
        nameSearchJob = viewModelScope.launch {
            delay(600)
            localRepo.searchByName(query).fold(
                onSuccess = { _nameSearchState.value = NameSearchState.Found(it) },
                onFailure = { _nameSearchState.value = NameSearchState.NotFound }
            )
        }
    }

    fun resetNameSearch() { _nameSearchState.value = NameSearchState.Idle }

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
            firestoreRepo.getItemsFlow(code).catch { }.collect { items ->
                val fixed = items.map { item ->
                    if (item.category == Category.OVERIG.displayName)
                        guessCategoryFromName(item.name)?.let { item.copy(category = it.displayName) } ?: item
                    else item
                }
                _sharedItems.value = fixed
                updateDisplayedItems(fixed)
            }
        }
    }

    private fun updateDisplayedItems(rawItems: List<ShoppingItem>) {
        rawItemsCache = rawItems
        _availableCategories.value = rawItems.map { it.category }.toSet()
        reapplyFilters()
    }

    private fun reapplyFilters() {
        _uiState.update { state ->
            var items = rawItemsCache
            if (state.filterCategory != null) items = items.filter { it.category == state.filterCategory }
            if (state.searchQuery.isNotBlank()) items = items.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                it.brand?.contains(state.searchQuery, ignoreCase = true) == true
            }
            if (!state.showChecked) items = items.filter { !it.isChecked }
            items = when (state.sortMode) {
                SortMode.CATEGORY   -> items.sortedWith(compareBy({ it.category }, { it.name.lowercase() }))
                SortMode.NAME       -> items.sortedBy { it.name.lowercase() }
                SortMode.DATE_ADDED -> items.sortedByDescending { it.createdAt }
            }
            state.copy(items = items)
        }
    }

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
        if (trimmed.length != 6) { _shareUiState.value = ShareUiState.Error("Code moet 6 tekens zijn"); return }
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

    fun resetShareError() { _shareUiState.value = ShareUiState.Idle }

    fun addItem(item: ShoppingItem) {
        addToRecent(item.name)
        viewModelScope.launch {
            when (val mode = _syncMode.value) {
                is SyncMode.Local -> {
                    localRepo.addItem(item)
                    _snackbarMessage.emit("${item.name} toegevoegd")
                }
                is SyncMode.Shared -> {
                    val newItem = item.copy(id = FirestoreRepository.newItemId())
                    val updated = _sharedItems.value + newItem
                    _sharedItems.value = updated; updateDisplayedItems(updated)
                    try { firestoreRepo.publishList(mode.code, updated); _snackbarMessage.emit("${item.name} toegevoegd") }
                    catch (e: Exception) { _snackbarMessage.emit("Fout: ${e.message}") }
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
                    _sharedItems.value = updated; updateDisplayedItems(updated)
                    runCatching { firestoreRepo.publishList(mode.code, updated) }
                }
            }
        }
    }

    fun deleteItem(item: ShoppingItem, silent: Boolean = false) {
        viewModelScope.launch {
            when (val mode = _syncMode.value) {
                is SyncMode.Local -> {
                    localRepo.deleteItem(item)
                    if (!silent) _snackbarMessage.emit("${item.name} verwijderd")
                }
                is SyncMode.Shared -> {
                    val updated = _sharedItems.value.filter { it.id != item.id }
                    _sharedItems.value = updated; updateDisplayedItems(updated)
                    try { firestoreRepo.publishList(mode.code, updated)
                        if (!silent) _snackbarMessage.emit("${item.name} verwijderd") }
                    catch (e: Exception) { _snackbarMessage.emit("Fout: ${e.message}") }
                }
            }
        }
    }

    fun restoreItem(item: ShoppingItem) {
        viewModelScope.launch {
            when (val mode = _syncMode.value) {
                is SyncMode.Local -> localRepo.addItem(item.copy(id = 0))
                is SyncMode.Shared -> {
                    val updated = _sharedItems.value + item
                    _sharedItems.value = updated; updateDisplayedItems(updated)
                    runCatching { firestoreRepo.publishList(mode.code, updated) }
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
                    _sharedItems.value = updated; updateDisplayedItems(updated)
                    try { firestoreRepo.publishList(mode.code, updated); _snackbarMessage.emit("$removed items verwijderd") }
                    catch (e: Exception) { _snackbarMessage.emit("Fout: ${e.message}") }
                }
            }
        }
    }

    fun setFilterCategory(category: String?) { _uiState.update { it.copy(filterCategory = category) }; reapplyFilters() }
    fun toggleShowChecked() { _uiState.update { it.copy(showChecked = !it.showChecked) }; reapplyFilters() }
    fun setSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) }; reapplyFilters() }
    fun setSortMode(mode: SortMode) { _uiState.update { it.copy(sortMode = mode) }; reapplyFilters() }

    fun lookupBarcode(barcode: String) {
        _scanState.value = ScanState.Scanning
        viewModelScope.launch {
            // Step 1: Retrofit + Gson (lenient)
            val offResult = localRepo.lookupBarcode(barcode)
            if (offResult.isSuccess) {
                _scanState.value = ScanState.Found(offResult.getOrThrow(), barcode)
                return@launch
            }
            val retrofitError = offResult.exceptionOrNull()?.let {
                "${it.javaClass.simpleName}: ${it.message?.take(100)}"
            }
            Log.w("Scanner", "Retrofit failed for $barcode: $retrofitError")

            // Step 2: Direct OkHttp (bypasses Gson binding issues)
            val directProduct = lookupBarcodeDirectHttp(barcode)
            if (directProduct != null) {
                _scanState.value = ScanState.Found(directProduct, barcode)
                return@launch
            }

            // Step 3: UPC Item DB fallback
            val upcProduct = tryUpcItemDb(barcode)
            if (upcProduct != null) {
                _scanState.value = ScanState.Found(upcProduct, barcode)
                return@launch
            }

            _scanState.value = ScanState.NotFound(barcode, error = retrofitError)
        }
    }

    private suspend fun lookupBarcodeDirectHttp(barcode: String): ProductDto? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://world.openfoodfacts.org/api/v0/product/$barcode.json"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "BoodschappenApp/3.3 (Android; +https://github.com/sabair24/boodschappenapp)")
                    .header("Accept", "application/json")
                    .build()
                val response = offClient.newCall(request).execute()
                val code = response.code
                val body = response.body?.string()
                Log.d("Scanner", "Direct HTTP $code for $barcode, body length: ${body?.length}")
                if (!response.isSuccessful || body == null) {
                    Log.w("Scanner", "Direct HTTP failed: HTTP $code")
                    return@withContext null
                }
                val json = JsonParser.parseString(body).asJsonObject
                val status = json.get("status")?.let { if (it.isJsonPrimitive) it.asInt else 0 } ?: 0
                Log.d("Scanner", "OFF status=$status for $barcode")
                if (status != 1) return@withContext null
                val p = json.get("product")?.takeIf { !it.isJsonNull }?.asJsonObject
                    ?: return@withContext null
                fun str(key: String) = p.get(key)
                    ?.takeIf { it.isJsonPrimitive }
                    ?.asString
                    ?.takeIf { it.isNotBlank() }
                ProductDto(
                    product_name          = str("product_name"),
                    product_name_nl       = str("product_name_nl"),
                    brands                = str("brands"),
                    image_front_url       = str("image_front_url"),
                    image_url             = str("image_url"),
                    image_front_small_url = str("image_front_small_url"),
                    quantity              = str("quantity"),
                    nutriscore_grade      = str("nutriscore_grade"),
                    categories_tags       = try {
                        p.get("categories_tags")?.asJsonArray
                            ?.mapNotNull { if (it.isJsonPrimitive) it.asString else null }
                    } catch (e: Exception) { null }
                )
            } catch (e: Exception) {
                Log.e("Scanner", "Direct HTTP error: ${e.javaClass.simpleName}: ${e.message}")
                null
            }
        }
    }

    private suspend fun tryUpcItemDb(barcode: String): ProductDto? {
        return try {
            val response = upcApi.lookup(barcode)
            val item = response.items.firstOrNull() ?: return null
            if (item.title.isBlank()) return null
            ProductDto(
                product_name    = item.title,
                brands          = item.brand.ifBlank { null },
                image_front_url = item.images.firstOrNull()
            )
        } catch (e: Exception) {
            Log.w("Scanner", "UPC Item DB failed: ${e.message}")
            null
        }
    }

    fun resetScanState() { _scanState.value = ScanState.Idle }

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

    fun aiCategorize(name: String) {
        aiCategoryJob?.cancel()
        if (name.length < 3) { _aiCategoryState.value = AiCategoryState.Idle; return }
        _aiCategoryState.value = AiCategoryState.Loading
        aiCategoryJob = viewModelScope.launch {
            delay(700)
            val cat = aiRepo.suggestCategory(name)
            _aiCategoryState.value = if (cat != null) AiCategoryState.Suggested(cat) else AiCategoryState.Idle
        }
    }

    fun resetAiCategory() { _aiCategoryState.value = AiCategoryState.Idle }

    fun refreshAiSuggestions() {
        viewModelScope.launch {
            val current = _uiState.value.items.filter { !it.isChecked }.map { it.name }
            val suggestions = aiRepo.getSuggestions(current, _recentItems.value, _favoriteNames.value.toList())
            _aiSuggestions.value = suggestions
        }
    }

    fun getRecipeIngredients(dish: String) {
        if (dish.isBlank()) return
        _recipeState.value = RecipeState.Loading
        viewModelScope.launch {
            val ingredients = aiRepo.getRecipeIngredients(dish)
            _recipeState.value = if (ingredients.isEmpty())
                RecipeState.Error("Geen ingrediënten gevonden voor \"$dish\"")
            else RecipeState.Ready(ingredients)
        }
    }

    fun resetRecipeState() { _recipeState.value = RecipeState.Idle }

    fun addRecipeItems(items: List<String>) {
        items.forEach { name ->
            val guessed = guessCategoryFromName(name)
            addItem(ShoppingItem(name = name, category = (guessed ?: Category.OVERIG).displayName))
        }
        viewModelScope.launch { _snackbarMessage.emit("${items.size} ingrediënten toegevoegd") }
    }

    suspend fun getItemById(id: Long): ShoppingItem? = localRepo.getItemById(id)
}
