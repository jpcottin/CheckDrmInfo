package com.example.checkdrminfo.model

import android.os.Build

sealed class DrmResult {
    data object Loading : DrmResult()
    data class Supported(val details: String) : DrmResult()
    data class NotSupported(val reason: String) : DrmResult()
    data class CheckError(val message: String) : DrmResult()
}

data class DrmEntry(
    val name: String,
    val result: DrmResult = DrmResult.Loading,
)

data class DeviceInfo(
    val model: String = try { Build.MODEL } catch (_: Exception) { "Unknown" },
    val manufacturer: String = try { Build.MANUFACTURER } catch (_: Exception) { "Unknown" },
    val androidVersion: String = try { Build.VERSION.RELEASE } catch (_: Exception) { "Unknown" },
    val sdkInt: Int = try { Build.VERSION.SDK_INT } catch (_: Exception) { 0 },
    val fingerprint: String = try { Build.FINGERPRINT } catch (_: Exception) { "Unknown" },
)

data class DrmInfoState(
    val drmEntries: List<DrmEntry> = emptyList(),
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val isLoading: Boolean = true,
)
