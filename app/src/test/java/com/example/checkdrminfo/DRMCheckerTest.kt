package com.example.checkdrminfo

import android.media.MediaDrm
import android.util.Log
import androidx.media3.common.C
import com.example.checkdrminfo.model.DrmResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DRMCheckerTest {

    private lateinit var mockDrm: MediaDrm
    private lateinit var checker: DRMChecker

    @Before
    fun setUp() {
        mockkStatic(MediaDrm::class)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        // relaxed mock: close()→Unit, getPropertyString→"" by default
        mockDrm = mockk(relaxed = true)
        checker = DRMChecker(mediaDrmFactory = { mockDrm })
    }

    @After
    fun tearDown() = unmockkAll()

    // ── Widevine ──────────────────────────────────────────────────────────────

    @Test
    fun `checkWidevineSupport - not supported returns NotSupported`() {
        every { MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID) } returns false
        assertTrue(checker.checkWidevineSupport() is DrmResult.NotSupported)
    }

    @Test
    fun `checkWidevineSupport - exception returns CheckError`() {
        every { MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID) } throws RuntimeException("boom")
        assertTrue(checker.checkWidevineSupport() is DrmResult.CheckError)
    }

    @Test
    fun `checkWidevineSupport - L1 device returns Supported containing L1`() {
        every { MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID) } returns true
        every { mockDrm.getPropertyString("securityLevel") } returns "L1"
        every { mockDrm.getPropertyString("hdcpLevel") }     returns "HW_SECURE_ALL"
        every { mockDrm.getPropertyString("vendor") }        returns "Google"
        every { mockDrm.getPropertyString("version") }       returns "16.0"

        val result = checker.checkWidevineSupport()
        assertTrue(result is DrmResult.Supported)
        assertTrue((result as DrmResult.Supported).details.contains("L1"))
    }

    @Test
    fun `checkWidevineSupport - L3 device returns Supported containing L3`() {
        every { MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID) } returns true
        every { mockDrm.getPropertyString("securityLevel") } returns "L3"
        every { mockDrm.getPropertyString("hdcpLevel") }     returns "NONE"
        every { mockDrm.getPropertyString("vendor") }        returns "Acme"
        every { mockDrm.getPropertyString("version") }       returns "14.0"

        val result = checker.checkWidevineSupport()
        assertTrue(result is DrmResult.Supported)
        assertTrue((result as DrmResult.Supported).details.contains("L3"))
    }

    // ── ClearKey ──────────────────────────────────────────────────────────────

    @Test
    fun `checkClearKeySupport - not supported returns NotSupported`() {
        every { MediaDrm.isCryptoSchemeSupported(C.CLEARKEY_UUID) } returns false
        assertTrue(checker.checkClearKeySupport() is DrmResult.NotSupported)
    }

    @Test
    fun `checkClearKeySupport - exception returns CheckError`() {
        every { MediaDrm.isCryptoSchemeSupported(C.CLEARKEY_UUID) } throws RuntimeException("boom")
        assertTrue(checker.checkClearKeySupport() is DrmResult.CheckError)
    }

    @Test
    fun `checkClearKeySupport - supported returns Supported`() {
        every { MediaDrm.isCryptoSchemeSupported(C.CLEARKEY_UUID) } returns true
        assertTrue(checker.checkClearKeySupport() is DrmResult.Supported)
    }

    // ── PlayReady ─────────────────────────────────────────────────────────────

    @Test
    fun `checkPlayReadySupport - not supported returns NotSupported`() {
        every { MediaDrm.isCryptoSchemeSupported(C.PLAYREADY_UUID) } returns false
        assertTrue(checker.checkPlayReadySupport() is DrmResult.NotSupported)
    }

    @Test
    fun `checkPlayReadySupport - exception returns CheckError`() {
        every { MediaDrm.isCryptoSchemeSupported(C.PLAYREADY_UUID) } throws RuntimeException("boom")
        assertTrue(checker.checkPlayReadySupport() is DrmResult.CheckError)
    }

    @Test
    fun `checkPlayReadySupport - supported returns Supported`() {
        every { MediaDrm.isCryptoSchemeSupported(C.PLAYREADY_UUID) } returns true
        assertTrue(checker.checkPlayReadySupport() is DrmResult.Supported)
    }

    // ── Marlin ────────────────────────────────────────────────────────────────

    @Test
    fun `checkMarlinSupport - not supported returns NotSupported`() {
        every { MediaDrm.isCryptoSchemeSupported(DRMChecker.MARLIN_UUID) } returns false
        assertTrue(checker.checkMarlinSupport() is DrmResult.NotSupported)
    }

    @Test
    fun `checkMarlinSupport - exception returns CheckError`() {
        every { MediaDrm.isCryptoSchemeSupported(DRMChecker.MARLIN_UUID) } throws RuntimeException("boom")
        assertTrue(checker.checkMarlinSupport() is DrmResult.CheckError)
    }

    @Test
    fun `checkMarlinSupport - supported returns Supported`() {
        every { MediaDrm.isCryptoSchemeSupported(DRMChecker.MARLIN_UUID) } returns true
        assertTrue(checker.checkMarlinSupport() is DrmResult.Supported)
    }

    // ── PrimeTime ─────────────────────────────────────────────────────────────

    @Test
    fun `checkPrimeTimeSupport - not supported returns NotSupported`() {
        every { MediaDrm.isCryptoSchemeSupported(DRMChecker.PRIMETIME_UUID) } returns false
        assertTrue(checker.checkPrimeTimeSupport() is DrmResult.NotSupported)
    }

    @Test
    fun `checkPrimeTimeSupport - exception returns CheckError`() {
        every { MediaDrm.isCryptoSchemeSupported(DRMChecker.PRIMETIME_UUID) } throws RuntimeException("boom")
        assertTrue(checker.checkPrimeTimeSupport() is DrmResult.CheckError)
    }

    @Test
    fun `checkPrimeTimeSupport - supported returns Supported`() {
        every { MediaDrm.isCryptoSchemeSupported(DRMChecker.PRIMETIME_UUID) } returns true
        assertTrue(checker.checkPrimeTimeSupport() is DrmResult.Supported)
    }

    // ── Generic checkDrmSupport ───────────────────────────────────────────────

    @Test
    fun `checkDrmSupport - unsupported returns NotSupported with DRM name`() {
        val uuid = UUID.randomUUID()
        every { MediaDrm.isCryptoSchemeSupported(uuid) } returns false
        val result = checker.checkDrmSupport(uuid, "TestDRM")
        assertTrue(result is DrmResult.NotSupported)
        assertEquals("TestDRM is not supported on this device.", (result as DrmResult.NotSupported).reason)
    }

    @Test
    fun `checkDrmSupport - supported propagates additionalChecks result`() {
        val uuid = UUID.randomUUID()
        every { MediaDrm.isCryptoSchemeSupported(uuid) } returns true
        val result = checker.checkDrmSupport(uuid, "TestDRM") { "my details" }
        assertTrue(result is DrmResult.Supported)
        assertEquals("my details", (result as DrmResult.Supported).details)
    }
}
