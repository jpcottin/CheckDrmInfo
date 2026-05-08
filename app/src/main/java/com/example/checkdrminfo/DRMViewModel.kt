package com.example.checkdrminfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.checkdrminfo.model.DeviceInfo
import com.example.checkdrminfo.model.DrmEntry
import com.example.checkdrminfo.model.DrmInfoState
import com.example.checkdrminfo.model.DrmResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DRMViewModel(
    private val drmChecker: DRMChecker = DRMChecker(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrmInfoState())
    val uiState: StateFlow<DrmInfoState> = _uiState.asStateFlow()

    private val drmChecks: List<Pair<String, () -> DrmResult>> = listOf(
        "Widevine" to drmChecker::checkWidevineSupport,
        "ClearKey" to drmChecker::checkClearKeySupport,
        "PlayReady" to drmChecker::checkPlayReadySupport,
        "Marlin" to drmChecker::checkMarlinSupport,
        "PrimeTime" to drmChecker::checkPrimeTimeSupport,
    )

    init {
        loadDrmInfo()
    }

    fun loadDrmInfo() {
        _uiState.value = DrmInfoState(
            drmEntries = drmChecks.map { (name, _) -> DrmEntry(name, DrmResult.Loading) },
            deviceInfo = DeviceInfo(),
            isLoading = true,
        )
        viewModelScope.launch(ioDispatcher) {
            val results = drmChecks
                .map { (name, check) -> async { DrmEntry(name, check()) } }
                .map { it.await() }

            _uiState.value = DrmInfoState(
                drmEntries = results,
                deviceInfo = DeviceInfo(),
                isLoading = false,
            )
        }
    }

    fun getShareableReport(): String {
        val state = _uiState.value
        return buildString {
            appendLine("=== DRM Check Report ===")
            appendLine()
            appendLine("Device:")
            appendLine("  ${state.deviceInfo.manufacturer} ${state.deviceInfo.model}")
            appendLine("  Android ${state.deviceInfo.androidVersion} (SDK ${state.deviceInfo.sdkInt})")
            appendLine("  ${state.deviceInfo.fingerprint}")
            appendLine()
            appendLine("DRM Support:")
            state.drmEntries.forEach { entry ->
                when (val r = entry.result) {
                    is DrmResult.Supported -> {
                        appendLine("  [+] ${entry.name}: Supported")
                        if (r.details.isNotBlank()) {
                            r.details.trim().lines().forEach { appendLine("      $it") }
                        }
                    }
                    is DrmResult.NotSupported -> appendLine("  [-] ${entry.name}: ${r.reason}")
                    is DrmResult.CheckError   -> appendLine("  [!] ${entry.name}: ${r.message}")
                    DrmResult.Loading         -> appendLine("  [?] ${entry.name}: checking…")
                }
            }
        }
    }
}
