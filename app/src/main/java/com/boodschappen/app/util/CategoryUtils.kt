package com.boodschappen.app.util

import com.boodschappen.app.data.local.Category

fun guessCategoryFromName(name: String): Category? {
    val n = name.lowercase().trim()
    return when {
        // SNOEP_KOEK vóór DRANKEN — anders matcht "chocolade" op "cola"
        n.hasAny("chocolade", "snoep", "drop", "lolly", "kauwgom", "haribo", "mentos",
            "stroopwafel", "chips", "popcorn", "nootjes", "wafels", "pepernoten",
            "snickers", "twix", "kitkat", "bounty", "m&m", "oreo", "speculaas",
            "marsepein", "toffee", "bonbon", "mueslireep", "energiereep", "rijstwafel") ||
        n.hasWord("koek") || n.hasWord("mars") -> Category.SNOEP_KOEK

        // VERZORGING vóór VLEES_VIS — anders matcht "shampoo" op "ham"
        n.hasAny("nagellak", "nagelschaar", "nagelvijl", "nagelolie",
            "mascara", "make-up", "makeup", "lippenstift", "lipliner", "lipgloss",
            "lipbalm", "lippenbalsem", "foundation", "concealer", "oogschaduw",
            "eyeliner", "blush", "rouge", "bronzer", "highlighter",
            "shampoo", "conditioner", "douchegel", "badschuim", "bodywash",
            "tandpasta", "tandenborstel", "mondwater", "flosdraad",
            "haargel", "haarwax", "haarspray", "haarlak", "haarbalsem", "haarolie",
            "haarserum", "haarverf", "gezichtscrème", "dagcrème", "nachtcrème",
            "gezichtsmasker", "toner", "cleanser", "reinigingsmelk", "micellair",
            "handcrème", "bodylotion", "voetcrème", "deodorant", "antiperspirant",
            "scheerschuim", "scheermesje", "scheermes",
            "maandverband", "tampon", "inlegkruisje",
            "wattenschijfjes", "wattenstaafjes",
            "parfum", "aftershave", "zonnebrand", "vaseline", "pleisters") ||
        n.hasWord("zeep") || n.hasWord("serum") || n.hasWord("nagels") -> Category.VERZORGING

        // DRANKEN vóór GROENTE_FRUIT — anders matcht "sinaasappelsap" op "sinaasappel"
        // hasWord("water") matcht "water" maar NIET "watermeloen"
        n.hasAny("cola", "fanta", "sprite", "7up", "pepsi", "limonade", "frisdrank",
            "appelsap", "sinaasappelsap", "druivensap", "smoothie",
            "redbull", "monster energy", "tonic", "chocomel", "karnemelk",
            "ijsthee", "energiedrank", "sportdrank", "prosecco", "champagne",
            "cava", "jenever", "whisky", "whiskey", "rum", "vodka", "gin",
            "bronwater", "mineraalwater", "kraanwater", "spuitwater",
            "koolzuurwater", "flessenwater") ||
        n.hasWord("water") || n.hasWord("sap") || n.hasWord("bier") ||
        n.hasWord("wijn") || n.hasWord("thee") || n.hasWord("koffie") -> Category.DRANKEN

        // GROENTE & FRUIT — "kers" vervangen door "kersen" om "crackers"-bug te voorkomen
        n.hasAny("banaan", "appel", "peer", "druif", "aardbei", "framboos", "bosbes",
            "braam", "sinaasappel", "mandarijn", "citroen", "limoen", "mango", "ananas",
            "kiwi", "perzik", "pruim", "meloen", "watermeloen", "vijg", "dadel",
            "papaja", "lychee", "passievrucht", "tomaat", "komkommer", "paprika",
            "spinazie", "wortel", "broccoli", "bloemkool", "spruitjes", "knoflook",
            "courgette", "aubergine", "prei", "champignon", "paddenstoel", "avocado",
            "gember", "selderij", "paksoi", "andijvie", "witlof", "radijs", "biet",
            "mais", "erwtjes", "doperwten", "tuinbonen", "asperge", "artisjok",
            "venkel", "rucola", "postelein", "pompoen", "boerenkool", "nectarine",
            "kersen", "aardbeien", "frambozen", "pruimen", "sla") ||
        n.hasWord("ui") || n.hasWord("aardappel") -> Category.GROENTE_FRUIT

        n.hasAny("melk", "kaas", "boter", "yoghurt", "yogurt", "kwark", "room", "vla", "slagroom",
            "mozzarella", "cheddar", "gouda", "brie", "camembert", "ricotta", "feta",
            "parmezan", "halvarine", "margarine", "crème fraîche", "creme fraiche",
            "eieren") ||
        n.hasWord("ei") -> Category.ZUIVEL

        n.hasAny("kip", "gehakt", "biefstuk", "tartaar", "schnitzel", "kalkoen", "eend",
            "speklapje", "chorizo", "zalm", "tonijn", "haring", "makreel",
            "tilapia", "kabeljauw", "garnalen", "mosselen", "inktvis",
            "kipfilet", "drumstick", "ribkarbonade", "gehaktbal", "slavinken",
            "rookworst", "scampi", "pangasius", "ossenhaas", "entrecote",
            "varkensvlees", "kalkoenfilet", "lamsrack", "lamskotelet", "lamsvlees",
            "ham", "spek", "vis", "worst", "salami", "lam", "varken") -> Category.VLEES_VIS

        // BAKKERIJ — "crackers" expliciet toegevoegd, "kers" niet meer hier
        n.hasAny("brood", "baguette", "beschuit", "stokbrood", "croissant", "muffin",
            "bagel", "ciabatta", "pistolet", "roggebrood", "tortilla", "wraptortilla",
            "crackers", "cracker", "knäckebröd", "ontbijtkoek", "krentenbol",
            "rozijnenbrood", "appelflap", "plaatkoek") ||
        n.hasWord("bolletje") || n.hasWord("volkoren") -> Category.BAKKERIJ

        n.hasAny("diepvries", "vriesvers", "ijsco", "magnum", "cornetto",
            "diepgevroren", "bevroren") ||
        n.hasWord("ijsje") -> Category.DIEPVRIES

        n.hasAny("wasmiddel", "wasverzachter", "afwasmiddel", "vaatwasmiddel",
            "vaatwastablet", "schoonmaak", "bleek", "allesreiniger",
            "toiletrollen", "wc-papier", "keukenpapier", "keukenrol", "vuilniszakken",
            "ziplock", "aluminiumfolie", "plasticfolie", "sponsje", "dweil",
            "schoonmaakdoekjes", "keukendoekjes", "wc-blok") -> Category.HUISHOUDEN

        else -> null
    }
}

private fun String.hasAny(vararg words: String) = words.any { this.contains(it) }

private fun String.hasWord(word: String): Boolean {
    var idx = 0
    while (true) {
        idx = this.indexOf(word, idx)
        if (idx == -1) return false
        val before = idx == 0 || !this[idx - 1].isLetter()
        val after = idx + word.length >= this.length || !this[idx + word.length].isLetter()
        if (before && after) return true
        idx++
    }
}
