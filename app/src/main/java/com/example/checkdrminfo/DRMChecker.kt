package com.example.checkdrminfo

import android.media.MediaDrm
import android.media.MediaDrmException
import android.util.Log
import androidx.media3.common.C
import com.example.checkdrminfo.model.DrmResult
import java.util.UUID

class DRMChecker(
    private val mediaDrmFactory: (UUID) -> MediaDrm = ::MediaDrm,
) {

    companion object {
        private const val TAG = "DRMChecker"

        val WIDEVINE_UUID: UUID = C.WIDEVINE_UUID
        val CLEARKEY_UUID: UUID = C.CLEARKEY_UUID
        val PLAYREADY_UUID: UUID = C.PLAYREADY_UUID

        // Marlin: used by Sony/Amazon on select devices
        val MARLIN_UUID: UUID = UUID.fromString("5E629AF5-38DA-4063-8977-97FFBD9902D4")

        // Adobe PrimeTime: used in US broadcast streaming
        val PRIMETIME_UUID: UUID = UUID.fromString("F239E769-EFA3-4850-9C16-A903C6932EFB")
    }

    fun checkWidevineSupport(): DrmResult = checkDrmSupport(WIDEVINE_UUID, "Widevine") { drm ->
        val level = drm.getPropertyString("securityLevel")
        val hdcp = runCatching { drm.getPropertyString("hdcpLevel") }.getOrDefault("N/A")
        val vendor = runCatching { drm.getPropertyString("vendor") }.getOrDefault("N/A")
        val version = runCatching { drm.getPropertyString("version") }.getOrDefault("N/A")
        buildString {
            appendLine("Security Level: $level (L1=${level.equals("L1", true)} L2=${level.equals("L2", true)} L3=${level.equals("L3", true)})")
            appendLine("HDCP Level: $hdcp")
            appendLine("Vendor: $vendor")
            append("CDM Version: $version")
        }
    }

    fun checkClearKeySupport(): DrmResult = checkDrmSupport(CLEARKEY_UUID, "ClearKey") {
        "ClearKey CDM present"
    }

    fun checkPlayReadySupport(): DrmResult = checkDrmSupport(PLAYREADY_UUID, "PlayReady") {
        "PlayReady CDM present"
    }

    fun checkMarlinSupport(): DrmResult = checkDrmSupport(MARLIN_UUID, "Marlin") {
        "Marlin CDM present"
    }

    fun checkPrimeTimeSupport(): DrmResult = checkDrmSupport(PRIMETIME_UUID, "PrimeTime") {
        "Adobe PrimeTime CDM present"
    }

    fun checkDrmSupport(
        uuid: UUID,
        drmName: String,
        additionalChecks: (MediaDrm) -> String = { "" },
    ): DrmResult = try {
        if (!MediaDrm.isCryptoSchemeSupported(uuid)) {
            DrmResult.NotSupported("$drmName is not supported on this device.")
        } else {
            mediaDrmFactory(uuid).use { drm ->
                val details = try {
                    additionalChecks(drm)
                } catch (e: MediaDrmException) {
                    "Error querying $drmName properties: ${e.message}"
                }
                DrmResult.Supported(details)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error checking $drmName support", e)
        DrmResult.CheckError("Error checking $drmName: ${e.message}")
    }
}
