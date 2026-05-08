package com.example.checkdrminfo

import android.util.Log
import com.example.checkdrminfo.model.DeviceInfo
import com.example.checkdrminfo.model.DrmInfoState
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceInfoTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `DeviceInfo stores provided values correctly`() {
        val info = DeviceInfo(
            model = "TestModel",
            manufacturer = "TestManufacturer",
            androidVersion = "12",
            sdkInt = 31,
            fingerprint = "test/fingerprint",
        )
        assertEquals("TestModel", info.model)
        assertEquals("TestManufacturer", info.manufacturer)
        assertEquals("12", info.androidVersion)
        assertEquals(31, info.sdkInt)
        assertEquals("test/fingerprint", info.fingerprint)
    }

    @Test
    fun `DrmInfoState starts with empty entries and loading true`() {
        val state = DrmInfoState()
        assertTrue(state.drmEntries.isEmpty())
        assertTrue(state.isLoading)
    }

    @Test
    fun `DrmInfoState with custom entries stores them`() {
        val state = DrmInfoState(
            drmEntries = emptyList(),
            deviceInfo = DeviceInfo(model = "Pixel", manufacturer = "Google", androidVersion = "14", sdkInt = 34, fingerprint = "fp"),
            isLoading = false,
        )
        assertFalse(state.isLoading)
        assertEquals("Pixel", state.deviceInfo.model)
    }
}

private fun assertFalse(value: Boolean) = assertEquals(false, value)
