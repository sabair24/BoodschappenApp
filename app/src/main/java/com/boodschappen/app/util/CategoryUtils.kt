package com.boodschappen.app.util

import com.boodschappen.app.data.local.Category

fun guessCategoryFromName(name: String): Category? {
    val n = name.lowercase()
    return when {
        n.hasAny("banaan", "appel", "peer", "druif", "aardbei", "framboos", "bosbes",
            "braam", "sinaasappel", "mandarijn", "citroen", "limoen", "mango", "ananas",
            "kiwi", "perzik", "pruim", "kers", "meloen", "watermeloen", "vijg", "dadel",
            "papaja", "lychee", "passievrucht", "tomaat", "komkommer", "paprika", "sla",
            "spinazie", "wortel", "broccoli", "bloemkool", "spruitjes", "ui", "knoflook",
            "aardappel", "zoete aardappel", "courgette", "aubergine", "prei", "champignon",
            "paddenstoel", "avocado", "gember", "selderij", "paksoi", "andijvie", "witlof",
            "radijs", "biet", "mais", "erwtjes", "doperwten", "tuinbonen", "asperge",
            "artisjok", "venkel", "rucola", "postelein") -> Category.GROENTE_FRUIT

        n.hasAny("melk", "kaas", "boter", "yoghurt", "kwark", "room", "vla", "slagroom",
            "mozzarella", "cheddar", "gouda", "brie", "camembert", "ricotta", "feta",
            "parmezan", "halvarine", "margarine", "creme fraiche", "crème fraîche",
            "eieren", "ei ") -> Category.ZUIVEL

        n.hasAny("kip", "gehakt", "biefstuk", "tartaar", "schnitzel", "kalkoen", "eend",
            "lam", "varken", "speklapje", "spek", "ham", "worst", "salami", "chorizo",
            "zalm", "tonijn", "haring", "makreel", "tilapia", "kabeljauw", "garnalen",
            "mosselen", "inktvis", "vis ") -> Category.VLEES_VIS

        n.hasAny("brood", "baguette", "beschuit", "bolletje", "stokbrood", "croissant",
            "muffin", "bagel", "ciabatta", "pistolet", "roggebrood", "volkoren",
            "tortilla", "wraptortilla") -> Category.BAKKERIJ

        n.hasAny("water", "cola", "fanta", "sprite", "7up", "pepsi", "sap", "bier",
            "wijn", "thee ", "koffie", "limonade", "frisdrank", "appelsap",
            "sinaasappelsap", "smoothie", "redbull", "monster energy", "tonic",
            "chocomel", "karnemelk") -> Category.DRANKEN

        n.hasAny("diepvries", "vriesvers", "ijsje", "ijsco", "magnum", "cornetto") -> Category.DIEPVRIES

        n.hasAny("chocolade", "snoep", "drop", "lolly", "kauwgom", "haribo", "mentos",
            "stroopwafel", "koek", "chips", "popcorn", "nootjes", "wafels", "pepernoten",
            "snickers", "twix", "kitkat", "bounty", "mars ") -> Category.SNOEP_KOEK

        n.hasAny("zeep", "shampoo", "conditioner", "tandpasta", "tandenborstel",
            "deodorant", "scheerschuim", "scheermesje", "maandverband", "tampon",
            "wattenschijfjes", "bodylotion", "parfum", "zonnebrand") -> Category.VERZORGING

        n.hasAny("wasmiddel", "afwasmiddel", "vaatwasmiddel", "schoonmaak", "bleek",
            "allesreiniger", "wc-blok", "toiletrollen", "keukenpapier", "vuilniszakken",
            "ziplock", "aluminiumfolie", "plasticfolie", "sponsje", "dweil") -> Category.HUISHOUDEN

        else -> null
    }
}

private fun String.hasAny(vararg words: String) = words.any { this.contains(it) }
