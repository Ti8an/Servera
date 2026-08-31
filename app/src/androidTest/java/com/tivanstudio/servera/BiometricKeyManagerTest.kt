package com.tivanstudio.servera

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tivanstudio.servera.data.crypto.BiometricKeyManager
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.KeyGenerator

/**
 * Covers the wrapping itself -- Base64, the stored IV, the GCM tag, and clearing -- against a key
 * built by [BiometricKeyManager.generateBekForTest].
 *
 * The production key insists on an authenticated Cipher, so every doFinal here would throw
 * UserNotAuthenticatedException with no BiometricPrompt to drive. The test key is that same key
 * minus the authentication gate, which leaves exactly the parts worth pinning down in CI. What it
 * cannot cover is the gate: that a real BEK is unusable without a strong biometric, and that a new
 * enrollment invalidates it, are properties of the Keystore and need a device with a finger
 * enrolled to observe.
 */
@RunWith(AndroidJUnit4::class)
class BiometricKeyManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var manager: BiometricKeyManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Plain prefs on a throwaway file: what is stored is already ciphertext, so the
        // production EncryptedSharedPreferences would only add a second, unrelated key to the
        // picture. The user's real "auth_prefs" is never opened.
        context.deleteSharedPreferences(TEST_PREFS)
        prefs = context.getSharedPreferences(TEST_PREFS, Context.MODE_PRIVATE)
        manager = BiometricKeyManager(prefs)
        manager.clearBiometric()
    }

    @After
    fun tearDown() {
        manager.clearBiometric()
        prefs.edit().clear().commit()
        context.deleteSharedPreferences(TEST_PREFS)
    }

    private fun newDek() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun wrappedDekComesBackByteForByte() {
        manager.generateBekForTest()
        val dek = newDek()

        manager.wrapDekWithCipher(dek, manager.getEncryptCipher())

        val decryptCipher = manager.getDecryptCipher()
        assertNotNull("decrypt cipher", decryptCipher)
        val unwrapped = manager.unwrapDekWithCipher(decryptCipher!!)

        assertNotNull("unwrapped DEK", unwrapped)
        assertArrayEquals(dek.encoded, unwrapped!!.encoded)
    }

    @Test
    fun wrapIsReportedOnlyOnceItExists() {
        manager.generateBekForTest()
        assertFalse("nothing wrapped yet", manager.isBiometricWrapPresent())

        manager.wrapDekWithCipher(newDek(), manager.getEncryptCipher())

        assertTrue(manager.isBiometricWrapPresent())
    }

    @Test
    fun withoutAWrapThereIsNoDecryptCipher() {
        manager.generateBekForTest()
        assertNull(manager.getDecryptCipher())
    }

    @Test
    fun rewrappingKeepsTheLatestDek() {
        manager.generateBekForTest()
        manager.wrapDekWithCipher(newDek(), manager.getEncryptCipher())

        val second = newDek()
        manager.wrapDekWithCipher(second, manager.getEncryptCipher())

        val unwrapped = manager.unwrapDekWithCipher(manager.getDecryptCipher()!!)
        assertArrayEquals(second.encoded, unwrapped!!.encoded)
    }

    @Test
    fun everyWrapGetsItsOwnIv() {
        manager.generateBekForTest()
        val dek = newDek()

        manager.wrapDekWithCipher(dek, manager.getEncryptCipher())
        val firstIv = prefs.getString(PREF_BIO_IV, null)
        val firstBlob = prefs.getString(PREF_WRAPPED_DEK_BIO, null)

        manager.wrapDekWithCipher(dek, manager.getEncryptCipher())

        assertNotNull(firstIv)
        // The same DEK under the same key twice: a repeated IV would be a GCM nonce reuse.
        assertFalse("IV reused", firstIv == prefs.getString(PREF_BIO_IV, null))
        assertFalse("ciphertext repeated", firstBlob == prefs.getString(PREF_WRAPPED_DEK_BIO, null))
    }

    @Test
    fun clearingLeavesNothingBehind() {
        manager.generateBekForTest()
        manager.wrapDekWithCipher(newDek(), manager.getEncryptCipher())

        manager.clearBiometric()

        assertFalse(manager.isBiometricWrapPresent())
        assertNull(manager.getDecryptCipher())
        assertNull(prefs.getString(PREF_BIO_IV, null))
        assertNull(prefs.getString(PREF_WRAPPED_DEK_BIO, null))
    }

    @Test
    fun aWrapMadeUnderAnOldKeyDoesNotSurviveANewOne() {
        manager.generateBekForTest()
        manager.wrapDekWithCipher(newDek(), manager.getEncryptCipher())

        // Stands in for what a re-enrollment does to the real key.
        manager.generateBekForTest()

        val cipher = manager.getDecryptCipher()
        // The stored blob is still there, but it was sealed under a key that no longer exists.
        assertNotNull(cipher)
        runCatching { manager.unwrapDekWithCipher(cipher!!) }
            .onSuccess { throw AssertionError("stale wrapping decrypted under a new key") }
    }

    private companion object {
        const val TEST_PREFS = "auth_prefs_biometric_key_test"
        const val PREF_BIO_IV = "wrapped_dek_bio_iv"
        const val PREF_WRAPPED_DEK_BIO = "wrapped_dek_bio"
    }
}
