import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../models/shopping_item.dart';

class LocalDatabase {
  static final LocalDatabase _instance = LocalDatabase._internal();
  factory LocalDatabase() => _instance;
  LocalDatabase._internal();

  Database? _db;

  Future<Database> get database async {
    _db ??= await _initDb();
    return _db!;
  }

  Future<Database> _initDb() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'boodschappen.db');

    return await openDatabase(
      path,
      version: 1,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE shopping_items (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            quantity TEXT NOT NULL DEFAULT '1',
            unit TEXT NOT NULL DEFAULT '',
            category TEXT NOT NULL DEFAULT 'Overig',
            isChecked INTEGER NOT NULL DEFAULT 0,
            note TEXT NOT NULL DEFAULT '',
            imageUrl TEXT,
            brand TEXT,
            barcode TEXT,
            createdAt INTEGER NOT NULL
          )
        ''');
      },
    );
  }

  Future<List<ShoppingItem>> getAllItems() async {
    final db = await database;
    final maps = await db.query('shopping_items', orderBy: 'createdAt DESC');
    return maps.map((m) => ShoppingItem.fromMap(m)).toList();
  }

  Future<void> insertItem(ShoppingItem item) async {
    final db = await database;
    await db.insert(
      'shopping_items',
      item.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<void> updateItem(ShoppingItem item) async {
    final db = await database;
    await db.update(
      'shopping_items',
      item.toMap(),
      where: 'id = ?',
      whereArgs: [item.id],
    );
  }

  Future<void> deleteItem(String id) async {
    final db = await database;
    await db.delete('shopping_items', where: 'id = ?', whereArgs: [id]);
  }

  Future<void> deleteAllItems() async {
    final db = await database;
    await db.delete('shopping_items');
  }

  Future<void> deleteCheckedItems() async {
    final db = await database;
    await db.delete('shopping_items', where: 'isChecked = 1');
  }

  Future<void> replaceAllItems(List<ShoppingItem> items) async {
    final db = await database;
    await db.transaction((txn) async {
      await txn.delete('shopping_items');
      for (final item in items) {
        await txn.insert('shopping_items', item.toMap());
      }
    });
  }
}
