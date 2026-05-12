package com.boodschappen.app.data.repository

import com.boodschappen.app.data.local.Category
import com.boodschappen.app.data.remote.ClaudeApiService

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
}
