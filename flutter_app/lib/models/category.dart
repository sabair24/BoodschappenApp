enum AppCategory {
  groenteFruit('Groente & Fruit', '🥦'),
  zuivel('Zuivel', '🧀'),
  vleesVis('Vlees & Vis', '🥩'),
  bakkerij('Bakkerij', '🍞'),
  dranken('Dranken', '🥤'),
  diepvries('Diepvries', '🧧'),
  snoepKoek('Snoep & Koek', '🍪'),
  verzorging('Verzorging', '🧴'),
  huishouden('Huishouden', '�a7a'),
  overig('Overig', '🛒');

  const AppCategory(this.displayName, this.emoji);

  final String displayName;
  final String emoji;

  static AppCategory fromName(String name) {
    for (final cat in AppCategory.values) {
      if (cat.displayName == name) return cat;
    }
    return AppCategory.overig;
  }

  static List<String> get allDisplayNames =>
      AppCategory.values.map((c) => c.displayName).toList();
}
