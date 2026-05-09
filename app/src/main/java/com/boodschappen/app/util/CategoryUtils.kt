package com.boodschappen.app.util

import com.boodschappen.app.data.local.Category

fun guessCategoryFromName(name: String): Category? {
    val n = name.lowercase().trim()
    return when {
        n.matchesAny(
            "banaan", "appel", "peer", "druif", "aardbei", "framboos", "bosbes",
            "braam", "sinaasappel", "mandarijn", "citroen", "limoen", "mango", "ananas",
            "kiwi", "perzik", "pruim", "kers", "meloen", "watermeloen", "vijg", "dadel",
            "papaja", "lychee", "passievrucht", "tomaat", "komkommer", "paprika", "sla",
            "spinazie", "wortel", "broccoli", "bloemkool", "spruitjes", "ui", "uien",
            "knoflook", "aardappel", "zoete aardappel", "courgette", "aubergine", "prei",
            "champignon", "paddenstoel", "avocado", "gember", "selderij", "paksoi",
            "andijvie", "witlof", "radijs", "biet", "mais", "maïs", "erwtjes", "doperwten",
            "tuinbonen", "asperge", "artisjok", "venkel", "rucola", "postelein"
        ) -> Category.GROENTE_FRUIT

        n.matchesAny(
            "melk", "kaas", "boter", "yoghurt", "kwark", "room", "vla", "slagroom",
            "mozzarella", "cheddar", "gouda", "brie", "camembert", "ricotta", "feta",
            "parmezan", "halvarine", "margarine", "creme fraiche", "crème fraîche",
            "eieren", "ei", "karnemelk", "kefir", "mascarpone", "roomkaas", "smeerkaas"
        ) -> Category.ZUIVEL

        n.matchesAny(
            "kip", "gehakt", "biefstuk", "tartaar", "schnitzel", "kalkoen", "eend",
            "lam", "varken", "spek", "ham", "worst", "salami", "chorizo",
            "zalm", "tonijn", "haring", "makreel", "tilapia", "kabeljauw", "garnalen",
            "mosselen", "inktvis", "vis",
            "kipfilet", "kipschnitzel", "kippenbout", "kipdij", "drumstick",
            "hamlapje", "speklapje", "rookham", "gerookte ham",
            "rookworst", "knakworst", "braadworst",
            "visfilet", "visstick", "viskroket", "zalmfilet", "gerookte zalm",
            "lamsbout", "varkenshaas", "ribeye"
        ) -> Category.VLEES_VIS

        n.matchesAny(
            "brood", "baguette", "beschuit", "bolletje", "stokbrood", "croissant",
            "muffin", "bagel", "ciabatta", "pistolet", "roggebrood", "volkoren",
            "tortilla", "wraptortilla", "pita", "naan", "focaccia", "brioche"
        ) -> Category.BAKKERIJ

        n.matchesAny(
            "water", "cola", "fanta", "sprite", "7up", "pepsi", "sap", "bier",
            "wijn", "thee", "koffie", "limonade", "frisdrank", "appelsap",
            "sinaasappelsap", "smoothie", "redbull", "tonic", "chocomel",
            "chocolademelk", "theezakje", "koffiecups", "cappuccino", "espresso",
            "ijsthee", "sportdrank", "energiedrank", "rosé", "prosecco", "champagne",
            "monster energy"
        ) -> Category.DRANKEN

        n.matchesAny(
            "diepvries", "vriesvers", "ijsje", "ijsco", "magnum", "cornetto",
            "sorbet", "gelato"
        ) -> Category.DIEPVRIES

        n.matchesAny(
            "chocolade", "snoep", "drop", "lolly", "kauwgom", "haribo", "mentos",
            "stroopwafel", "koek", "chips", "popcorn", "nootjes", "wafels", "pepernoten",
            "snickers", "twix", "kitkat", "bounty", "mars", "toblerone", "oreo",
            "biscuit", "crackers", "rijstwafel", "mueslireep"
        ) -> Category.SNOEP_KOEK

        n.matchesAny(
            "zeep", "shampoo", "conditioner", "tandpasta", "tandenborstel",
            "deodorant", "scheerschuim", "scheermesje", "maandverband", "tampon",
            "wattenschijfjes", "bodylotion", "parfum", "zonnebrand",
            "haarlak", "haarverf", "haarspray", "haargel", "haarmousse",
            "aftershave", "lippenstift", "mascara", "foundation", "wattenstokjes",
            "douchegel", "badschuim", "mondwater", "handcreme", "gezichtscreme"
        ) -> Category.VERZORGING

        n.matchesAny(
            "wasmiddel", "afwasmiddel", "vaatwasmiddel", "schoonmaak", "bleek",
            "allesreiniger", "wc-blok", "toiletrollen", "keukenpapier", "vuilniszakken",
            "ziplock", "aluminiumfolie", "plasticfolie", "sponsje", "dweil",
            "afwasborstel", "reinigingsdoekjes"
        ) -> Category.HUISHOUDEN

        else -> null
    }
}

// Short keywords (≤3 chars) use word-boundary matching to prevent false positives.
// E.g. "ham" must NOT match "shampoo", "vis" must NOT match "divisie".
// Longer keywords (4+ chars) use substring matching, safe for Dutch compound words.
private fun String.matchesAny(vararg keywords: String): Boolean {
    val padded = " $this "
    return keywords.any { keyword ->
        when {
            ' ' in keyword -> this.contains(keyword) // multi-word: substring
            keyword.length <= 3 -> {
                // Word-boundary check: surrounding chars must not be letters
                var i = padded.indexOf(keyword)
                while (i != -1) {
                    val before = padded[i - 1]
                    val after  = padded[i + keyword.length]
                    if (!before.isLetter() && !after.isLetter()) return@any true
                    i = padded.indexOf(keyword, i + 1)
                }
                false
            }
            else -> this.contains(keyword) // 4+ chars: substring is safe
        }
    }
}
