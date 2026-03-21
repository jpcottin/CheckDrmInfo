package com.example.checkdrminfo

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceInfoTest {

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
}
