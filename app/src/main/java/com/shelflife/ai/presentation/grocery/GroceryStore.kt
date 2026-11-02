package com.shelflife.ai.presentation.grocery

import androidx.compose.ui.graphics.Color

data class GroceryStore(
    val id: String,
    val name: String,
    val description: String,
    val packageName: String,
    val webUrl: String,
    val accentColor: Color,
    val badge: String,
    val deliveryTime: String,
    val logoUrl: String,
    val bannerUrl: String
)

object GroceryStores {
    val supportedStores = listOf(
        GroceryStore(
            id = "blinkit",
            name = "Blinkit",
            description = "Instant 10-minute grocery & fresh vegetable delivery",
            packageName = "com.grofers.customerapp",
            webUrl = "https://blinkit.com",
            accentColor = Color(0xFFF7CB44),
            badge = "10 Min",
            deliveryTime = "10-15 mins",
            logoUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=200&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=600&auto=format&fit=crop&q=80"
        ),
        GroceryStore(
            id = "swiggy_instamart",
            name = "Swiggy Instamart",
            description = "Groceries, dairy, snacks & household essentials",
            packageName = "in.swiggy.android",
            webUrl = "https://www.swiggy.com/instamart",
            accentColor = Color(0xFFFC8019),
            badge = "Instant",
            deliveryTime = "15-20 mins",
            logoUrl = "https://images.unsplash.com/photo-1526367790999-0150786686a2?w=200&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1526367790999-0150786686a2?w=600&auto=format&fit=crop&q=80"
        ),
        GroceryStore(
            id = "zepto",
            name = "Zepto",
            description = "Ultra-fast groceries, farm-fresh produce & meat",
            packageName = "com.zeptoconsumerapp",
            webUrl = "https://www.zeptonow.com",
            accentColor = Color(0xFFA855F7),
            badge = "10 Min",
            deliveryTime = "10 mins",
            logoUrl = "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=200&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=600&auto=format&fit=crop&q=80"
        ),
        GroceryStore(
            id = "amazon_fresh",
            name = "Amazon Fresh",
            description = "Quality fruits, vegetables, dairy & pantry staples",
            packageName = "in.amazon.mShop.android.shopping",
            webUrl = "https://www.amazon.in/fresh",
            accentColor = Color(0xFF00A8E1),
            badge = "Same Day",
            deliveryTime = "2-4 hours",
            logoUrl = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=200&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=600&auto=format&fit=crop&q=80"
        ),
        GroceryStore(
            id = "bigbasket",
            name = "BigBasket",
            description = "Comprehensive online supermarket & organic essentials",
            packageName = "com.bigbasket.mobileapp",
            webUrl = "https://www.bigbasket.com",
            accentColor = Color(0xFF84C225),
            badge = "Supermarket",
            deliveryTime = "Scheduled / 15-30m",
            logoUrl = "https://images.unsplash.com/photo-1588964895597-cfccd6e2dbf9?w=200&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1588964895597-cfccd6e2dbf9?w=600&auto=format&fit=crop&q=80"
        )
    )

    data class QuickOrderItem(
        val name: String,
        val category: String,
        val defaultDays: Int,
        val imageUrl: String
    )

    val popularOrderItems = listOf(
        QuickOrderItem("Whole Milk", "Dairy", 7, "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=160&auto=format&fit=crop&q=80"),
        QuickOrderItem("Farm Fresh Eggs", "Dairy", 14, "https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?w=160&auto=format&fit=crop&q=80"),
        QuickOrderItem("Sourdough Bread", "Bakery", 5, "https://images.unsplash.com/photo-1589367920969-ab8e050bbb04?w=160&auto=format&fit=crop&q=80"),
        QuickOrderItem("Organic Bananas", "Produce", 5, "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=160&auto=format&fit=crop&q=80"),
        QuickOrderItem("Fresh Tomatoes", "Produce", 6, "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=160&auto=format&fit=crop&q=80"),
        QuickOrderItem("Baby Spinach", "Produce", 4, "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=160&auto=format&fit=crop&q=80"),
        QuickOrderItem("Chicken Breast", "Meat & Seafood", 3, "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=160&auto=format&fit=crop&q=80"),
        QuickOrderItem("Greek Yogurt", "Dairy", 10, "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=160&auto=format&fit=crop&q=80"),
        QuickOrderItem("Cheddar Cheese", "Dairy", 21, "https://images.unsplash.com/photo-1618164436241-4473940d1f5c?w=160&auto=format&fit=crop&q=80"),
        QuickOrderItem("Basmati Rice", "Pantry Staples", 90, "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=160&auto=format&fit=crop&q=80"),
        QuickOrderItem("Rolled Oats", "Pantry Staples", 60, "https://images.unsplash.com/photo-1586444248902-2f64eddc13df?w=160&auto=format&fit=crop&q=80"),
        QuickOrderItem("Apples", "Produce", 10, "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=160&auto=format&fit=crop&q=80")
    )
}
