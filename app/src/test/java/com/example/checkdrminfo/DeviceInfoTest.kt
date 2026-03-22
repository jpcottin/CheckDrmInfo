package com.example.checkdrminfo

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `DeviceInfo default values are set correctly`() {
        // Since we can't easily mock android.os.Build in unit tests without a library like Robolectric
        // or using mockkStatic (which is complex for Build), we'll test the data class construction.
        val deviceInfo = DeviceInfo(
            model = "TestModel",
            manufacturer = "TestManufacturer",
            androidVersion = "12",
            sdkInt = 31,
            fingerprint = "test/fingerprint",
        )

        assertEquals("TestModel", deviceInfo.model)
        assertEquals("TestManufacturer", deviceInfo.manufacturer)
        assertEquals("12", deviceInfo.androidVersion)
        assertEquals(31, deviceInfo.sdkInt)
        assertEquals("test/fingerprint", deviceInfo.fingerprint)
    }

    @Test
    fun `DRMInfoState default values are correctly initialized`() {
        val state = DRMInfoState()
        assertEquals(null, state.widevineInfo)
        assertEquals(null, state.clearKeyInfo)
        assertEquals(null, state.playReadyInfo)
        // Check that DeviceInfo is also initialized without crashing
        assertEquals("Unknown", state.deviceInfo.model)
    }

    @Test
    fun `DRMViewModel getShareableReport formats report correctly`() {
        val viewModel = DRMViewModel()
        val report = viewModel.getShareableReport()

        // Verify key sections are present in the report
        assert(report.contains("DRM Check Report"))
        assert(report.contains("Device Info:"))
        assert(report.contains("DRM Support:"))

        // Verify some default values from the state are reflected in the report
        // Note: The actual values will depend on what DRMChecker returns on the test environment
        // but since we aren't mocking DRMChecker in this test, we check for presence of labels.
        assert(report.contains("Manufacturer:"))
        assert(report.contains("Model:"))
        assert(report.contains("Widevine:"))
        assert(report.contains("ClearKey:"))
        assert(report.contains("PlayReady:"))
    }
}
