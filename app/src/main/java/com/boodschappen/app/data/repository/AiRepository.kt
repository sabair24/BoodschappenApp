package com.boodschappen.app.data.repository

import com.boodschappen.app.data.local.Category
import com.boodschappen.app.data.remote.ClaudeApiService
import org.json.JSONArray

data class ReceiptItem(
    val name: String,
    val quantity: String = "1",
    val price: Double? = null
)

class AiRepository(private val claude: ClaudeApiService = ClaudeApiService()) {

    suspend fun suggestCategory(itemName: String): Category? {
        val categories = Category.entries.joinToString(", ") { it.displayName }
        val response = claude.complete(
            prompt = """Categoriseer dit boodschappenproduct in één van deze categorieën: $categories
Product: "$itemName"
Geef ALLEEN de exacte categorienaam terug, niets anders.""",
            maxTokens = 20
        ) ?: return null
        return Category.entries.find { response.contains(it.displayName, ignoreCase = true) }
    }

    suspend fun getSuggestions(
        currentItems: List<String>,
        recents: List<String>,
        favorites: List<String>
    ): List<String> {
        val context = buildString {
            if (currentItems.isNotEmpty()) append("Op de lijst: ${currentItems.take(15).joinToString(", ")}\n")
            if (favorites.isNotEmpty()) append("Favorieten: ${favorites.take(8).joinToString(", ")}\n")
            if (recents.isNotEmpty()) append("Recent: ${recents.take(8).joinToString(", ")}\n")
        }
        if (context.isBlank()) return emptyList()

        val response = claude.complete(
            prompt = """Je bent een boodschappenlijst assistent. Geef 6 slimme suggesties voor producten om toe te voegen.

$context
Regels: geen duplicaten van de lijst, Nederlandse productnamen, maximaal 3 woorden per product.
Geef ALLEEN een komma-gescheiden lijst, geen nummers of uitleg.""",
            maxTokens = 80
        ) ?: return emptyList()

        return response.split(",")
            .map { it.trim().trimEnd('.') }
            .filter { it.isNotBlank() && currentItems.none { cur -> cur.equals(it, ignoreCase = true) } }
            .take(6)
    }

    suspend fun getRecipeIngredients(dish: String): List<String> {
        val response = claude.complete(
            prompt = """Geef de ingrediënten voor: "$dish"
Schrijf in het Nederlands. Geef ALLEEN een komma-gescheiden lijst van ingrediëntnamen zonder hoeveelheden, geen uitleg.""",
            maxTokens = 150
        ) ?: return emptyList()

        return response.split(",")
            .map { it.trim().trimEnd('.') }
            .filter { it.isNotBlank() }
            .take(15)
    }

    suspend fun getNutritionAnalysis(itemNames: List<String>): String? {
        if (itemNames.isEmpty()) return null
        val list = itemNames.take(30).joinToString(", ")
        return claude.complete(
            prompt = """Analyseer deze boodschappenlijst op gezondheid en geef advies in het Nederlands.

Producten: $list

Geef terug:
🏆 Score: [1-10]/10
📊 Analyse: [2 zinnen over de balans van de lijst]
💡 Tips: [2 korte, concrete tips om de lijst gezonder te maken]

Wees positief maar eerlijk.""",
            maxTokens = 300
        )
    }

    suspend fun parseVoiceInput(transcript: String): Triple<String, String, String>? {
        val response = claude.complete(
            prompt = """Verwerk deze gesproken boodschap naar JSON. Geef ALLEEN JSON terug, geen uitleg.
Formaat: {"name":"productnaam","quantity":"hoeveelheid","unit":"eenheid"}
- name: Nederlandstalige productnaam, beginhoofdletter, max 4 woorden
- quantity: alleen het getal als tekst (gebruik "1" als onbekend)
- unit: eenheid zoals "liter", "kg", "pak", "stuks", of "" als onbekend
Voorbeelden:
"twee liter volle melk" → {"name":"Volle melk","quantity":"2","unit":"liter"}
"een pak boter" → {"name":"Boter","quantity":"1","unit":"pak"}
"appels" → {"name":"Appels","quantity":"1","unit":""}
Input: "$transcript"""",
            maxTokens = 80
        ) ?: return null
        return try {
            val cleaned = response.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            val json = org.json.JSONObject(cleaned)
            Triple(
                json.optString("name", transcript).trim().ifBlank { transcript },
                json.optString("quantity", "1").ifBlank { "1" },
                json.optString("unit", "")
            )
        } catch (e: Exception) {
            Triple(transcript.trim().replaceFirstChar { it.uppercaseChar() }, "1", "")
        }
    }

    suspend fun scanReceipt(imageBase64: String): List<ReceiptItem> {
        val response = claude.completeWithImage(
            prompt = """Dit is een kassabon of boodschappenbon. Identificeer alle gekochte producten.
Geef de resultaten terug als JSON array: [{"name": "productnaam", "quantity": "1", "price": 1.99}]
- Gebruik korte Nederlandse productnamen (max 4 woorden)
- price is het bedrag in euros als getal (bijv. 2.49), of laat weg als onbekend
- Geef ALLEEN de JSON array terug, geen andere tekst of markdown""",
            imageBase64 = imageBase64,
            maxTokens = 1024
        ) ?: return emptyList()

        return try {
            val cleaned = response.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            val jsonArray = JSONArray(cleaned)
            (0 until jsonArray.length()).mapNotNull { i ->
                val obj = jsonArray.getJSONObject(i)
                val name = obj.optString("name").trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val qty = obj.optString("quantity", "1").ifBlank { "1" }
                val price = if (obj.has("price")) obj.optDouble("price", -1.0).takeIf { it >= 0 } else null
                ReceiptItem(name = name, quantity = qty, price = price)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
