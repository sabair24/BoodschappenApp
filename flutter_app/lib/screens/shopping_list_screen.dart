// ignore_for_file: use_build_context_synchronously
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../providers/shopping_provider.dart';
import '../models/shopping_item.dart';
import '../models/category.dart';
import '../theme/app_theme.dart';
import '../widgets/item_card.dart';
import '../widgets/category_chips.dart';
import 'add_edit_item_screen.dart';
import 'scanner_screen.dart';

class ShoppingListScreen extends StatefulWidget {
  const ShoppingListScreen({super.key});

  @override
  State<ShoppingListScreen> createState() => _ShoppingListScreenState();
}

class _ShoppingListScreenState extends State<ShoppingListScreen>
    with SingleTickerProviderStateMixin {
  final _searchController = TextEditingController();
  ShoppingItem? _selectedItem;
  bool _showSearch = false;

  bool _celebrationShown = false;
  late final AnimationController _celebCtrl;
  late final Animation<double> _celebFade;

  @override
  void initState() {
    super.initState();
    _celebCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 600),
    );
    _celebFade =
        CurvedAnimation(parent: _celebCtrl, curve: Curves.easeInOut);
  }

  @override
  void dispose() {
    _searchController.dispose();
    _celebCtrl.dispose();
    super.dispose();
  }

  void _maybeShowCelebration(ShoppingProvider provider) {
    if (provider.allChecked && !_celebrationShown && provider.totalItems > 0) {
      _celebrationShown = true;
      _celebCtrl.forward(from: 0);
      Future.delayed(const Duration(seconds: 3), () {
        if (mounted) _celebCtrl.reverse();
      });
    } else if (!provider.allChecked) {
      _celebrationShown = false;
    }
  }

  void _openAddItem(BuildContext context) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const AddEditItemScreen()),
    );
  }

  void _openEditItem(BuildContext context, ShoppingItem item) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => AddEditItemScreen(item: item)),
    );
  }

  Future<void> _scanBarcode(BuildContext context) async {
    await Navigator.push<String>(
      context,
      MaterialPageRoute(builder: (_) => const ScannerScreen()),
    );
    if (context.mounted) _openAddItem(context);
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<ShoppingProvider>(
      builder: (context, provider, _) {
        _maybeShowCelebration(provider);
        return LayoutBuilder(
          builder: (context, constraints) {
            final isTablet = constraints.maxWidth >= 768;
            if (isTablet) {
              return _buildTabletLayout(context);
            }
            return _buildPhoneLayout(context);
          },
        );
      },
    );
  }

  Widget _buildCelebrationOverlay() {
    return FadeTransition(
      opacity: _celebFade,
      child: IgnorePointer(
        child: Container(
          color: Colors.black.withOpacity(0.65),
          child: const Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text('🎉', style: TextStyle(fontSize: 80)),
                SizedBox(height: 16),
                Text(
                  'Lijst voltooid!',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 32,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                SizedBox(height: 8),
                Text(
                  'Geweldig, je hebt alles afgevinkt!',
                  style: TextStyle(color: Colors.white70, fontSize: 16),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildTabletLayout(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          Row(
            children: [
              SizedBox(
                width: 400,
                child: _buildListPane(context, isTablet: true),
              ),
              const VerticalDivider(width: 1),
              Expanded(
                child: _selectedItem == null
                    ? _buildEmptyDetailPane(context)
                    : _buildDetailPane(context, _selectedItem!),
              ),
            ],
          ),
          _buildCelebrationOverlay(),
        ],
      ),
      floatingActionButton: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          FloatingActionButton.small(
            heroTag: 'scan_fab_tablet',
            onPressed: () => _scanBarcode(context),
            tooltip: 'Barcode scannen',
            child: const Icon(Icons.qr_code_scanner),
          ),
          const SizedBox(height: 10),
          FloatingActionButton.extended(
            heroTag: 'add_fab_tablet',
            onPressed: () => _openAddItemTablet(context),
            icon: const Icon(Icons.add),
            label: const Text('Toevoegen'),
          ),
        ],
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.endFloat,
    );
  }

  Widget _buildEmptyDetailPane(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.shopping_cart_outlined,
            size: 80,
            color: Theme.of(context).colorScheme.primary.withOpacity(0.3),
          ),
          const SizedBox(height: 16),
          Text(
            'Selecteer een product',
            style: TextStyle(
              fontSize: 18,
              color: Theme.of(context).colorScheme.onSurface.withOpacity(0.4),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDetailPane(BuildContext context, ShoppingItem item) {
    final provider = context.watch<ShoppingProvider>();
    final isDark = provider.isDarkTheme;
    final catColor = AppTheme.categoryColor(item.category, isDark);
    final cat = AppCategory.fromName(item.category);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => setState(() => _selectedItem = null),
        ),
        title: Text(item.name),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit),
            onPressed: () async {
              await _openEditItemTablet(context, item);
            },
          ),
          IconButton(
            icon: const Icon(Icons.delete_outline),
            onPressed: () async {
              await provider.deleteItem(item.id);
              setState(() => _selectedItem = null);
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              decoration: BoxDecoration(
                color: catColor.withOpacity(0.15),
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: catColor.withOpacity(0.4)),
              ),
              child: Text(
                '${cat.emoji} ${cat.displayName}',
                style: TextStyle(
                  color: catColor,
                  fontWeight: FontWeight.bold,
                  fontSize: 15,
                ),
              ),
            ),
            const SizedBox(height: 24),
            _DetailRow(label: 'Aantal', value: '${item.quantity} ${item.unit}'.trim()),
            if (item.brand != null && item.brand!.isNotEmpty)
              _DetailRow(label: 'Merk', value: item.brand!),
            if (item.note.isNotEmpty)
              _DetailRow(label: 'Notitie', value: item.note),
            if (item.barcode != null && item.barcode!.isNotEmpty)
              _DetailRow(label: 'Barcode', value: item.barcode!),
            const SizedBox(height: 32),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    icon: Icon(
                      item.isChecked
                          ? Icons.radio_button_unchecked
                          : Icons.check_circle_outline,
                    ),
                    label: Text(item.isChecked ? 'Markeer als niet gekocht' : 'Markeer als gekocht'),
                    onPressed: () {
                      provider.toggleChecked(item.id);
                      final updated = provider.allItems.firstWhere(
                        (i) => i.id == item.id,
                        orElse: () => item,
                      );
                      setState(() => _selectedItem = updated);
                    },
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _openAddItemTablet(BuildContext context) async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const AddEditItemScreen()),
    );
  }

  Future<void> _openEditItemTablet(BuildContext context, ShoppingItem item) async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => AddEditItemScreen(item: item)),
    );
    final provider = context.read<ShoppingProvider>();
    final updated = provider.allItems.where((i) => i.id == item.id).toList();
    if (updated.isNotEmpty && mounted) {
      setState(() => _selectedItem = updated.first);
    }
  }

  Widget _buildPhoneLayout(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          _buildListPane(context, isTablet: false),
          _buildCelebrationOverlay(),
        ],
      ),
      floatingActionButton: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          FloatingActionButton.small(
            heroTag: 'scan_fab',
            onPressed: () => _scanBarcode(context),
            tooltip: 'Barcode scannen',
            child: const Icon(Icons.qr_code_scanner),
          ),
          const SizedBox(height: 10),
          FloatingActionButton(
            heroTag: 'add_fab',
            onPressed: () => _openAddItem(context),
            tooltip: 'Artikel toevoegen',
            child: const Icon(Icons.add),
          ),
        ],
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.endFloat,
    );
  }

  Widget _buildListPane(BuildContext context, {required bool isTablet}) {
    return Consumer<ShoppingProvider>(
      builder: (context, provider, _) {
        final isDark = provider.isDarkTheme;

        return Scaffold(
          appBar: AppBar(
            title: _showSearch
                ? TextField(
                    controller: _searchController,
                    autofocus: true,
                    decoration: InputDecoration(
                      hintText: 'Zoeken...',
                      border: InputBorder.none,
                      filled: false,
                      hintStyle: TextStyle(color: Colors.white.withOpacity(0.7)),
                    ),
                    style: const TextStyle(color: Colors.white),
                    onChanged: provider.setSearchQuery,
                  )
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Boodschappen'),
                      if (provider.totalItems > 0)
                        Text(
                          '${provider.checkedItems}/${provider.totalItems} gekocht',
                          style: const TextStyle(
                            fontSize: 12,
                            color: Colors.white70,
                            fontWeight: FontWeight.normal,
                          ),
                        ),
                    ],
                  ),
            actions: [
              IconButton(
                icon: Icon(_showSearch ? Icons.close : Icons.search),
                onPressed: () {
                  setState(() {
                    _showSearch = !_showSearch;
                    if (!_showSearch) {
                      _searchController.clear();
                      provider.setSearchQuery('');
                    }
                  });
                },
              ),
              _buildOverflowMenu(context, provider, isDark),
            ],
            bottom: provider.totalItems > 0
                ? PreferredSize(
                    preferredSize: const Size.fromHeight(8),
                    child: LinearProgressIndicator(
                      value: provider.totalItems == 0
                          ? 0
                          : provider.checkedItems / provider.totalItems,
                    ),
                  )
                : null,
          ),
          body: Column(
            children: [
              if (provider.syncMode == SyncMode.shared &&
                  provider.sharedCode != null)
                _SharedBanner(code: provider.sharedCode!),
              const CategoryChips(),
              Expanded(
                child: provider.displayedItems.isEmpty
                    ? _buildEmptyState(context, provider)
                    : _buildItemList(context, provider, isTablet),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildOverflowMenu(
    BuildContext context,
    ShoppingProvider provider,
    bool isDark,
  ) {
    return PopupMenuButton<String>(
      icon: const Icon(Icons.more_vert),
      onSelected: (value) => _handleMenuAction(context, provider, value),
      itemBuilder: (_) => [
        PopupMenuItem(
          value: 'sort_category',
          child: Row(children: [
            Icon(
              Icons.category,
              size: 20,
              color: provider.sortMode == SortMode.category
                  ? Theme.of(context).colorScheme.primary
                  : null,
            ),
            const SizedBox(width: 12),
            const Text('Sorteren op categorie'),
          ]),
        ),
        PopupMenuItem(
          value: 'sort_name',
          child: Row(children: [
            Icon(
              Icons.sort_by_alpha,
              size: 20,
              color: provider.sortMode == SortMode.name
                  ? Theme.of(context).colorScheme.primary
                  : null,
            ),
            const SizedBox(width: 12),
            const Text('Sorteren op naam'),
          ]),
        ),
        PopupMenuItem(
          value: 'sort_date',
          child: Row(children: [
            Icon(
              Icons.access_time,
              size: 20,
              color: provider.sortMode == SortMode.date
                  ? Theme.of(context).colorScheme.primary
                  : null,
            ),
            const SizedBox(width: 12),
            const Text('Sorteren op datum'),
          ]),
        ),
        const PopupMenuDivider(),
        PopupMenuItem(
          value: 'toggle_checked',
          child: Row(children: [
            Icon(
              provider.showChecked
                  ? Icons.visibility_off
                  : Icons.visibility,
              size: 20,
            ),
            const SizedBox(width: 12),
            Text(provider.showChecked
                ? 'Verberg gekochte items'
                : 'Toon gekochte items'),
          ]),
        ),
        const PopupMenuDivider(),
        PopupMenuItem(
          value: 'share',
          child: Row(children: [
            Icon(
              provider.syncMode == SyncMode.shared
                  ? Icons.cloud_done
                  : Icons.share,
              size: 20,
            ),
            const SizedBox(width: 12),
            Text(provider.syncMode == SyncMode.shared
                ? 'Gedeelde lijst beheren'
                : 'Lijst delen'),
          ]),
        ),
        const PopupMenuDivider(),
        PopupMenuItem(
          value: 'theme',
          child: Row(children: [
            Icon(isDark ? Icons.light_mode : Icons.dark_mode, size: 20),
            const SizedBox(width: 12),
            Text(isDark ? 'Licht thema' : 'Donker thema'),
          ]),
        ),
        const PopupMenuDivider(),
        const PopupMenuItem(
          value: 'delete_checked',
          child: Row(children: [
            Icon(Icons.playlist_remove, size: 20),
            SizedBox(width: 12),
            Text('Verwijder afgevinkte items'),
          ]),
        ),
        const PopupMenuItem(
          value: 'delete_all',
          child: Row(children: [
            Icon(Icons.delete_sweep, size: 20, color: Colors.red),
            SizedBox(width: 12),
            Text('Verwijder alles', style: TextStyle(color: Colors.red)),
          ]),
        ),
      ],
    );
  }

  void _handleMenuAction(
    BuildContext context,
    ShoppingProvider provider,
    String action,
  ) {
    switch (action) {
      case 'sort_category':
        provider.setSortMode(SortMode.category);
        break;
      case 'sort_name':
        provider.setSortMode(SortMode.name);
        break;
      case 'sort_date':
        provider.setSortMode(SortMode.date);
        break;
      case 'toggle_checked':
        provider.setShowChecked(!provider.showChecked);
        break;
      case 'theme':
        provider.toggleDarkTheme();
        break;
      case 'share':
        _showShareDialog(context, provider);
        break;
      case 'delete_checked':
        _confirmDeleteChecked(context, provider);
        break;
      case 'delete_all':
        _confirmDeleteAll(context, provider);
        break;
    }
  }

  Widget _buildItemList(
    BuildContext context,
    ShoppingProvider provider,
    bool isTablet,
  ) {
    if (provider.sortMode != SortMode.category) {
      return ListView.builder(
        padding: const EdgeInsets.only(bottom: 80),
        itemCount: provider.displayedItems.length,
        itemBuilder: (_, i) {
          final item = provider.displayedItems[i];
          return ItemCard(
            item: item,
            onTap: () {
              if (isTablet) {
                setState(() => _selectedItem = item);
              } else {
                _openEditItem(context, item);
              }
            },
            onDelete: () => provider.deleteItem(item.id),
          );
        },
      );
    }

    final grouped = <String, List<ShoppingItem>>{};
    for (final item in provider.displayedItems) {
      grouped.putIfAbsent(item.category, () => []).add(item);
    }

    final orderedKeys = AppCategory.values
        .map((c) => c.displayName)
        .where(grouped.containsKey)
        .toList();

    final slivers = <Widget>[const SliverToBoxAdapter(child: SizedBox(height: 4))];

    for (final cat in orderedKeys) {
      final items = grouped[cat]!;
      final appCat = AppCategory.fromName(cat);
      slivers.add(
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
            child: Text(
              '${appCat.emoji} $cat',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.bold,
                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.55),
                letterSpacing: 0.8,
              ),
            ),
          ),
        ),
      );
      slivers.add(
        SliverList(
          delegate: SliverChildBuilderDelegate(
            (_, i) => ItemCard(
              item: items[i],
              onTap: () {
                if (isTablet) {
                  setState(() => _selectedItem = items[i]);
                } else {
                  _openEditItem(context, items[i]);
                }
              },
              onDelete: () => provider.deleteItem(items[i].id),
            ),
            childCount: items.length,
          ),
        ),
      );
    }

    slivers.add(const SliverToBoxAdapter(child: SizedBox(height: 80)));

    return CustomScrollView(slivers: slivers);
  }

  Widget _buildEmptyState(BuildContext context, ShoppingProvider provider) {
    final hasFilter =
        provider.filterCategory != null || provider.searchQuery.isNotEmpty;
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            hasFilter
                ? Icons.filter_list_off
                : Icons.shopping_cart_outlined,
            size: 72,
            color: Theme.of(context).colorScheme.primary.withOpacity(0.25),
          ),
          const SizedBox(height: 16),
          Text(
            hasFilter
                ? 'Geen resultaten'
                : 'Je lijst is leeg',
            style: TextStyle(
              fontSize: 18,
              color:
                  Theme.of(context).colorScheme.onSurface.withOpacity(0.4),
            ),
          ),
          if (!hasFilter) ...[
            const SizedBox(height: 8),
            Text(
              'Tik + om iets toe te voegen',
              style: TextStyle(
                fontSize: 14,
                color: Theme.of(context)
                    .colorScheme
                    .onSurface
                    .withOpacity(0.3),
              ),
            ),
          ],
        ],
      ),
    );
  }

  void _showShareDialog(BuildContext context, ShoppingProvider provider) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => _ShareBottomSheet(provider: provider),
    );
  }

  void _confirmDeleteChecked(
    BuildContext context,
    ShoppingProvider provider,
  ) {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Afgevinkte items verwijderen?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Annuleren'),
          ),
          ElevatedButton(
            onPressed: () {
              provider.deleteCheckedItems();
              Navigator.pop(context);
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Verwijder'),
          ),
        ],
      ),
    );
  }

  void _confirmDeleteAll(BuildContext context, ShoppingProvider provider) {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Hele lijst verwijderen?'),
        content: const Text('Dit kan niet ongedaan worden gemaakt.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Annuleren'),
          ),
          ElevatedButton(
            onPressed: () {
              provider.deleteAllItems();
              Navigator.pop(context);
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Verwijder alles'),
          ),
        ],
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  final String label;
  final String value;

  const _DetailRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(
              label,
              style: TextStyle(
                fontSize: 14,
                color:
                    Theme.of(context).colorScheme.onSurface.withOpacity(0.55),
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w500),
            ),
          ),
        ],
      ),
    );
  }
}

class _SharedBanner extends StatelessWidget {
  final String code;

  const _SharedBanner({required this.code});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Theme.of(context).colorScheme.primaryContainer,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        children: [
          Icon(
            Icons.cloud_sync,
            size: 18,
            color: Theme.of(context).colorScheme.onPrimaryContainer,
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              'Gedeelde lijst · Code: $code',
              style: TextStyle(
                fontSize: 13,
                color: Theme.of(context).colorScheme.onPrimaryContainer,
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.copy, size: 18),
            onPressed: () {
              Clipboard.setData(ClipboardData(text: code));
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Code gekopieerd!')),
              );
            },
          ),
        ],
      ),
    );
  }
}

class _ShareBottomSheet extends StatefulWidget {
  final ShoppingProvider provider;

  const _ShareBottomSheet({required this.provider});

  @override
  State<_ShareBottomSheet> createState() => _ShareBottomSheetState();
}

class _ShareBottomSheetState extends State<_ShareBottomSheet> {
  final _codeController = TextEditingController();

  @override
  void dispose() {
    _codeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final provider = widget.provider;
    final isShared = provider.syncMode == SyncMode.shared;

    return Padding(
      padding: EdgeInsets.fromLTRB(
        24,
        24,
        24,
        MediaQuery.of(context).viewInsets.bottom + 24,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Lijst delen',
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
          ),
          const SizedBox(height: 24),
          if (isShared) ...[
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primaryContainer,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                children: [
                  Row(
                    children: [
                      Icon(
                        Icons.cloud_done,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Lijst wordt gesynchroniseerd',
                              style: TextStyle(fontWeight: FontWeight.bold),
                            ),
                            Text(
                              'Code: ${provider.sharedCode}',
                              style: const TextStyle(fontSize: 13),
                            ),
                          ],
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.copy),
                        onPressed: () {
                          Clipboard.setData(
                              ClipboardData(text: provider.sharedCode!));
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Code gekopieerd!')),
                          );
                        },
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    icon: const Icon(Icons.cloud_off),
                    label: const Text('Stoppen met delen'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.red,
                      side: const BorderSide(color: Colors.red),
                    ),
                    onPressed: () async {
                      await provider.stopSharing();
                      if (context.mounted) Navigator.pop(context);
                    },
                  ),
                ],
              ),
            ),
          ] else ...[
            ElevatedButton.icon(
              icon: provider.shareUiState == ShareUiState.loading
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.add_link),
              label: const Text('Nieuwe gedeelde lijst starten'),
              onPressed: provider.shareUiState == ShareUiState.loading
                  ? null
                  : () async {
                      await provider.createSharedList();
                      if (context.mounted) Navigator.pop(context);
                    },
            ),
            const SizedBox(height: 16),
            const Divider(),
            const SizedBox(height: 16),
            Text(
              'Bestaande lijst joinen',
              style: Theme.of(context).textTheme.titleSmall,
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _codeController,
                    decoration: const InputDecoration(
                      hintText: 'Voer code in...',
                      prefixIcon: Icon(Icons.vpn_key),
                    ),
                    textCapitalization: TextCapitalization.characters,
                  ),
                ),
                const SizedBox(width: 12),
                ElevatedButton(
                  onPressed: () async {
                    final code = _codeController.text.trim().toUpperCase();
                    if (code.isEmpty) return;
                    await provider.joinSharedList(code);
                    if (context.mounted) Navigator.pop(context);
                  },
                  child: const Text('Joinen'),
                ),
              ],
            ),
            if (provider.shareUiState == ShareUiState.error &&
                provider.shareError != null) ...[
              const SizedBox(height: 8),
              Text(
                provider.shareError!,
                style: const TextStyle(color: Colors.red, fontSize: 13),
              ),
            ],
          ],
        ],
      ),
    );
  }
}
