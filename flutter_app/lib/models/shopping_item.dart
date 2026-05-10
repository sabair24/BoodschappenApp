import 'package:uuid/uuid.dart';

class ShoppingItem {
  final String id;
  final String name;
  final String quantity;
  final String unit;
  final String category;
  final bool isChecked;
  final String note;
  final String? imageUrl;
  final String? brand;
  final String? barcode;
  final int createdAt;

  const ShoppingItem({
    required this.id,
    required this.name,
    this.quantity = '1',
    this.unit = '',
    required this.category,
    this.isChecked = false,
    this.note = '',
    this.imageUrl,
    this.brand,
    this.barcode,
    required this.createdAt,
  });

  factory ShoppingItem.create({
    required String name,
    String quantity = '1',
    String unit = '',
    required String category,
    bool isChecked = false,
    String note = '',
    String? imageUrl,
    String? brand,
    String? barcode,
  }) {
    return ShoppingItem(
      id: const Uuid().v4(),
      name: name,
      quantity: quantity,
      unit: unit,
      category: category,
      isChecked: isChecked,
      note: note,
      imageUrl: imageUrl,
      brand: brand,
      barcode: barcode,
      createdAt: DateTime.now().millisecondsSinceEpoch,
    );
  }

  ShoppingItem copyWith({
    String? id,
    String? name,
    String? quantity,
    String? unit,
    String? category,
    bool? isChecked,
    String? note,
    String? imageUrl,
    String? brand,
    String? barcode,
    int? createdAt,
  }) {
    return ShoppingItem(
      id: id ?? this.id,
      name: name ?? this.name,
      quantity: quantity ?? this.quantity,
      unit: unit ?? this.unit,
      category: category ?? this.category,
      isChecked: isChecked ?? this.isChecked,
      note: note ?? this.note,
      imageUrl: imageUrl ?? this.imageUrl,
      brand: brand ?? this.brand,
      barcode: barcode ?? this.barcode,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'quantity': quantity,
      'unit': unit,
      'category': category,
      'isChecked': isChecked ? 1 : 0,
      'note': note,
      'imageUrl': imageUrl,
      'brand': brand,
      'barcode': barcode,
      'createdAt': createdAt,
    };
  }

  factory ShoppingItem.fromMap(Map<String, dynamic> map) {
    return ShoppingItem(
      id: map['id'] as String,
      name: map['name'] as String,
      quantity: (map['quantity'] as String?) ?? '1',
      unit: (map['unit'] as String?) ?? '',
      category: (map['category'] as String?) ?? 'Overig',
      isChecked: (map['isChecked'] == 1 || map['isChecked'] == true),
      note: (map['note'] as String?) ?? '',
      imageUrl: map['imageUrl'] as String?,
      brand: map['brand'] as String?,
      barcode: map['barcode'] as String?,
      createdAt: (map['createdAt'] as int?) ??
          DateTime.now().millisecondsSinceEpoch,
    );
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ShoppingItem &&
          runtimeType == other.runtimeType &&
          id == other.id;

  @override
  int get hashCode => id.hashCode;
}
