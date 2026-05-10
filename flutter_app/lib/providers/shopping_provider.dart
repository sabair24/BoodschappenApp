import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/shopping_item.dart';
import '../models/category.dart';
import '../data/local_database.dart';
import '../data/firestore_repository.dart';
import '../utils/category_utils.dart';

enum SortMode { category, name, date }

enum SyncMode { local, shared }

enum ShareUiState { idle, loading, active, error }

class ShoppingProvider extends ChangeNotifier {
  final LocalDatabase _db = LocalDatabase();
  final FirestoreRepository _repo = FirestoreRepository();

  List<ShoppingItem> _allItems = [];
  List<ShoppingItem> _displayedItems = [];
  Set<String> _availableCategories = {};

  String? _filterCategory;
  String _searchQuery = '';
  SortMode _sortMode = SortMode.category;
  bool _showChecked = true;
  bool _isDarkTheme = false;

  SyncMode _syncMode = SyncMode.local;
  String? _sharedCode;
  ShareUiState _shareUiState = ShareUiState.idle;
  String? _shareError;
  StreamSubscription<List<ShoppingItem>>? _firestoreSubscription;

  List<String> _recentItems = [];
  Set<String> _favoriteNames = {};

  List<ShoppingItem> get displayedItems => _displayedItems;
  Set<String> get availableCategories => _availableCategories;
  String? get filterCategory => _filterCategory;
  String get searchQuery => _searchQuery;
  SortMode get sortMode => _sortMode;
  bool get showChecked => _showChecked;
  bool get isDarkTheme => _isDarkTheme;
  SyncMode get syncMode => _syncMode;
  String? get sharedCode => _sharedCode;
  ShareUiState get shareUiState => _shareUiState;
  String? get shareError => _shareError;
  List<String> get recentItems => List.unmodifiable(_recentItems);
  Set<String> get favoriteNames => Set.unmodifiable(_favoriteNames);
  List<ShoppingItem> get allItems => List.unmodifiable(_allItems);

  int get totalItems => _allItems.length;
  int get checkedItems => _allItems.where((i) => i.isChecked).length;
  bool get allChecked => _allItems.isNotEmpty && _allItems.every((i) => i.isChecked);

  Future<void> init() async {
    await _loadPreferences();
    await _loadFromDatabase();
    _reCategorizeOverigItems();
    notifyListeners();
  }

  Future<void> _loadPreferences() async {
    final prefs = await SharedPreferences.getInstance();
    _isDarkTheme = prefs.getBool('isDarkTheme') ?? false;
    _sortMode = SortMode.values[prefs.getInt('sortMode') ?? 0];
    _showChecked = prefs.getBool('showChecked') ?? true;
    _recentItems = prefs.getStringList('recentItems') ?? [];
    _favoriteNames = (prefs.getStringList('favoriteNames') ?? []).toSet();

    final savedCode = prefs.getString('sharedCode');
    final syncModeIndex = prefs.getInt('syncMode') ?? 0;
    if (syncModeIndex == 1 && savedCode != null && savedCode.isNotEmpty) {
      _sharedCode = savedCode;
      _syncMode = SyncMode.shared;
      _shareUiState = ShareUiState.active;
      _startFirestoreListener(savedCode);
    }
  }

  Future<void> _savePreferences() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isDarkTheme', _isDarkTheme);
    await prefs.setInt('sortMode', _sortMode.index);
    await prefs.setBool('showChecked', _showChecked);
    await prefs.setStringList('recentItems', _recentItems);
    await prefs.setStringList('favoriteNames', _favoriteNames.toList());
    await prefs.setInt('syncMode', _syncMode.index);
    if (_sharedCode != null) {
      await prefs.setString('sharedCode', _sharedCode!);
    } else {
      await prefs.remove('sharedCode');
    }
  }

  Future<void> _loadFromDatabase() async {
    _allItems = await _db.getAllItems();
    _applyFilters();
  }

  void _applyFilters() {
    var items = List<ShoppingItem>.from(_allItems);

    if (!_showChecked) {
      items = items.where((i) => !i.isChecked).toList();
    }

    if (_filterCategory != null) {
      items = items.where((i) => i.category == _filterCategory).toList();
    }

    if (_searchQuery.isNotEmpty) {
      final q = _searchQuery.toLowerCase();
      items = items.where((i) {
        return i.name.toLowerCase().contains(q) ||
            (i.brand?.toLowerCase().contains(q) ?? false) ||
            (i.note.toLowerCase().contains(q));
      }).toList();
    }

    switch (_sortMode) {
      case SortMode.category:
        items.sort((a, b) {
          final catCompare = a.category.compareTo(b.category);
          if (catCompare != 0) return catCompare;
          return a.name.compareTo(b.name);
        });
        break;
      case SortMode.name:
        items.sort((a, b) => a.name.toLowerCase().compareTo(b.name.toLowerCase()));
        break;
      case SortMode.date:
        items.sort((a, b) => b.createdAt.compareTo(a.createdAt));
        break;
    }

    _displayedItems = items;
    _availableCategories = _allItems.map((i) => i.category).toSet();
  }

  void _reCategorizeOverigItems() {
    final updated = <ShoppingItem>[];
    for (final item in _allItems) {
      if (item.category == AppCategory.overig.displayName) {
        final guessed = guessCategoryFromName(item.name);
        if (guessed != AppCategory.overig) {
          updated.add(item.copyWith(category: guessed.displayName));
        }
      }
    }
    if (updated.isNotEmpty) {
      for (final item in updated) {
        final idx = _allItems.indexWhere((i) => i.id == item.id);
        if (idx >= 0) _allItems[idx] = item;
        _db.updateItem(item);
      }
      _applyFilters();
    }
  }

  Future<void> addItem(ShoppingItem item) async {
    await _db.insertItem(item);
    _allItems.insert(0, item);
    _addToRecent(item.name);
    _applyFilters();
    notifyListeners();
    if (_syncMode == SyncMode.shared && _sharedCode != null) {
      _publishToFirestore();
    }
  }

  Future<void> updateItem(ShoppingItem item) async {
    await _db.updateItem(item);
    final idx = _allItems.indexWhere((i) => i.id == item.id);
    if (idx >= 0) {
      _allItems[idx] = item;
    }
    _applyFilters();
    notifyListeners();
    if (_syncMode == SyncMode.shared && _sharedCode != null) {
      _publishToFirestore();
    }
  }

  Future<void> deleteItem(String id) async {
    await _db.deleteItem(id);
    _allItems.removeWhere((i) => i.id == id);
    _applyFilters();
    notifyListeners();
    if (_syncMode == SyncMode.shared && _sharedCode != null) {
      _publishToFirestore();
    }
  }

  Future<void> toggleChecked(String id) async {
    final idx = _allItems.indexWhere((i) => i.id == id);
    if (idx < 0) return;
    final updated = _allItems[idx].copyWith(isChecked: !_allItems[idx].isChecked);
    _allItems[idx] = updated;
    await _db.updateItem(updated);
    _applyFilters();
    notifyListeners();
    if (_syncMode == SyncMode.shared && _sharedCode != null) {
      _publishToFirestore();
    }
  }

  Future<void> deleteAllItems() async {
    await _db.deleteAllItems();
    _allItems.clear();
    _applyFilters();
    notifyListeners();
    if (_syncMode == SyncMode.shared && _sharedCode != null) {
      _publishToFirestore();
    }
  }

  Future<void> deleteCheckedItems() async {
    await _db.deleteCheckedItems();
    _allItems.removeWhere((i) => i.isChecked);
    _applyFilters();
    notifyListeners();
    if (_syncMode == SyncMode.shared && _sharedCode != null) {
      _publishToFirestore();
    }
  }

  void setFilterCategory(String? category) {
    _filterCategory = category;
    _applyFilters();
    notifyListeners();
  }

  void setSearchQuery(String query) {
    _searchQuery = query;
    _applyFilters();
    notifyListeners();
  }

  void setSortMode(SortMode mode) {
    _sortMode = mode;
    _applyFilters();
    _savePreferences();
    notifyListeners();
  }

  void setShowChecked(bool show) {
    _showChecked = show;
    _applyFilters();
    _savePreferences();
    notifyListeners();
  }

  void toggleDarkTheme() {
    _isDarkTheme = !_isDarkTheme;
    _savePreferences();
    notifyListeners();
  }

  void toggleFavorite(String name) {
    if (_favoriteNames.contains(name)) {
      _favoriteNames.remove(name);
    } else {
      _favoriteNames.add(name);
    }
    _savePreferences();
    notifyListeners();
  }

  bool isFavorite(String name) => _favoriteNames.contains(name);

  void _addToRecent(String name) {
    _recentItems.remove(name);
    _recentItems.insert(0, name);
    if (_recentItems.length > 20) {
      _recentItems = _recentItems.sublist(0, 20);
    }
    _savePreferences();
  }

  void addToRecent(String name) {
    _addToRecent(name);
    notifyListeners();
  }

  Future<void> createSharedList() async {
    _shareUiState = ShareUiState.loading;
    _shareError = null;
    notifyListeners();

    try {
      final code = FirestoreRepository.generateCode();
      await _repo.createList(code, _allItems);
      _sharedCode = code;
      _syncMode = SyncMode.shared;
      _shareUiState = ShareUiState.active;
      _startFirestoreListener(code);
      await _savePreferences();
      notifyListeners();
    } catch (e) {
      _shareUiState = ShareUiState.error;
      _shareError = e.toString();
      notifyListeners();
    }
  }

  Future<void> joinSharedList(String code) async {
    _shareUiState = ShareUiState.loading;
    _shareError = null;
    notifyListeners();

    try {
      final exists = await _repo.listExists(code);
      if (!exists) {
        _shareUiState = ShareUiState.error;
        _shareError = 'Lijst met code $code niet gevonden.';
        notifyListeners();
        return;
      }
      _sharedCode = code;
      _syncMode = SyncMode.shared;
      _shareUiState = ShareUiState.active;
      _startFirestoreListener(code);
      await _savePreferences();
      notifyListeners();
    } catch (e) {
      _shareUiState = ShareUiState.error;
      _shareError = e.toString();
      notifyListeners();
    }
  }

  Future<void> stopSharing() async {
    await _firestoreSubscription?.cancel();
    _firestoreSubscription = null;
    _sharedCode = null;
    _syncMode = SyncMode.local;
    _shareUiState = ShareUiState.idle;
    _shareError = null;
    await _savePreferences();
    await _loadFromDatabase();
    notifyListeners();
  }

  void _startFirestoreListener(String code) {
    _firestoreSubscription?.cancel();
    _firestoreSubscription = _repo.getItemsStream(code).listen(
      (items) async {
        final recategorized = items.map((item) {
          if (item.category == AppCategory.overig.displayName) {
            final guessed = guessCategoryFromName(item.name);
            if (guessed != AppCategory.overig) {
              return item.copyWith(category: guessed.displayName);
            }
          }
          return item;
        }).toList();

        _allItems = recategorized;
        await _db.replaceAllItems(recategorized);
        _applyFilters();
        notifyListeners();
      },
      onError: (e) {
        _shareError = e.toString();
        notifyListeners();
      },
    );
  }

  Future<void> _publishToFirestore() async {
    if (_sharedCode == null) return;
    try {
      await _repo.publishList(_sharedCode!, _allItems);
    } catch (_) {}
  }

  Future<void> incrementQuantity(ShoppingItem item) async {
    final current = double.tryParse(item.quantity) ?? 1;
    final newQty = (current + 1).toInt().toString();
    await updateItem(item.copyWith(quantity: newQty));
  }

  Future<void> decrementQuantity(ShoppingItem item) async {
    final current = double.tryParse(item.quantity) ?? 1;
    if (current <= 1) return;
    final newQty = (current - 1).toInt().toString();
    await updateItem(item.copyWith(quantity: newQty));
  }

  @override
  void dispose() {
    _firestoreSubscription?.cancel();
    super.dispose();
  }
}
