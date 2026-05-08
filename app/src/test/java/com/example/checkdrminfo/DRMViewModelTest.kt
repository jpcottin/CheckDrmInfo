package com.example.checkdrminfo

import com.example.checkdrminfo.model.DrmResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DRMViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockChecker: DRMChecker
    private lateinit var viewModel: DRMViewModel

    @Before
    fun setUp() {
        mockChecker = mockk()
        every { mockChecker.checkWidevineSupport() }  returns DrmResult.Supported("L1\nHDCP: HW_SECURE_ALL")
        every { mockChecker.checkClearKeySupport() }   returns DrmResult.Supported("ClearKey CDM present")
        every { mockChecker.checkPlayReadySupport() }  returns DrmResult.NotSupported("Not supported")
        every { mockChecker.checkMarlinSupport() }     returns DrmResult.NotSupported("Not supported")
        every { mockChecker.checkPrimeTimeSupport() }  returns DrmResult.CheckError("Init failed")
        viewModel = DRMViewModel(mockChecker, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `initial load produces five DRM entries`() = runTest {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(5, state.drmEntries.size)
    }

    @Test
    fun `widevine entry is Supported`() = runTest {
        val entry = viewModel.uiState.value.drmEntries.first { it.name == "Widevine" }
        assertTrue(entry.result is DrmResult.Supported)
    }

    @Test
    fun `clearKey entry is Supported`() = runTest {
        val entry = viewModel.uiState.value.drmEntries.first { it.name == "ClearKey" }
        assertTrue(entry.result is DrmResult.Supported)
    }

    @Test
    fun `playReady entry is NotSupported`() = runTest {
        val entry = viewModel.uiState.value.drmEntries.first { it.name == "PlayReady" }
        assertTrue(entry.result is DrmResult.NotSupported)
    }

    @Test
    fun `marlin entry is NotSupported`() = runTest {
        val entry = viewModel.uiState.value.drmEntries.first { it.name == "Marlin" }
        assertTrue(entry.result is DrmResult.NotSupported)
    }

    @Test
    fun `primeTime entry is CheckError`() = runTest {
        val entry = viewModel.uiState.value.drmEntries.first { it.name == "PrimeTime" }
        assertTrue(entry.result is DrmResult.CheckError)
    }

    @Test
    fun `loadDrmInfo re-runs checks and updates state`() = runTest {
        viewModel.loadDrmInfo()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(5, state.drmEntries.size)
    }

    @Test
    fun `shareable report contains all DRM names`() {
        val report = viewModel.getShareableReport()
        assertTrue(report.contains("DRM Check Report"))
        assertTrue(report.contains("Widevine"))
        assertTrue(report.contains("ClearKey"))
        assertTrue(report.contains("PlayReady"))
        assertTrue(report.contains("Marlin"))
        assertTrue(report.contains("PrimeTime"))
    }

    @Test
    fun `shareable report marks supported DRM with plus sign`() {
        val report = viewModel.getShareableReport()
        assertTrue(report.contains("[+] Widevine"))
        assertTrue(report.contains("[+] ClearKey"))
    }

    @Test
    fun `shareable report marks not-supported DRM with minus sign`() {
        val report = viewModel.getShareableReport()
        assertTrue(report.contains("[-] PlayReady"))
        assertTrue(report.contains("[-] Marlin"))
    }

    @Test
    fun `shareable report marks error DRM with exclamation`() {
        val report = viewModel.getShareableReport()
        assertTrue(report.contains("[!] PrimeTime"))
    }
}
