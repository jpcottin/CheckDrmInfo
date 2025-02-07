package com.example.checkdrminfo

import android.media.MediaDrm
import android.util.Log
import androidx.media3.common.C
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID
import io.mockk.OfTypeMatcher

class DRMCheckerTest {

    private lateinit var drmChecker: DRMChecker

    @Before
    fun setUp() {
        drmChecker = DRMChecker()
        mockkStatic(MediaDrm::class) // Mock MediaDrm for the entire test class
        mockkStatic(Log::class)       // Mock Log for the entire test class
        mockkConstructor(MediaDrm::class) // Mock the MediaDrm constructor

        every { Log.d(any(), any()) } returns 0 // Mock Log.d
        every { Log.e(any(), any(), any()) } returns 0 // Mock Log.e
    }

    @After
    fun tearDown() {
        unmockkAll() // Unmock everything after each test
    }


    @Test
    fun `checkWidevineSupport when not supported returns correct result`() {
        every { MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID) } returns false

        val result = drmChecker.checkWidevineSupport()
        assertEquals(Pair(false, "Widevine is NOT supported on this device."), result)
    }

    @Test
    fun `checkWidevineSupport when exception occurs returns correct result`() {
        every { MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID) } throws RuntimeException("Test Exception")

        val result = drmChecker.checkWidevineSupport()
        assertEquals(Pair(false, "Error checking Widevine support: Test Exception"), result)
    }



    @Test
    fun `checkClearKeySupport when not supported returns correct result`() {
        every { MediaDrm.isCryptoSchemeSupported(C.CLEARKEY_UUID) } returns false

        val result = drmChecker.checkClearKeySupport()
        assertEquals(Pair(false, "ClearKey is NOT supported on this device."), result)
    }
    @Test
    fun `checkClearKeySupport when exception is throw returns correct result`() {
        every { MediaDrm.isCryptoSchemeSupported(C.CLEARKEY_UUID) } throws RuntimeException("Test Exception")

        val result = drmChecker.checkClearKeySupport()
        assertEquals(Pair(false, "Error checking ClearKey support: Test Exception"), result)
    }



    @Test
    fun `checkPlayReadySupport when not supported returns correct result`() {
        every { MediaDrm.isCryptoSchemeSupported(C.PLAYREADY_UUID) } returns false

        val result = drmChecker.checkPlayReadySupport()
        assertEquals(Pair(false, "PlayReady is NOT supported on this device."), result)
    }

    @Test
    fun `checkPlayReadySupport when MediaDrm throws exception returns correct result`() {
        every { MediaDrm.isCryptoSchemeSupported(C.PLAYREADY_UUID) } throws RuntimeException("Test Exception") // Simulate an error

        val result = drmChecker.checkPlayReadySupport()
        assertEquals(Pair(false, "Error checking PlayReady support: Test Exception"), result)
    }

    @Test
    fun `checkDrmSupport generic test for unsupported DRM`() {
        val testUUID = UUID.randomUUID() // Use a random UUID
        every { MediaDrm.isCryptoSchemeSupported(testUUID) } returns false

        val result = drmChecker.checkDrmSupport(testUUID, "TestDRM")
        assertEquals(Pair(false, "TestDRM is NOT supported on this device."), result)
    }


}