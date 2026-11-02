package com.shelflife.ai.domain.model

/**
 * Domain entity representing the local user profile and household context.
 * Stored 100% locally on the device for privacy and offline zero-waste management.
 */
data class UserProfile(
    val name: String = "Elena Rostova",
    val email: String = "elena.rostova@shelflife.local",
    val phone: String = "+1 (555) 019-2834",
    val bio: String = "Zero-waste kitchen enthusiast • Household of 3",
    val householdSize: String = "3 members",
    val dietaryPreference: String = "Eco-conscious & Plant-forward",
    val photoUri: String? = null
)
