package com.shelflife.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileTest {

    @Test
    fun defaultUserProfile_hasExpectedDefaults() {
        val profile = UserProfile()

        assertEquals("Elena Rostova", profile.name)
        assertEquals("elena.rostova@shelflife.local", profile.email)
        assertEquals("+1 (555) 019-2834", profile.phone)
        assertEquals("Zero-waste kitchen enthusiast • Household of 3", profile.bio)
        assertEquals("3 members", profile.householdSize)
        assertEquals("Eco-conscious & Plant-forward", profile.dietaryPreference)
        assertNull(profile.photoUri)
    }

    @Test
    fun customUserProfile_storesCustomFields() {
        val custom = UserProfile(
            name = "Alex Mercer",
            email = "alex@kitchen.local",
            phone = "+1 555-4321",
            bio = "Master baker & sourdough pro",
            householdSize = "2 people",
            dietaryPreference = "Vegetarian",
            photoUri = "file:///data/user/0/com.shelflife.ai/files/user_profile_avatar.jpg"
        )

        assertEquals("Alex Mercer", custom.name)
        assertEquals("alex@kitchen.local", custom.email)
        assertEquals("+1 555-4321", custom.phone)
        assertEquals("Master baker & sourdough pro", custom.bio)
        assertEquals("2 people", custom.householdSize)
        assertEquals("Vegetarian", custom.dietaryPreference)
        assertEquals("file:///data/user/0/com.shelflife.ai/files/user_profile_avatar.jpg", custom.photoUri)
    }
}
