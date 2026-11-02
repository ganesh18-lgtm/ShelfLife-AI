package com.shelflife.ai.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.shelflife.ai.data.local.UserProfileManager
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.presentation.recipe.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing AI vision analysis of a scanned grocery or food item.
 */
data class GeminiScannedAnalysis(
    val productName: String,
    val category: Category,
    val estimatedDaysToExpiry: Int,
    val storageTip: String,
    val confidence: Float = 0.9f
)

/**
 * Centralized Gemini 1.5 Flash AI integration service for ShelfLife AI.
 * Powers:
 * 1. AI Zero-Waste Chef Recipe Synthesis from expiring pantry items.
 * 2. Visual Camera Food & Expiry Analyzer in Scanner page.
 * 3. Conversational AI Assistant grounded in the user's live SQLite Room pantry inventory.
 */
@Singleton
class GeminiManager @Inject constructor(
    private val userProfileManager: UserProfileManager
) {

    companion object {
        private const val TAG = "GeminiManager"
        const val MODEL_FLASH = "gemini-3.5-flash-lite"
    }

    private fun getGenerativeModel(): GenerativeModel? {
        val apiKey = userProfileManager.getActiveGeminiApiKey().trim()
        if (apiKey.isBlank()) {
            Log.w(TAG, "No Gemini API key configured. Provide an API key in Settings or local.properties.")
            return null
        }
        return GenerativeModel(
            modelName = MODEL_FLASH,
            apiKey = apiKey
        )
    }

    fun hasApiKey(): Boolean = userProfileManager.hasGeminiApiKey()

    /**
     * Synthesizes tailored zero-waste recipes using items currently in the user's pantry.
     */
    suspend fun generateZeroWasteRecipes(pantryItems: List<PantryItem>): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        val model = getGenerativeModel()
            ?: return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))

        if (pantryItems.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val ingredientListStr = pantryItems.take(15).joinToString("\n") { item ->
            val expiryDateStr = dateFormat.format(Date(item.expiryTimestamp))
            "- ${item.name} (Qty: ${item.quantity}, Category: ${item.category.displayName}, Expires: $expiryDateStr, Status: ${item.urgency.name})"
        }

        val prompt = """
            You are an expert zero-waste chef AI for the app ShelfLife AI.
            Here is the user's pantry inventory with expiration status:
            $ingredientListStr

            Task: Synthesize 2 to 3 delicious, practical zero-waste recipes specifically designed to rescue items that are expiring soonest.
            
            Respond ONLY with a valid JSON array of objects. Do NOT include markdown code fences (like ```json), no intro, and no outro.
            Each object in the array must strictly match this schema:
            [
              {
                "title": "Recipe Title",
                "description": "Short 1-2 sentence description explaining which ingredients are rescued.",
                "rescuedPantryItems": ["Item 1", "Item 2"],
                "additionalIngredients": ["Pantry staple 1", "Pantry staple 2"],
                "cookingTimeMinutes": 20,
                "difficulty": "Easy",
                "imageUrl": "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&auto=format&fit=crop&q=80",
                "instructions": [
                  "Step 1 instruction",
                  "Step 2 instruction"
                ]
              }
            ]
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val responseText = response.text?.trim()
                ?: return@withContext Result.failure(Exception("Empty response from Gemini Flash"))

            val cleanJson = cleanJsonResponse(responseText)
            val jsonArray = JSONArray(cleanJson)
            val recipes = mutableListOf<Recipe>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", "Zero-Waste Chef Creation")
                val description = obj.optString("description", "A tasty meal made from your pantry ingredients.")
                val rescuedJson = obj.optJSONArray("rescuedPantryItems") ?: JSONArray()
                val rescued = mutableListOf<String>()
                for (j in 0 until rescuedJson.length()) {
                    rescued.add(rescuedJson.getString(j))
                }

                val addlJson = obj.optJSONArray("additionalIngredients") ?: JSONArray()
                val additional = mutableListOf<String>()
                for (j in 0 until addlJson.length()) {
                    additional.add(addlJson.getString(j))
                }

                val cookingTime = obj.optInt("cookingTimeMinutes", 15)
                val difficulty = obj.optString("difficulty", "Easy")
                val imageUrl = obj.optString("imageUrl", "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&auto=format&fit=crop&q=80")
                val instructionsJson = obj.optJSONArray("instructions") ?: JSONArray()
                val instructions = mutableListOf<String>()
                for (j in 0 until instructionsJson.length()) {
                    instructions.add(instructionsJson.getString(j))
                }

                recipes.add(
                    Recipe(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        description = description,
                        rescuedPantryItems = rescued,
                        additionalIngredients = additional,
                        cookingTimeMinutes = cookingTime,
                        difficulty = difficulty,
                        imageUrl = imageUrl,
                        instructions = instructions
                    )
                )
            }
            Result.success(recipes)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating recipes with Gemini Flash", e)
            Result.failure(e)
        }
    }

    /**
     * Analyzes a camera bitmap to detect food name, category, shelf-life, and storage guidance.
     */
    suspend fun analyzeFoodImage(bitmap: Bitmap): Result<GeminiScannedAnalysis> = withContext(Dispatchers.IO) {
        val model = getGenerativeModel()
            ?: return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))

        val prompt = """
            Analyze this image of a grocery or pantry item.
            Identify:
            1. The specific food or product name (e.g. 'Ripe Hass Avocado', 'Whole Milk', 'Sourdough Bread').
            2. The food category: choose strictly one of [PRODUCE, DAIRY, MEAT, BAKERY, PANTRY, BEVERAGES, FROZEN, OTHER].
            3. The estimated shelf life in days from today (integer) under standard home storage.
            4. One practical zero-waste storage tip to maximize freshness.
            
            Respond ONLY with a valid JSON object. Do not include markdown code fences (like ```json), no commentary.
            {
              "productName": "Item Name",
              "category": "PRODUCE",
              "estimatedDaysToExpiry": 5,
              "storageTip": "Keep in a cool dry place away from direct sunlight."
            }
        """.trimIndent()

        try {
            val inputContent = content {
                image(bitmap)
                text(prompt)
            }
            val response = model.generateContent(inputContent)
            val responseText = response.text?.trim()
                ?: return@withContext Result.failure(Exception("Empty vision response from Gemini"))

            val cleanJson = cleanJsonResponse(responseText)
            val json = JSONObject(cleanJson)

            val productName = json.optString("productName", "Scanned Food Item")
            val categoryStr = json.optString("category", "PRODUCE")
            val days = json.optInt("estimatedDaysToExpiry", 7)
            val storageTip = json.optString("storageTip", "Store properly to prolong shelf life.")

            val category = Category.fromString(categoryStr)

            Result.success(
                GeminiScannedAnalysis(
                    productName = productName,
                    category = category,
                    estimatedDaysToExpiry = days,
                    storageTip = storageTip
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image with Gemini Flash Vision", e)
            Result.failure(e)
        }
    }

    /**
     * Answers conversational queries about the user's pantry Room database.
     */
    suspend fun chatWithPantry(
        userQuery: String,
        pantryItems: List<PantryItem>,
        chatHistory: List<Pair<String, Boolean>> // (message, isUser)
    ): Result<String> = withContext(Dispatchers.IO) {
        val model = getGenerativeModel()
            ?: return@withContext Result.failure(IllegalStateException("Gemini API key is not configured. Please enter your free key in Settings or Chat."))

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val pantrySummary = if (pantryItems.isEmpty()) {
            "The user's pantry currently has 0 items."
        } else {
            pantryItems.joinToString("\n") { item ->
                val dateStr = dateFormat.format(Date(item.expiryTimestamp))
                "- ${item.name} | Category: ${item.category.displayName} | Qty: ${item.quantity} | Expires: $dateStr | Urgency: ${item.urgency.name}"
            }
        }

        val prompt = """
            You are ShelfLife AI Assistant, an intelligent, friendly culinary and food-waste prevention companion.
            
            USER'S LIVE PANTRY INVENTORY (from SQLite Room Database):
            $pantrySummary

            Conversation Guidelines:
            1. You have direct knowledge of the user's current pantry items listed above.
            2. Answer the user's query accurately using their inventory.
            3. Prioritize items that are marked CRITICAL or WARNING (expiring soon).
            4. Provide concise, actionable advice (recipes, storage hacks, expiration alerts, shopping reminders).
            5. Use formatting like bullet points and bold text where helpful.
            
            User Query: $userQuery
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val answer = response.text?.trim()
                ?: "I couldn't process that query. Please try again."
            Result.success(answer)
        } catch (e: Exception) {
            Log.e(TAG, "Error in chatWithPantry", e)
            Result.failure(e)
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json").trim()
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```").trim()
        }
        return clean.trim()
    }
}
