import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:http/http.dart' as http;
import 'package:cached_network_image/cached_network_image.dart';
import '../models/shopping_item.dart';
import '../models/category.dart';
import '../providers/shopping_provider.dart';
import '../theme/app_theme.dart';
import '../utils/category_utils.dart';
import 'scanner_screen.dart';

class AddEditItemScreen extends StatefulWidget {
  final ShoppingItem? item;

  const AddEditItemScreen({super.key, this.item});

  @override
  State<AddEditItemScreen> createState() => _AddEditItemScreenState();
}

class _AddEditItemScreenState extends State<AddEditItemScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameCtrl;
  late final TextEditingController _quantityCtrl;
  late final TextEditingController _noteCtrl;
  late final TextEditingController _brandCtrl;

  String _selectedUnit = '';
  String _selectedCategory = AppCategory.overig.displayName;
  String? _imageUrl;
  String? _barcode;
  bool _isLookingUp = false;
  bool _isFavorite = false;

  static const List<String> _units = [
    '',
    'stuk',
    'stuks',
    'kg',
    'g',
    'l',
    'ml',
    'pak',
    'blik',
    'fles',
    'doos',
    'zak',
    'pot',
    'tube',
    'rol',
  ];

  @override
  void initState() {
    super.initState();
    final item = widget.item;
    _nameCtrl = TextEditingController(text: item?.name ?? '');
    _quantityCtrl = TextEditingController(text: item?.quantity ?? '1');
    _noteCtrl = TextEditingController(text: item?.note ?? '');
    _brandCtrl = TextEditingController(text: item?.brand ?? '');
    _selectedUnit = item?.unit ?? '';
    _selectedCategory =
        item?.category ?? AppCategory.overig.displayName;
    _imageUrl = item?.imageUrl;
    _barcode = item?.barcode;

    if (item != null) {
      final provider =
          Provider.of<ShoppingProvider>(context, listen: false);
      _isFavorite = provider.isFavorite(item.name);
    }

    _nameCtrl.addListener(_onNameChanged);
  }

  @override
  void dispose() {
    _nameCtrl.removeListener(_onNameChanged);
    _nameCtrl.dispose();
    _quantityCtrl.dispose();
    _noteCtrl.dispose();
    _brandCtrl.dispose();
    super.dispose();
  }

  void _onNameChanged() {
    final guessed = guessCategoryFromName(_nameCtrl.text);
    if (guessed != AppCategory.overig) {
      setState(() => _selectedCategory = guessed.displayName);
    }
  }

  Future<void> _scanBarcode() async {
    final result = await Navigator.of(context).push<String>(
      MaterialPageRoute(builder: (_) => const ScannerScreen()),
    );
    if (result != null && result.isNotEmpty) {
      setState(() {
        _barcode = result;
        _isLookingUp = true;
      });
      await _lookupBarcode(result);
    }
  }

  Future<void> _lookupBarcode(String barcode) async {
    try {
      final url = Uri.parse(
          'https://world.openfoodfacts.org/api/v0/product/$barcode.json');
      final response = await http.get(url).timeout(const Duration(seconds: 8));
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        if (data['status'] == 1) {
          final product = data['product'] as Map<String, dynamic>;
          final name = (product['product_name'] as String?) ??
              (product['product_name_nl'] as String?) ??
              '';
          final brand = (product['brands'] as String?) ?? '';
          final imgUrl = (product['image_front_url'] as String?) ??
              (product['image_url'] as String?) ??
              '';

          setState(() {
            if (name.isNotEmpty && _nameCtrl.text.isEmpty) {
              _nameCtrl.text = name;
            }
            if (brand.isNotEmpty) _brandCtrl.text = brand;
            if (imgUrl.isNotEmpty) _imageUrl = imgUrl;
            _isLookingUp = false;
          });
        } else {
          setState(() => _isLookingUp = false);
        }
      } else {
        setState(() => _isLookingUp = false);
      }
    } catch (_) {
      setState(() => _isLookingUp = false);
    }
  }

  void _save() {
    if (!_formKey.currentState!.validate()) return;
    final provider = Provider.of<ShoppingProvider>(context, listen: false);

    final name = _nameCtrl.text.trim();

    if (widget.item == null) {
      final newItem = ShoppingItem.create(
        name: name,
        quantity: _quantityCtrl.text.trim().isEmpty
            ? '1'
            : _quantityCtrl.text.trim(),
        unit: _selectedUnit,
        category: _selectedCategory,
        note: _noteCtrl.text.trim(),
        imageUrl: _imageUrl,
        brand: _brandCtrl.text.trim().isEmpty ? null : _brandCtrl.text.trim(),
        barcode: _barcode,
      );
      provider.addItem(newItem);
      if (_isFavorite && !provider.isFavorite(name)) {
        provider.toggleFavorite(name);
      }
    } else {
      final updated = widget.item!.copyWith(
        name: name,
        quantity: _quantityCtrl.text.trim().isEmpty
            ? '1'
            : _quantityCtrl.text.trim(),
        unit: _selectedUnit,
        category: _selectedCategory,
        note: _noteCtrl.text.trim(),
        imageUrl: _imageUrl,
        brand: _brandCtrl.text.trim().isEmpty ? null : _brandCtrl.text.trim(),
        barcode: _barcode,
      );
      provider.updateItem(updated);
      final wasFav = provider.isFavorite(widget.item!.name);
      if (_isFavorite != wasFav) provider.toggleFavorite(name);
    }

    provider.addToRecent(name);
    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<ShoppingProvider>(
      builder: (context, provider, _) {
        final isDark = provider.isDarkTheme;
        final isEditing = widget.item != null;

        return Scaffold(
          appBar: AppBar(
            title: Text(isEditing ? 'Artikel bewerken' : 'Artikel toevoegen'),
            actions: [
              IconButton(
                icon: Icon(
                  _isFavorite ? Icons.star : Icons.star_border,
                  color: _isFavorite ? Colors.amber : null,
                ),
                onPressed: () => setState(() => _isFavorite = !_isFavorite),
                tooltip: 'Favoriet',
              ),
              TextButton(
                onPressed: _save,
                child: Text(
                  isEditing ? 'Opslaan' : 'Toevoegen',
                  style: const TextStyle(
                      color: Colors.white, fontWeight: FontWeight.bold),
                ),
              ),
            ],
          ),
          body: Form(
            key: _formKey,
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                if (_imageUrl != null && _imageUrl!.isNotEmpty)
                  Center(
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(12),
                      child: CachedNetworkImage(
                        imageUrl: _imageUrl!,
                        height: 120,
                        width: 120,
                        fit: BoxFit.cover,
                        errorWidget: (_, __, ___) => const SizedBox.shrink(),
                      ),
                    ),
                  ),
                if (_imageUrl != null && _imageUrl!.isNotEmpty)
                  const SizedBox(height: 12),

                TextFormField(
                  controller: _nameCtrl,
                  autofocus: widget.item == null,
                  decoration: const InputDecoration(
                    labelText: 'Naam *',
                    prefixIcon: Icon(Icons.shopping_basket_outlined),
                  ),
                  textCapitalization: TextCapitalization.sentences,
                  validator: (v) => (v == null || v.trim().isEmpty)
                      ? 'Vul een naam in'
                      : null,
                ),
                const SizedBox(height: 12),

                _SuggestionsRow(
                  provider: provider,
                  onSuggestionTap: (name) {
                    _nameCtrl.text = name;
                    final guessed = guessCategoryFromName(name);
                    setState(() {
                      if (guessed != AppCategory.overig) {
                        _selectedCategory = guessed.displayName;
                      }
                    });
                  },
                ),

                Row(
                  children: [
                    Expanded(
                      flex: 2,
                      child: TextFormField(
                        controller: _quantityCtrl,
                        keyboardType: const TextInputType.numberWithOptions(
                            decimal: true),
                        decoration: const InputDecoration(
                          labelText: 'Aantal',
                          prefixIcon: Icon(Icons.numbers),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      flex: 3,
                      child: DropdownButtonFormField<String>(
                        value: _selectedUnit,
                        decoration: const InputDecoration(
                          labelText: 'Eenheid',
                        ),
                        items: _units.map((u) {
                          return DropdownMenuItem(
                            value: u,
                            child: Text(u.isEmpty ? '— geen —' : u),
                          );
                        }).toList(),
                        onChanged: (v) =>
                            setState(() => _selectedUnit = v ?? ''),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),

                DropdownButtonFormField<String>(
                  value: _selectedCategory,
                  decoration: const InputDecoration(
                    labelText: 'Categorie',
                    prefixIcon: Icon(Icons.category_outlined),
                  ),
                  items: AppCategory.values.map((cat) {
                    final color = AppTheme.categoryColor(cat.displayName, isDark);
                    return DropdownMenuItem(
                      value: cat.displayName,
                      child: Row(
                        children: [
                          Text(cat.emoji, style: const TextStyle(fontSize: 20)),
                          const SizedBox(width: 8),
                          Text(cat.displayName),
                          const SizedBox(width: 8),
                          Container(
                            width: 10,
                            height: 10,
                            decoration: BoxDecoration(
                              color: color,
                              shape: BoxShape.circle,
                            ),
                          ),
                        ],
                      ),
                    );
                  }).toList(),
                  onChanged: (v) =>
                      setState(() => _selectedCategory = v ?? _selectedCategory),
                ),
                const SizedBox(height: 12),

                TextFormField(
                  controller: _brandCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Merk (optioneel)',
                    prefixIcon: Icon(Icons.business_outlined),
                  ),
                ),
                const SizedBox(height: 12),

                TextFormField(
                  controller: _noteCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Notitie (optioneel)',
                    prefixIcon: Icon(Icons.notes_outlined),
                  ),
                  maxLines: 2,
                ),
                const SizedBox(height: 16),

                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        icon: _isLookingUp
                            ? const SizedBox(
                                width: 18,
                                height: 18,
                                child: CircularProgressIndicator(strokeWidth: 2),
                              )
                            : const Icon(Icons.qr_code_scanner),
                        label: Text(
                          _barcode != null
                              ? 'Barcode: $_barcode'
                              : 'Scan barcode',
                        ),
                        onPressed: _isLookingUp ? null : _scanBarcode,
                        style: OutlinedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                      ),
                    ),
                    if (_barcode != null)
                      IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () => setState(() {
                          _barcode = null;
                          _imageUrl = null;
                        }),
                        tooltip: 'Verwijder barcode',
                      ),
                  ],
                ),
                const SizedBox(height: 24),

                ElevatedButton.icon(
                  icon: Icon(isEditing ? Icons.save : Icons.add),
                  label: Text(
                      isEditing ? 'Wijzigingen opslaan' : 'Toevoegen aan lijst'),
                  onPressed: _save,
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    textStyle: const TextStyle(
                        fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _SuggestionsRow extends StatelessWidget {
  final ShoppingProvider provider;
  final ValueChanged<String> onSuggestionTap;

  const _SuggestionsRow({
    required this.provider,
    required this.onSuggestionTap,
  });

  @override
  Widget build(BuildContext context) {
    final favorites = provider.favoriteNames.toList();
    final recents = provider.recentItems
        .where((r) => !favorites.contains(r))
        .take(5)
        .toList();

    if (favorites.isEmpty && recents.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (favorites.isNotEmpty) ...[
          const Text('Favorieten',
              style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          Wrap(
            spacing: 6,
            runSpacing: 4,
            children: favorites.take(10).map((name) {
              return ActionChip(
                label: Text(name, style: const TextStyle(fontSize: 12)),
                avatar: const Icon(Icons.star, size: 14, color: Colors.amber),
                onPressed: () => onSuggestionTap(name),
                visualDensity: VisualDensity.compact,
              );
            }).toList(),
          ),
          const SizedBox(height: 8),
        ],
        if (recents.isNotEmpty) ...[
          const Text('Recent',
              style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          Wrap(
            spacing: 6,
            runSpacing: 4,
            children: recents.map((name) {
              return ActionChip(
                label: Text(name, style: const TextStyle(fontSize: 12)),
                avatar: const Icon(Icons.history, size: 14),
                onPressed: () => onSuggestionTap(name),
                visualDensity: VisualDensity.compact,
              );
            }).toList(),
          ),
          const SizedBox(height: 8),
        ],
      ],
    );
  }
}
