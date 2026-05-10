import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:provider/provider.dart';
import '../models/shopping_item.dart';
import '../providers/shopping_provider.dart';
import '../theme/app_theme.dart';

class ItemCard extends StatelessWidget {
  final ShoppingItem item;
  final VoidCallback onTap;
  final VoidCallback onDelete;

  const ItemCard({
    super.key,
    required this.item,
    required this.onTap,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return Consumer<ShoppingProvider>(
      builder: (context, provider, _) {
        final isDark = provider.isDarkTheme;
        final catColor = AppTheme.categoryColor(item.category, isDark);
        final isFav = provider.isFavorite(item.name);

        return Dismissible(
          key: Key(item.id),
          direction: DismissDirection.endToStart,
          background: Container(
            margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.red,
              borderRadius: BorderRadius.circular(12),
            ),
            alignment: Alignment.centerRight,
            padding: const EdgeInsets.only(right: 20),
            child: const Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.delete, color: Colors.white, size: 28),
                SizedBox(height: 2),
                Text('Verwijder', style: TextStyle(color: Colors.white, fontSize: 11)),
              ],
            ),
          ),
          confirmDismiss: (_) async => true,
          onDismissed: (_) => onDelete(),
          child: GestureDetector(
            onTap: onTap,
            child: Container(
              margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              decoration: BoxDecoration(
                color: Theme.of(context).cardTheme.color,
                borderRadius: BorderRadius.circular(12),
                border: Border(left: BorderSide(color: catColor, width: 4)),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(isDark ? 0.3 : 0.06),
                    blurRadius: 4,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Padding(
                padding: const EdgeInsets.all(10),
                child: Row(
                  children: [
                    Transform.scale(
                      scale: 1.1,
                      child: Checkbox(
                        value: item.isChecked,
                        onChanged: (_) => provider.toggleChecked(item.id),
                        activeColor: catColor,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
                      ),
                    ),
                    if (item.imageUrl != null && item.imageUrl!.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(right: 10),
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: CachedNetworkImage(
                            imageUrl: item.imageUrl!,
                            width: 48,
                            height: 48,
                            fit: BoxFit.cover,
                            placeholder: (_, __) => Container(
                              width: 48, height: 48,
                              color: catColor.withOpacity(0.1),
                              child: Icon(Icons.image, color: catColor, size: 24),
                            ),
                            errorWidget: (_, __, ___) => Container(
                              width: 48, height: 48,
                              color: catColor.withOpacity(0.1),
                              child: Icon(Icons.broken_image, color: catColor, size: 24),
                            ),
                          ),
                        ),
                      ),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            item.name,
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w600,
                              color: item.isChecked
                                  ? Theme.of(context).colorScheme.onSurface.withOpacity(0.4)
                                  : Theme.of(context).colorScheme.onSurface,
                              decoration: item.isChecked ? TextDecoration.lineThrough : null,
                              decorationColor: catColor,
                              decorationThickness: 2,
                            ),
                          ),
                          if (item.brand != null && item.brand!.isNotEmpty)
                            Text(
                              item.brand!,
                              style: TextStyle(
                                fontSize: 12,
                                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.55),
                                decoration: item.isChecked ? TextDecoration.lineThrough : null,
                              ),
                            ),
                          if (item.note.isNotEmpty)
                            Text(
                              item.note,
                              style: TextStyle(
                                fontSize: 12,
                                fontStyle: FontStyle.italic,
                                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6),
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                        ],
                      ),
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            _QuantityButton(icon: Icons.remove, onPressed: () => provider.decrementQuantity(item), color: catColor),
                            Padding(
                              padding: const EdgeInsets.symmetric(horizontal: 6),
                              child: Text(
                                item.unit.isNotEmpty ? '${item.quantity} ${item.unit}' : item.quantity,
                                style: TextStyle(
                                  fontSize: 14,
                                  fontWeight: FontWeight.bold,
                                  color: item.isChecked
                                      ? Theme.of(context).colorScheme.onSurface.withOpacity(0.4)
                                      : Theme.of(context).colorScheme.onSurface,
                                  decoration: item.isChecked ? TextDecoration.lineThrough : null,
                                ),
                              ),
                            ),
                            _QuantityButton(icon: Icons.add, onPressed: () => provider.incrementQuantity(item), color: catColor),
                          ],
                        ),
                        const SizedBox(height: 4),
                        GestureDetector(
                          onTap: () => provider.toggleFavorite(item.name),
                          child: Icon(
                            isFav ? Icons.star : Icons.star_border,
                            color: isFav ? Colors.amber : Theme.of(context).colorScheme.onSurface.withOpacity(0.35),
                            size: 20,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

class _QuantityButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback onPressed;
  final Color color;

  const _QuantityButton({required this.icon, required this.onPressed, required this.color});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 26,
      height: 26,
      child: Material(
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(6),
        child: InkWell(
          onTap: onPressed,
          borderRadius: BorderRadius.circular(6),
          child: Icon(icon, size: 16, color: color),
        ),
      ),
    );
  }
}
