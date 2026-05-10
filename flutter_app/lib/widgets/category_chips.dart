import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/shopping_provider.dart';
import '../models/category.dart';
import '../theme/app_theme.dart';

class CategoryChips extends StatelessWidget {
  const CategoryChips({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<ShoppingProvider>(
      builder: (context, provider, _) {
        final isDark = provider.isDarkTheme;
        final available = provider.availableCategories;
        final selected = provider.filterCategory;

        final orderedCategories = AppCategory.values
            .where((c) => available.contains(c.displayName))
            .toList();

        if (orderedCategories.isEmpty) return const SizedBox.shrink();

        return SizedBox(
          height: 44,
          child: ListView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
            children: [
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: FilterChip(
                  label: const Text('Alle'),
                  selected: selected == null,
                  onSelected: (_) => provider.setFilterCategory(null),
                  selectedColor: Theme.of(context).colorScheme.primary,
                  checkmarkColor: Colors.white,
                  labelStyle: TextStyle(
                    color: selected == null
                        ? Colors.white
                        : Theme.of(context).colorScheme.onSurface,
                    fontWeight: selected == null
                        ? FontWeight.bold
                        : FontWeight.normal,
                    fontSize: 13,
                  ),
                  backgroundColor: isDark
                      ? const Color(0xFF2A1050)
                      : const Color(0xFFEDE0FF),
                ),
              ),
              ...orderedCategories.map((cat) {
                final isSelected = selected == cat.displayName;
                final color = AppTheme.categoryColor(cat.displayName, isDark);
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: FilterChip(
                    label: Text('${cat.emoji} ${cat.displayName}'),
                    selected: isSelected,
                    onSelected: (_) {
                      provider.setFilterCategory(
                        isSelected ? null : cat.displayName,
                      );
                    },
                    selectedColor: color,
                    checkmarkColor: Colors.white,
                    labelStyle: TextStyle(
                      color: isSelected
                          ? Colors.white
                          : Theme.of(context).colorScheme.onSurface,
                      fontWeight:
                          isSelected ? FontWeight.bold : FontWeight.normal,
                      fontSize: 13,
                    ),
                    backgroundColor: isDark
                        ? const Color(0xFF2A1050)
                        : const Color(0xFFEDE0FF),
                    side: BorderSide(
                      color: isSelected ? color : Colors.transparent,
                    ),
                  ),
                );
              }),
            ],
          ),
        );
      },
    );
  }
}
