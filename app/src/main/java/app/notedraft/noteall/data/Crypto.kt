package app.notedraft.noteall.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 笔记加密。参数还原自原始 APK：
 *  - 密钥派生：PBKDF2WithHmacSHA1，迭代 100000 次，输出 256 bit
 *  - 对称加密：AES/GCM/NoPadding，128 bit tag，IV 12 字节，salt 16 字节
 *  - 负载布局：salt(16) || iv(12) || ciphertext+tag，整体 Base64 存储
 *  - encryption_salt 列格式：base64(salt) + ":" + base64(派生密钥)
 */
object Crypto {

    private const val ITERATIONS = 100_000
    private const val KEY_BITS = 256
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** 加密正文，返回可直接写入 body 列的 Base64 字符串。 */
    fun encrypt(plain: String, password: CharArray): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LEN).also(random::nextBytes)
        val iv = ByteArray(IV_LEN).also(random::nextBytes)
        val key = SecretKeySpec(deriveKey(password, salt), "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val out = ByteArray(SALT_LEN + IV_LEN + encrypted.size)
        System.arraycopy(salt, 0, out, 0, SALT_LEN)
        System.arraycopy(iv, 0, out, SALT_LEN, IV_LEN)
        System.arraycopy(encrypted, 0, out, SALT_LEN + IV_LEN, encrypted.size)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    /** 解密 body 列内容；失败时返回 null。 */
    fun decrypt(payload: String, password: CharArray): String? {
        return try {
            val raw = Base64.decode(payload, Base64.NO_WRAP)
            if (raw.size < SALT_LEN + IV_LEN + 16) return null
            val salt = raw.copyOfRange(0, SALT_LEN)
            val iv = raw.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
            val data = raw.copyOfRange(SALT_LEN + IV_LEN, raw.size)
            val key = SecretKeySpec(deriveKey(password, salt), "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /** 生成 encryption_salt 列的值。 */
    fun makeVerifier(password: CharArray): String {
        val salt = ByteArray(SALT_LEN).also(SecureRandom()::nextBytes)
        val derived = deriveKey(password, salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(derived, Base64.NO_WRAP)
    }

    /** 校验密码是否与 encryption_salt 列匹配。 */
    fun verify(password: CharArray, verifier: String?): Boolean {
        if (verifier.isNullOrBlank()) return false
        return try {
            val parts = verifier.split(":")
            if (parts.size != 2) return false
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expected = Base64.decode(parts[1], Base64.NO_WRAP)
            derivedEquals(deriveKey(password, salt), expected)
        } catch (e: Exception) {
            false
        }
    }

    private fun derivedEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}