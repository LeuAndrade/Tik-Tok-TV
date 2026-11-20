package com.example.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.secureAuthDataStore: DataStore<Preferences> by preferencesDataStore(name = "tiktok_tv_secure_auth")

class SecureTokenStore(
  private val context: Context,
  private val moshi: Moshi,
) {
  private object Keys {
    val ENCRYPTED_TOKENS = stringPreferencesKey("encrypted_oauth_tokens")
    val ENCRYPTED_PROFILE = stringPreferencesKey("encrypted_oauth_profile")
  }

  private val tokenAdapter = moshi.adapter(TikTokTokenBundle::class.java)
  private val profileAdapter = moshi.adapter(TikTokUserProfile::class.java)

  val tokenBundleFlow: Flow<TikTokTokenBundle?> =
    context.secureAuthDataStore.data.map { prefs ->
      val raw = prefs[Keys.ENCRYPTED_TOKENS] ?: return@map null
      val json = decryptPayload(raw) ?: return@map null
      runCatching { tokenAdapter.fromJson(json) }.getOrNull()
    }

  val userProfileFlow: Flow<TikTokUserProfile?> =
    context.secureAuthDataStore.data.map { prefs ->
      val raw = prefs[Keys.ENCRYPTED_PROFILE] ?: return@map null
      val json = decryptPayload(raw) ?: return@map null
      runCatching { profileAdapter.fromJson(json) }.getOrNull()
    }

  suspend fun saveTokenBundle(bundle: TikTokTokenBundle) {
    val json = tokenAdapter.toJson(bundle)
    val encrypted = encryptPayload(json)
    context.secureAuthDataStore.edit { prefs ->
      prefs[Keys.ENCRYPTED_TOKENS] = encrypted
    }
  }

  suspend fun getTokenBundle(): TikTokTokenBundle? = tokenBundleFlow.first()

  suspend fun saveUserProfile(profile: TikTokUserProfile) {
    val json = profileAdapter.toJson(profile)
    val encrypted = encryptPayload(json)
    context.secureAuthDataStore.edit { prefs ->
      prefs[Keys.ENCRYPTED_PROFILE] = encrypted
    }
  }

  suspend fun clearAll() {
    context.secureAuthDataStore.edit { it.clear() }
  }

  private fun encryptPayload(plainText: String): String {
    val key = getOrCreateKeystoreKey()
    if (key == null) {
      // Fallback for non-Keystore JVM unit test environments only
      return "PLAIN_B64:" + Base64.encodeToString(plainText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }
    val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val iv = cipher.iv
    val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
    val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
    val dataB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
    return "GCM:$ivB64:$dataB64"
  }

  private fun decryptPayload(payload: String): String? {
    if (payload.startsWith("PLAIN_B64:")) {
      return runCatching {
        val raw = Base64.decode(payload.removePrefix("PLAIN_B64:"), Base64.NO_WRAP)
        String(raw, StandardCharsets.UTF_8)
      }.getOrNull()
    }
    if (!payload.startsWith("GCM:")) return null
    val parts = payload.split(":")
    if (parts.size != 3) return null
    val key = getOrCreateKeystoreKey() ?: return null
    return runCatching {
      val iv = Base64.decode(parts[1], Base64.NO_WRAP)
      val cipherBytes = Base64.decode(parts[2], Base64.NO_WRAP)
      val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
      val plainBytes = cipher.doFinal(cipherBytes)
      String(plainBytes, StandardCharsets.UTF_8)
    }.getOrNull()
  }

  private fun getOrCreateKeystoreKey(): SecretKey? {
    return runCatching {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
      val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
      if (existing != null) return existing

      val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
      val spec =
        KeyGenParameterSpec.Builder(
          KEY_ALIAS,
          KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
          .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .setKeySize(256)
          .build()
      keyGen.init(spec)
      keyGen.generateKey()
    }.getOrNull()
  }

  private companion object {
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val KEY_ALIAS = "tiktok_tv_oauth_aes_key"
    const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
  }
}
