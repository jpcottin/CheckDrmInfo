package com.example.checkdrminfo

import android.media.MediaDrm
import android.media.MediaDrmException
import android.util.Log
import androidx.media3.common.C
import com.example.checkdrminfo.model.DrmResult
import io.mockk.every
import io.mockk.verify
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
        // The details always spell out all three flags, so "L1" alone would
        // match any level; check the resolved level and the flag line.
        val details = (result as DrmResult.Supported).details
        assertTrue(details, details.contains("Security Level: L1 (L1=true L2=false L3=false)"))
        assertTrue(details, details.contains("HDCP Level: HW_SECURE_ALL"))
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
        val details = (result as DrmResult.Supported).details
        assertTrue(details, details.contains("Security Level: L3 (L1=false L2=false L3=true)"))
        assertTrue(details, details.contains("HDCP Level: NONE"))
    }

    @Test
    fun `checkWidevineSupport - property query failure is reported as Supported with the error`() {
        every { MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID) } returns true
        // What getPropertyString throws on a device: MediaDrm.MediaDrmStateException is an
        // IllegalStateException (MediaDrmException is reserved for opening the plugin).
        every { mockDrm.getPropertyString("securityLevel") } throws IllegalStateException("vendor plugin busy")

        val result = checker.checkWidevineSupport()
        // The scheme is supported even if its properties cannot be read right now.
        assertTrue(result is DrmResult.Supported)
        val details = (result as DrmResult.Supported).details
        assertTrue(details, details.startsWith("Error querying Widevine properties"))
        assertTrue(details, details.contains("vendor plugin busy"))
        verify(exactly = 1) { mockDrm.close() }
    }

    @Test
    fun `checkWidevineSupport - optional properties fall back to N-A`() {
        every { MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID) } returns true
        every { mockDrm.getPropertyString("securityLevel") } returns "L1"
        every { mockDrm.getPropertyString("hdcpLevel") }     throws MediaDrmException("no hdcp")
        every { mockDrm.getPropertyString("vendor") }        throws IllegalStateException("no vendor")
        every { mockDrm.getPropertyString("version") }       returns "16.0"

        val result = checker.checkWidevineSupport()
        assertTrue(result is DrmResult.Supported)
        val details = (result as DrmResult.Supported).details
        assertTrue(details, details.contains("Security Level: L1 (L1=true L2=false L3=false)"))
        assertTrue(details, details.contains("HDCP Level: N/A"))
        assertTrue(details, details.contains("Vendor: N/A"))
        assertTrue(details, details.contains("CDM Version: 16.0"))
    }

    // ── ClearKey ──────────────────────────────────────────────────────────────

    @Test
    fun `checkWidevineSupport - blank properties read N-A like a failed query`() {
        every { MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID) } returns true
        every { mockDrm.getPropertyString("securityLevel") } returns "L3"
        every { mockDrm.getPropertyString("hdcpLevel") }     returns ""
        every { mockDrm.getPropertyString("vendor") }        returns "  "
        every { mockDrm.getPropertyString("version") }       returns ""

        val details = (checker.checkWidevineSupport() as DrmResult.Supported).details
        assertTrue(details, details.contains("HDCP Level: N/A"))
        assertTrue(details, details.contains("Vendor: N/A"))
        assertTrue(details, details.contains("CDM Version: N/A"))
    }

    @Test
    fun `checkWidevineSupport - the maximum HDCP level is reported next to the current one`() {
        every { MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID) } returns true
        every { mockDrm.getPropertyString("securityLevel") } returns "L1"
        every { mockDrm.getPropertyString("hdcpLevel") }     returns "Unprotected"
        every { mockDrm.getPropertyString("maxHdcpLevel") }  returns "HDCP-2.2"

        val details = (checker.checkWidevineSupport() as DrmResult.Supported).details
        assertTrue(details, details.contains("HDCP Level: Unprotected (max HDCP-2.2)"))
    }

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

    // ── Handle lifecycle ──────────────────────────────────────────────────────

    @Test
    fun `an unsupported scheme never opens a MediaDrm`() {
        var opened = 0
        val counting = DRMChecker(mediaDrmFactory = { opened++; mockDrm })
        every { MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID) } returns false

        assertTrue(counting.checkWidevineSupport() is DrmResult.NotSupported)
        assertEquals(0, opened)
    }

    @Test
    fun `a failing MediaDrm constructor is a CheckError naming the scheme`() {
        val failing = DRMChecker(mediaDrmFactory = { throw RuntimeException("no plugin") })
        every { MediaDrm.isCryptoSchemeSupported(C.PLAYREADY_UUID) } returns true

        val result = failing.checkPlayReadySupport()
        assertTrue(result is DrmResult.CheckError)
        val message = (result as DrmResult.CheckError).message
        assertTrue(message, message.startsWith("Error checking PlayReady"))
        assertTrue(message, message.contains("no plugin"))
    }

    @Test
    fun `a crash while reading properties keeps the scheme Supported and still releases the handle`() {
        every { MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID) } returns true
        // Any failure inside the property queries: the plugin opened, so the scheme is supported.
        every { mockDrm.getPropertyString("securityLevel") } throws RuntimeException("dead service")

        val result = checker.checkWidevineSupport()
        assertTrue(result is DrmResult.Supported)
        val details = (result as DrmResult.Supported).details
        assertTrue(details, details.startsWith("Error querying Widevine properties"))
        assertTrue(details, details.contains("dead service"))
        verify(exactly = 1) { mockDrm.close() }
    }

    @Test
    fun `an exception without a message is named by its class, never as null`() {
        val failing = DRMChecker(mediaDrmFactory = { throw IllegalStateException() })
        every { MediaDrm.isCryptoSchemeSupported(DRMChecker.MARLIN_UUID) } returns true

        val result = failing.checkMarlinSupport()
        assertTrue(result is DrmResult.CheckError)
        val message = (result as DrmResult.CheckError).message
        assertEquals("Error checking Marlin: IllegalStateException", message)
    }
}
