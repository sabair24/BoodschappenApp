import '../models/category.dart';

AppCategory guessCategoryFromName(String name) {
  final lower = name.toLowerCase().trim();

  for (final entry in _categoryKeywords.entries) {
    final category = entry.key;
    final keywords = entry.value;
    for (final keyword in keywords) {
      if (_matches(lower, keyword)) {
        return category;
      }
    }
  }

  return AppCategory.overig;
}

bool _matches(String text, String keyword) {
  if (keyword.length <= 3) {
    final idx = text.indexOf(keyword);
    if (idx < 0) return false;
    final before = idx > 0 ? text[idx - 1] : ' ';
    final after =
        idx + keyword.length < text.length ? text[idx + keyword.length] : ' ';
    return !_isLetter(before) && !_isLetter(after);
  } else {
    return text.contains(keyword);
  }
}

bool _isLetter(String char) {
  return RegExp(r'[a-záéíóúàèìòùäëïöüâêîôûñ]', caseSensitive: false)
      .hasMatch(char);
}

const Map<AppCategory, List<String>> _categoryKeywords = {
  AppCategory.groenteFruit: [
    'banaan', 'appel', 'peer', 'druif', 'aardbei', 'framboos', 'bosbes',
    'braam', 'sinaasappel', 'mandarijn', 'citroen', 'limoen', 'mango',
    'ananas', 'kiwi', 'perzik', 'pruim', 'kers', 'meloen', 'watermeloen',
    'vijg', 'dadel', 'papaja', 'lychee', 'passievrucht', 'tomaat',
    'komkommer', 'paprika', 'sla', 'spinazie', 'wortel', 'broccoli',
    'bloemkool', 'spruitjes', 'uien', 'ui', 'knoflook', 'aardappel',
    'zoete aardappel', 'courgette', 'aubergine', 'prei', 'champignon',
    'paddenstoel', 'avocado', 'gember', 'selderij', 'paksoi', 'andijvie',
    'witlof', 'radijs', 'biet', 'mais', 'maís', 'erwtjes', 'doperwten',
    'tuinbonen', 'asperge', 'artisjok', 'venkel', 'rucola', 'postelein',
  ],
  AppCategory.zuivel: [
    'melk', 'kaas', 'boter', 'yoghurt', 'kwark', 'room', 'vla', 'slagroom',
    'mozzarella', 'cheddar', 'gouda', 'brie', 'camembert', 'ricotta',
    'feta', 'parmezan', 'halvarine', 'margarine', 'creme fraiche',
    'crème fraîche', 'eieren', 'karnemelk', 'kefir', 'mascarpone',
    'roomkaas', 'smeerkaas', 'ei',
  ],
  AppCategory.vleesVis: [
    'kip', 'gehakt', 'biefstuk', 'tartaar', 'schnitzel', 'kalkoen', 'eend',
    'lam', 'varken', 'spek', 'ham', 'worst', 'salami', 'chorizo', 'zalm',
    'tonijn', 'haring', 'makreel', 'tilapia', 'kabeljauw', 'garnalen',
    'mosselen', 'inktvis', 'vis', 'kipfilet', 'kipschnitzel', 'kippenbout',
    'kipdij', 'drumstick', 'hamlapje', 'speklapje', 'rookham',
    'gerookte ham', 'rookworst', 'knakworst', 'braadworst', 'visfilet',
    'visstick', 'viskroket', 'zalmfilet', 'gerookte zalm', 'lamsbout',
    'varkenshaas', 'ribeye',
  ],
  AppCategory.bakkerij: [
    'brood', 'baguette', 'beschuit', 'bolletje', 'stokbrood', 'croissant',
    'muffin', 'bagel', 'ciabatta', 'pistolet', 'roggebrood', 'volkoren',
    'tortilla', 'wraptortilla', 'pita', 'naan', 'focaccia', 'brioche',
  ],
  AppCategory.dranken: [
    'water', 'cola', 'fanta', 'sprite', '7up', 'pepsi', 'sap', 'bier',
    'wijn', 'thee', 'koffie', 'limonade', 'frisdrank', 'appelsap',
    'sinaasappelsap', 'smoothie', 'redbull', 'tonic', 'chocomel',
    'chocolademelk', 'theezakje', 'koffiecups', 'cappuccino', 'espresso',
    'ijsthee', 'sportdrank', 'energiedrank', 'rosé', 'prosecco',
    'champagne', 'monster energy',
  ],
  AppCategory.diepvries: [
    'diepvries', 'vriesvers', 'ijsje', 'ijsco', 'magnum', 'cornetto',
    'sorbet', 'gelato',
  ],
  AppCategory.snoepKoek: [
    'chocolade', 'snoep', 'drop', 'lolly', 'kauwgom', 'haribo', 'mentos',
    'stroopwafel', 'koek', 'chips', 'popcorn', 'nootjes', 'wafels',
    'pepernoten', 'snickers', 'twix', 'kitkat', 'bounty', 'mars',
    'toblerone', 'oreo', 'biscuit', 'crackers', 'rijstwafel', 'mueslireep',
  ],
  AppCategory.verzorging: [
    'zeep', 'shampoo', 'conditioner', 'tandpasta', 'tandenborstel',
    'deodorant', 'scheerschuim', 'scheermesje', 'maandverband', 'tampon',
    'wattenschijfjes', 'bodylotion', 'parfum', 'zonnebrand', 'haarlak',
    'haarverf', 'haarspray', 'haargel', 'haarmousse', 'aftershave',
    'lippenstift', 'mascara', 'foundation', 'wattenstokjes', 'douchegel',
    'badschuim', 'mondwater', 'handcreme', 'gezichtscreme',
  ],
  AppCategory.huishouden: [
    'wasmiddel', 'afwasmiddel', 'vaatwasmiddel', 'schoonmaak', 'bleek',
    'allesreiniger', 'wc-blok', 'toiletrollen', 'keukenpapier',
    'vuilniszakken', 'ziplock', 'aluminiumfolie', 'plasticfolie', 'sponsje',
    'dweil', 'afwasborstel', 'reinigingsdoekjes',
  ],
};
