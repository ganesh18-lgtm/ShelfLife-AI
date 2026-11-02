package com.shelflife.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.shelflife.ai.BuildConfig
import com.shelflife.ai.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local privacy-first manager for user profile information, household size,
 * dietary preferences, custom avatar photos, and Gemini API key.
 * All data is stored strictly on device storage.
 */
@Singleton
open class UserProfileManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadProfile())
    open val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(loadGeminiApiKey())
    open val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private fun loadProfile(): UserProfile {
        return UserProfile(
            name = prefs.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME,
            email = prefs.getString(KEY_EMAIL, DEFAULT_EMAIL) ?: DEFAULT_EMAIL,
            phone = prefs.getString(KEY_PHONE, DEFAULT_PHONE) ?: DEFAULT_PHONE,
            bio = prefs.getString(KEY_BIO, DEFAULT_BIO) ?: DEFAULT_BIO,
            householdSize = prefs.getString(KEY_HOUSEHOLD_SIZE, DEFAULT_HOUSEHOLD) ?: DEFAULT_HOUSEHOLD,
            dietaryPreference = prefs.getString(KEY_DIETARY_PREF, DEFAULT_DIETARY) ?: DEFAULT_DIETARY,
            photoUri = prefs.getString(KEY_PHOTO_URI, null)
        )
    }

    private fun loadGeminiApiKey(): String {
        val saved = prefs.getString(KEY_GEMINI_API_KEY, null)?.trim()
        if (!saved.isNullOrBlank()) return saved
        // Fallback to BuildConfig if configured in local.properties
        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }
    }

    open fun getActiveGeminiApiKey(): String {
        val inMemory = _geminiApiKey.value.trim()
        if (inMemory.isNotBlank()) return inMemory
        val saved = prefs.getString(KEY_GEMINI_API_KEY, null)?.trim()
        if (!saved.isNullOrBlank()) return saved
        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }
    }

    open fun setGeminiApiKey(key: String) {
        val cleanKey = key.trim()
        prefs.edit().putString(KEY_GEMINI_API_KEY, cleanKey).apply()
        _geminiApiKey.value = cleanKey
    }

    open fun hasGeminiApiKey(): Boolean = getActiveGeminiApiKey().isNotBlank()

    /**
     * Updates and persists profile fields to local storage, broadcasting
     * the new state immediately to all active observers.
     */
    fun updateProfile(
        name: String,
        email: String,
        phone: String = DEFAULT_PHONE,
        bio: String = DEFAULT_BIO,
        householdSize: String,
        dietaryPreference: String,
        photoUri: String?
    ) {
        val updated = UserProfile(
            name = name.trim().ifBlank { DEFAULT_NAME },
            email = email.trim().ifBlank { DEFAULT_EMAIL },
            phone = phone.trim().ifBlank { DEFAULT_PHONE },
            bio = bio.trim().ifBlank { DEFAULT_BIO },
            householdSize = householdSize.trim().ifBlank { DEFAULT_HOUSEHOLD },
            dietaryPreference = dietaryPreference.trim().ifBlank { DEFAULT_DIETARY },
            photoUri = photoUri
        )

        prefs.edit()
            .putString(KEY_NAME, updated.name)
            .putString(KEY_EMAIL, updated.email)
            .putString(KEY_PHONE, updated.phone)
            .putString(KEY_BIO, updated.bio)
            .putString(KEY_HOUSEHOLD_SIZE, updated.householdSize)
            .putString(KEY_DIETARY_PREF, updated.dietaryPreference)
            .putString(KEY_PHOTO_URI, updated.photoUri)
            .apply()

        _profile.value = updated
    }

    /**
     * Overload for backward compatibility without phone parameter.
     */
    fun updateProfile(
        name: String,
        email: String,
        bio: String,
        householdSize: String,
        dietaryPreference: String,
        photoUri: String?
    ) {
        updateProfile(
            name = name,
            email = email,
            phone = _profile.value.phone,
            bio = bio,
            householdSize = householdSize,
            dietaryPreference = dietaryPreference,
            photoUri = photoUri
        )
    }

    /**
     * Safely saves a captured Bitmap from the camera into internal app storage
     * (filesDir), ensuring the app retains permanent offline read access.
     *
     * @return Local file URI string (e.g. file:///data/user/0/.../user_profile_avatar.jpg) or null if save failed.
     */
    fun saveImageFromBitmap(bitmap: Bitmap): String? {
        return try {
            val avatarFile = File(context.filesDir, AVATAR_FILENAME)
            avatarFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            }
            val savedUri = Uri.fromFile(avatarFile).toString()
            Log.d(TAG, "Successfully cached profile image from camera to: $savedUri")
            savedUri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user avatar from camera", e)
            null
        }
    }

    /**
     * Safely copies an image chosen from Gallery or Camera into internal app storage
     * (filesDir), ensuring the app retains permanent offline read access without
     * temporary URI permission expiry.
     *
     * @return Local file URI string (e.g. file:///data/user/0/.../user_profile_avatar.jpg) or null if copy failed.
     */
    fun saveImageFromUri(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val avatarFile = File(context.filesDir, AVATAR_FILENAME)
            avatarFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            val savedUri = Uri.fromFile(avatarFile).toString()
            Log.d(TAG, "Successfully cached profile image to: $savedUri")
            savedUri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy user avatar from URI: $uri", e)
            null
        }
    }

    /**
     * Resets profile back to factory default values.
     */
    fun resetProfile() {
        prefs.edit().clear().apply()
        try {
            val file = File(context.filesDir, AVATAR_FILENAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete cached avatar", e)
        }
        _profile.value = UserProfile()
    }

    companion object {
        private const val TAG = "UserProfileManager"
        private const val PREFS_NAME = "shelflife_user_profile"
        private const val AVATAR_FILENAME = "user_profile_avatar.jpg"

        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_BIO = "user_bio"
        private const val KEY_HOUSEHOLD_SIZE = "household_size"
        private const val KEY_DIETARY_PREF = "dietary_preference"
        private const val KEY_PHOTO_URI = "photo_uri"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"

        const val DEFAULT_NAME = "Elena Rostova"
        const val DEFAULT_EMAIL = "elena.rostova@shelflife.local"
        const val DEFAULT_PHONE = "+1 (555) 019-2834"
        const val DEFAULT_BIO = "Zero-waste kitchen enthusiast • Household of 3"
        const val DEFAULT_HOUSEHOLD = "3 members"
        const val DEFAULT_DIETARY = "Eco-conscious & Plant-forward"
    }
}
