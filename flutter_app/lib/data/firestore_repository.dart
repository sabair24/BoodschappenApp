import 'dart:math';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:uuid/uuid.dart';
import '../models/shopping_item.dart';

class FirestoreRepository {
  static const String _collection = 'sharedLists';
  static const String _itemsField = 'items';
  static const String _updatedAtField = 'updatedAt';

  final FirebaseFirestore _firestore = FirebaseFirestore.instance;

  Stream<List<ShoppingItem>> getItemsStream(String code) {
    return _firestore
        .collection(_collection)
        .doc(code)
        .snapshots()
        .map((snap) {
      if (!snap.exists) return <ShoppingItem>[];
      final data = snap.data();
      if (data == null) return <ShoppingItem>[];
      final rawItems = data[_itemsField];
      if (rawItems is! List) return <ShoppingItem>[];
      return rawItems
          .whereType<Map<String, dynamic>>()
          .map((m) => ShoppingItem.fromMap(m))
          .toList();
    });
  }

  Future<void> publishList(String code, List<ShoppingItem> items) async {
    await _firestore.collection(_collection).doc(code).set({
      _itemsField: items.map((i) => i.toMap()).toList(),
      _updatedAtField: FieldValue.serverTimestamp(),
    });
  }

  Future<void> createList(String code, List<ShoppingItem> items) async {
    await _firestore.collection(_collection).doc(code).set({
      _itemsField: items.map((i) => i.toMap()).toList(),
      'createdAt': FieldValue.serverTimestamp(),
      _updatedAtField: FieldValue.serverTimestamp(),
    });
  }

  Future<bool> listExists(String code) async {
    final doc = await _firestore.collection(_collection).doc(code).get();
    return doc.exists;
  }

  static String generateCode() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    final rand = Random.secure();
    return List.generate(6, (_) => chars[rand.nextInt(chars.length)]).join();
  }

  static String newItemId() => const Uuid().v4();
}
