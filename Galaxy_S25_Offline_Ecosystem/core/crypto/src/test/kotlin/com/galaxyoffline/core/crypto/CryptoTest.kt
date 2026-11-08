package com.galaxyoffline.core.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CryptoTest {
    @Test fun aesGcmRoundTripAndTamperDetection() {
        val key = CryptoBox.newKey()
        val ct = CryptoBox.encrypt(key, "gizli".toByteArray())
        assertArrayEquals("gizli".toByteArray(), CryptoBox.decrypt(key, ct))
        ct[ct.size - 1] = (ct.last() + 1).toByte()
        try { CryptoBox.decrypt(key, ct); fail("bozulma algılanmadı") } catch (e: javax.crypto.AEADBadTagException) { }
        try { CryptoBox.decrypt(CryptoBox.newKey(), CryptoBox.encrypt(key, byteArrayOf(1))); fail("yanlış anahtar kabul edildi") } catch (e: javax.crypto.AEADBadTagException) { }
    }

    @Test fun pbkdf2IsDeterministicPerSalt() {
        val s1 = CryptoBox.randomBytes(16)
        val a = CryptoBox.deriveKey("parola".toCharArray(), s1, 1000)
        val b = CryptoBox.deriveKey("parola".toCharArray(), s1, 1000)
        val c = CryptoBox.deriveKey("parola".toCharArray(), CryptoBox.randomBytes(16), 1000)
        assertTrue(CryptoBox.constantTimeEquals(a, b))
        assertFalse(CryptoBox.constantTimeEquals(a, c))
        assertEquals(32, a.size)
    }

    @Test fun rfc6238Vectors() {
        val sha1 = "12345678901234567890".toByteArray()
        val sha256 = "12345678901234567890123456789012".toByteArray()
        val sha512 = "1234567890123456789012345678901234567890123456789012345678901234".toByteArray()
        assertEquals("94287082", Totp.code(sha1, 59, 30, 8, OtpAlgorithm.SHA1))
        assertEquals("07081804", Totp.code(sha1, 1111111109, 30, 8, OtpAlgorithm.SHA1))
        assertEquals("65353130", Totp.code(sha1, 20000000000, 30, 8, OtpAlgorithm.SHA1))
        assertEquals("46119246", Totp.code(sha256, 59, 30, 8, OtpAlgorithm.SHA256))
        assertEquals("90693936", Totp.code(sha512, 59, 30, 8, OtpAlgorithm.SHA512))
        assertEquals(1, Totp.secondsRemaining(59))
    }

    @Test fun base32() {
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", Base32.encode("12345678901234567890".toByteArray()))
        assertArrayEquals("12345678901234567890".toByteArray(), Base32.decode("gezd gnbv gy3t qojq gezd gnbv gy3t qojq"))
        assertFalse(Base32.isValid("1!"))
    }

    @Test fun otpAuthParse() {
        val a = OtpAuthUri.parse("otpauth://totp/ACME%20Co:john@example.com?secret=JBSWY3DPEHPK3PXP&issuer=ACME%20Co&algorithm=SHA1&digits=6&period=30")
        assertEquals("ACME Co", a.issuer); assertEquals("john@example.com", a.account); assertEquals(6, a.digits)
        assertEquals(a, OtpAuthUri.parse(OtpAuthUri.build(a)))
        try { OtpAuthUri.parse("otpauth://hotp/x?secret=JBSWY3DPEHPK3PXP"); fail() } catch (e: OtpAuthUri.ParseException) { }
        try { OtpAuthUri.parse("https://example.com"); fail() } catch (e: OtpAuthUri.ParseException) { }
    }

    @Test fun generatorAndStrength() {
        val o = PasswordOptions(length = 24)
        repeat(50) {
            val p = PasswordGenerator.generate(o)
            assertEquals(24, p.length)
            assertTrue(p.any { it.isUpperCase() } && p.any { it.isLowerCase() } && p.any { it.isDigit() } && p.any { !it.isLetterOrDigit() })
            assertFalse(p.any { it in "Il1O0o" })
        }
        assertEquals(0, PasswordStrength.evaluate("123456").score)
        assertTrue(PasswordStrength.evaluate("galatasaray1905").score <= 1)
        assertTrue(PasswordStrength.evaluate(PasswordGenerator.generate(o)).score >= 3)
    }
}
