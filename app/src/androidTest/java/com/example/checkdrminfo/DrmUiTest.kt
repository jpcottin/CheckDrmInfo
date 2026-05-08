package com.example.checkdrminfo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import com.example.checkdrminfo.model.DeviceInfo
import com.example.checkdrminfo.model.DrmEntry
import com.example.checkdrminfo.model.DrmInfoState
import com.example.checkdrminfo.model.DrmResult
import com.example.checkdrminfo.ui.DeviceInfoSection
import com.example.checkdrminfo.ui.DrmCheckItem
import com.example.checkdrminfo.ui.DrmInfoContent
import com.example.checkdrminfo.ui.theme.CheckDrmInfoTheme
import org.junit.Rule
import org.junit.Test

class DrmUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeDevice = DeviceInfo(
        model = "Pixel 7",
        manufacturer = "Google",
        androidVersion = "14",
        sdkInt = 34,
        fingerprint = "google/test/test:14/finger",
    )

    private val fakeState = DrmInfoState(
        drmEntries = listOf(
            DrmEntry("Widevine",  DrmResult.Supported("Security Level: L1")),
            DrmEntry("ClearKey",  DrmResult.Supported("ClearKey CDM present")),
            DrmEntry("PlayReady", DrmResult.NotSupported("Not supported")),
            DrmEntry("Marlin",    DrmResult.NotSupported("Not supported")),
            DrmEntry("PrimeTime", DrmResult.CheckError("Init error")),
        ),
        deviceInfo = fakeDevice,
        isLoading = false,
    )

    // ── DrmInfoContent ────────────────────────────────────────────────────────

    @Test
    fun drmInfoContent_singlePane_showsAllDrmNames() {
        composeTestRule.setContent {
            CheckDrmInfoTheme { DrmInfoContent(state = fakeState, useTwoPane = false) }
        }
        listOf("Widevine", "ClearKey", "PlayReady", "Marlin", "PrimeTime").forEach { name ->
            composeTestRule.onNodeWithText(name).assertIsDisplayed()
        }
    }

    @Test
    fun drmInfoContent_singlePane_showsDeviceInfo() {
        composeTestRule.setContent {
            CheckDrmInfoTheme { DrmInfoContent(state = fakeState, useTwoPane = false) }
        }
        composeTestRule.onNodeWithText("Device Info").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Google", substring = true).onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Pixel 7", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun drmInfoContent_twoPane_showsBothSections() {
        composeTestRule.setContent {
            CheckDrmInfoTheme { DrmInfoContent(state = fakeState, useTwoPane = true) }
        }
        composeTestRule.onNodeWithText("Device Info").assertIsDisplayed()
        composeTestRule.onNodeWithText("DRM Support").assertIsDisplayed()
    }

    @Test
    fun drmInfoContent_loadingState_showsLoadingItems() {
        val loadingState = DrmInfoState(
            drmEntries = listOf(
                DrmEntry("Widevine", DrmResult.Loading),
                DrmEntry("ClearKey", DrmResult.Loading),
            ),
            deviceInfo = fakeDevice,
            isLoading = true,
        )
        composeTestRule.setContent {
            CheckDrmInfoTheme { DrmInfoContent(state = loadingState, useTwoPane = false) }
        }
        composeTestRule.onNodeWithText("Widevine").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Checking…").onFirst().assertIsDisplayed()
    }

    // ── DrmCheckItem ──────────────────────────────────────────────────────────

    @Test
    fun drmCheckItem_supported_showsNameAndSupportedIcon() {
        composeTestRule.setContent {
            CheckDrmInfoTheme {
                DrmCheckItem(DrmEntry("Widevine", DrmResult.Supported("Security Level: L1")))
            }
        }
        composeTestRule.onNodeWithText("Widevine").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Supported").assertIsDisplayed()
        composeTestRule.onNodeWithText("Security Level: L1").assertIsDisplayed()
    }

    @Test
    fun drmCheckItem_notSupported_showsNotSupportedIcon() {
        composeTestRule.setContent {
            CheckDrmInfoTheme {
                DrmCheckItem(DrmEntry("PlayReady", DrmResult.NotSupported("Not supported")))
            }
        }
        composeTestRule.onNodeWithContentDescription("Not Supported").assertIsDisplayed()
        composeTestRule.onNodeWithText("Not supported").assertIsDisplayed()
    }

    @Test
    fun drmCheckItem_checkError_showsErrorIcon() {
        composeTestRule.setContent {
            CheckDrmInfoTheme {
                DrmCheckItem(DrmEntry("PrimeTime", DrmResult.CheckError("Init error")))
            }
        }
        composeTestRule.onNodeWithContentDescription("Check Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Init error").assertIsDisplayed()
    }

    @Test
    fun drmCheckItem_loading_showsCheckingText() {
        composeTestRule.setContent {
            CheckDrmInfoTheme {
                DrmCheckItem(DrmEntry("Widevine", DrmResult.Loading))
            }
        }
        composeTestRule.onNodeWithText("Widevine").assertIsDisplayed()
        composeTestRule.onNodeWithText("Checking…").assertIsDisplayed()
    }

    // ── DeviceInfoSection ─────────────────────────────────────────────────────

    @Test
    fun deviceInfoSection_showsAllFields() {
        composeTestRule.setContent {
            CheckDrmInfoTheme { DeviceInfoSection(fakeDevice) }
        }
        composeTestRule.onNodeWithText("Device Info").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manufacturer: Google").assertIsDisplayed()
        composeTestRule.onNodeWithText("Model: Pixel 7").assertIsDisplayed()
        composeTestRule.onNodeWithText("Android: 14 (SDK 34)").assertIsDisplayed()
    }
}
