package com.shelflife.ai.presentation.recipe

import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.domain.model.PantryItem
import java.util.UUID

/**
 * Intelligent zero-waste recipe generation engine that maps expiring pantry ingredients
 * into delicious, practical meals to prevent food waste.
 */
object RecipeGenerator {

    /**
     * Synthesizes tailored zero-waste recipes prioritizing items with CRITICAL (<48h)
     * and WARNING (<5d) shelf-life urgency.
     */
    fun generateZeroWasteRecipes(items: List<PantryItem>): List<Recipe> {
        val inStockItems = items.filter { it.isInStock }
        if (inStockItems.isEmpty()) return emptyList()

        // Sort items by expiration ascending (most urgent first)
        val sortedItems = inStockItems.sortedBy { it.expiryTimestamp }
        val expiringSoon = sortedItems.filter { it.urgency.isUrgent }
        val candidateItems = if (expiringSoon.isNotEmpty()) expiringSoon else sortedItems

        val recipes = mutableListOf<Recipe>()

        // 1. Check for Produce / Vegetables -> Stir-fry or Soup or Salad
        val produceItems = candidateItems.filter { it.category == Category.PRODUCE || it.name.contains("Spinach", true) || it.name.contains("Tomato", true) || it.name.contains("Carrot", true) || it.name.contains("Avocado", true) || it.name.contains("Pepper", true) || it.name.contains("Broccoli", true) }
        if (produceItems.isNotEmpty()) {
            val rescued = produceItems.map { it.name }.distinct()
            recipes.add(
                Recipe(
                    id = UUID.randomUUID().toString(),
                    title = "Crispy Pantry Vegetable Stir-Fry",
                    description = "A rapid, nutrient-packed stir-fry designed specifically to rescue fresh vegetables before they lose their crispness.",
                    rescuedPantryItems = rescued,
                    additionalIngredients = listOf("Olive oil or sesame oil", "Soy sauce", "Garlic", "Cooked rice or noodles"),
                    cookingTimeMinutes = 15,
                    difficulty = "Easy",
                    imageUrl = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&auto=format&fit=crop&q=80",
                    instructions = listOf(
                        "Heat 1 tbsp oil in a skillet or wok over medium-high heat until shimmering.",
                        "Add minced garlic and sauté for 30 seconds until fragrant.",
                        "Toss in ${rescued.joinToString(", ")}, sliced into bite-sized strips.",
                        "Stir-fry briskly for 4-6 minutes until vegetables are tender-crisp.",
                        "Drizzle with 2 tbsp soy sauce, season with black pepper, and serve hot over rice."
                    )
                )
            )
        }

        // 2. Check for Dairy / Eggs / Milk -> Frittata / Scramble or Smoothie
        val dairyItems = candidateItems.filter { it.category == Category.DAIRY || it.name.contains("Milk", true) || it.name.contains("Yogurt", true) || it.name.contains("Cheese", true) || it.name.contains("Egg", true) }
        if (dairyItems.isNotEmpty()) {
            val rescued = dairyItems.map { it.name }.distinct()
            recipes.add(
                Recipe(
                    id = UUID.randomUUID().toString(),
                    title = "Rescue Skillet Frittata",
                    description = "An oven-baked skillet frittata that harmonizes expiring dairy, eggs, and leftover produce into a satisfying high-protein dish.",
                    rescuedPantryItems = rescued,
                    additionalIngredients = listOf("Eggs (or tofu scramble)", "Salt & black pepper", "Olive oil"),
                    cookingTimeMinutes = 20,
                    difficulty = "Easy",
                    imageUrl = "https://images.unsplash.com/photo-1510693206972-df098062cb71?w=500&auto=format&fit=crop&q=80",
                    instructions = listOf(
                        "Preheat your oven to 375°F (190°C) or heat an oven-safe skillet on the stove.",
                        "Whisk eggs with a splash of milk or yogurt and season with salt and pepper.",
                        "Fold in ${rescued.joinToString(", ")} and pour the mixture into the hot oiled skillet.",
                        "Cook on the stovetop for 3 minutes until edges begin to set.",
                        "Transfer to the oven for 10-12 minutes until golden and puffed in the center."
                    )
                )
            )
        }

        // 3. Check for Bakery / Bread -> French Toast or Crispy Garlic Croutons
        val bakeryItems = candidateItems.filter { it.category == Category.BAKERY || it.name.contains("Bread", true) || it.name.contains("Bagel", true) || it.name.contains("Bun", true) }
        if (bakeryItems.isNotEmpty()) {
            val rescued = bakeryItems.map { it.name }.distinct()
            recipes.add(
                Recipe(
                    id = UUID.randomUUID().toString(),
                    title = "Artisan Garlic Herb Croutons",
                    description = "Turn day-old or expiring bread into gourmet crunchy croutons perfect for salads, soups, or snacking.",
                    rescuedPantryItems = rescued,
                    additionalIngredients = listOf("Olive oil", "Garlic powder", "Dried oregano", "Sea salt"),
                    cookingTimeMinutes = 12,
                    difficulty = "Very Easy",
                    imageUrl = "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=500&auto=format&fit=crop&q=80",
                    instructions = listOf(
                        "Preheat oven to 375°F (190°C).",
                        "Cut ${rescued.joinToString(", ")} into 1-inch cubes.",
                        "Toss cubes in a large bowl with 2 tbsp olive oil, garlic powder, oregano, and salt.",
                        "Spread evenly on a baking sheet in a single layer.",
                        "Bake for 10-12 minutes, tossing halfway through, until golden brown and super crunchy."
                    )
                )
            )
        }

        // 4. Universal Catch-All: "Everything-in-the-Pantry" Warm Grain Bowl or Rustic Stew
        val topItems = candidateItems.take(4).map { it.name }.distinct()
        if (recipes.size < 3 && topItems.isNotEmpty()) {
            recipes.add(
                Recipe(
                    id = UUID.randomUUID().toString(),
                    title = "Chef's Zero-Waste Harvest Bowl",
                    description = "A nourishing bowl combining grains, roasted staples, and expiring goods into a vibrant balanced meal.",
                    rescuedPantryItems = topItems,
                    additionalIngredients = listOf("Quinoa or brown rice", "Lemon juice", "Salt & olive oil"),
                    cookingTimeMinutes = 25,
                    difficulty = "Easy",
                    imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop&q=80",
                    instructions = listOf(
                        "Cook a base of rice or quinoa according to package instructions.",
                        "Chop and roast or pan-sear ${topItems.joinToString(", ")} with olive oil and a pinch of salt.",
                        "Assemble bowls with warm grains on bottom, topped with roasted ingredients.",
                        "Drizzle with lemon juice or your favorite dressing and enjoy immediately."
                    )
                )
            )
        }

        return recipes
    }
}
