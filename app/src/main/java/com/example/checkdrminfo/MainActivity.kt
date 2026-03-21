package com.example.checkdrminfo

import android.media.MediaDrm
import android.media.MediaDrmException
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import com.example.checkdrminfo.ui.theme.CheckDrmInfoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CheckDrmInfoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DRMInfoScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

data class DRMInfoState(
    val widevineInfo: Pair<Boolean, String>? = null,
    val clearKeyInfo: Pair<Boolean, String>? = null,
    val playReadyInfo: Pair<Boolean, String>? = null,
    val deviceInfo: DeviceInfo = DeviceInfo()
)

data class DeviceInfo(
    val model: String = try { Build.MODEL } catch (_: Exception) { "Unknown" },
    val manufacturer: String = try { Build.MANUFACTURER } catch (_: Exception) { "Unknown" },
    val androidVersion: String = try { Build.VERSION.RELEASE } catch (_: Exception) { "Unknown" },
    val sdkInt: Int = try { Build.VERSION.SDK_INT } catch (_: Exception) { 0 },
    val fingerprint: String = try { Build.FINGERPRINT } catch (_: Exception) { "Unknown" }
)

class DRMViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DRMInfoState())
    val uiState: StateFlow<DRMInfoState> = _uiState.asStateFlow()

    private val drmChecker = DRMChecker()

    init {
        refreshDRMInfo()
    }

    fun refreshDRMInfo() {
        _uiState.value = _uiState.value.copy(
            widevineInfo = drmChecker.checkWidevineSupport(),
            clearKeyInfo = drmChecker.checkClearKeySupport(),
            playReadyInfo = drmChecker.checkPlayReadySupport()
        )
    }
}

@Composable
fun DRMInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: DRMViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    DRMInfoContent(
        modifier = modifier,
        widevineInfo = uiState.widevineInfo,
        clearKeyInfo = uiState.clearKeyInfo,
        playReadyInfo = uiState.playReadyInfo,
        deviceInfo = uiState.deviceInfo
    )
}

@Composable
fun DRMInfoContent(
    modifier: Modifier = Modifier,
    widevineInfo: Pair<Boolean, String>?,
    clearKeyInfo: Pair<Boolean, String>?,
    playReadyInfo: Pair<Boolean, String>?,
    deviceInfo: DeviceInfo = DeviceInfo()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp))
                append("DRM Check\n")
                pop()
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        DeviceInfoSection(deviceInfo)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "DRM Support",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        DRMCheckItem(
            drmName = "Widevine",
            isSupported = widevineInfo?.first,
            details = widevineInfo?.second
        )
        DRMCheckItem(
            drmName = "ClearKey",
            isSupported = clearKeyInfo?.first,
            details = clearKeyInfo?.second
        )
        DRMCheckItem(
            drmName = "PlayReady",
            isSupported = playReadyInfo?.first,
            details = playReadyInfo?.second
        )
    }
}

@Composable
fun DeviceInfoSection(deviceInfo: DeviceInfo) {
    Column {
        Text(
            text = "Device Info",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        DeviceInfoItem("Manufacturer", deviceInfo.manufacturer)
        DeviceInfoItem("Model", deviceInfo.model)
        DeviceInfoItem("Android Version", deviceInfo.androidVersion)
        DeviceInfoItem("SDK Level", deviceInfo.sdkInt.toString())
        DeviceInfoItem("Fingerprint", deviceInfo.fingerprint)
    }
}

@Composable
fun DeviceInfoItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun DRMCheckItem(drmName: String, isSupported: Boolean?, details: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        if (isSupported != null) {
            Image(
                painter = painterResource(id = if (isSupported) R.drawable.ic_check_circle else R.drawable.ic_close_circle),
                contentDescription = if (isSupported) "Supported" else "Not Supported",
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(if (isSupported) Color.Green else Color.Red)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_close_circle),
                contentDescription = "Not Supported",
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(Color.Red)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = drmName,
                style = MaterialTheme.typography.titleMedium
            )
            if (details != null) {
                Text(text = details, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    HorizontalDivider()
}

class DRMChecker {

    companion object {
        private const val TAG = "DRMChecker"
        private val WIDEVINE_UUID = C.WIDEVINE_UUID
        private val CLEARKEY_UUID = C.CLEARKEY_UUID
        private val PLAYREADY_UUID = C.PLAYREADY_UUID
    }

    fun checkWidevineSupport(): Pair<Boolean, String> {
        val result = checkDrmSupport(WIDEVINE_UUID, "Widevine") { mediaDrm ->
            val securityLevel = mediaDrm.getPropertyString("securityLevel")
            val widevineL1 = securityLevel.equals("L1", ignoreCase = true)
            val widevineL2 = securityLevel.equals("L2", ignoreCase = true)
            val widevineL3 = securityLevel.equals("L3", ignoreCase = true)
            val hdcpLevel = try {
                mediaDrm.getPropertyString("hdcpLevel") // Get HDCP Level
            } catch (e: MediaDrmException) {
                "HDCP Level: Not Available (${e.message})" // Handle potential exceptions
            }


            val details = buildString {
                appendLine("Security Level: $securityLevel")
                appendLine("Widevine L1 support: $widevineL1")
                appendLine("Widevine L2 support: $widevineL2")
                appendLine("Widevine L3 support: $widevineL3")
                appendLine("HDCP Level: $hdcpLevel")

            }
            details
        }
        Log.d(TAG, "Widevine check result: $result")
        return result
    }

    fun checkClearKeySupport(): Pair<Boolean, String> {
        val result =  checkDrmSupport(CLEARKEY_UUID, "ClearKey") {
            "ClearKey CDM is present."
        }
        Log.d(TAG, "ClearKey check result: $result")
        return result
    }

    fun checkPlayReadySupport(): Pair<Boolean, String> {
        val result = checkDrmSupport(PLAYREADY_UUID, "PlayReady") {
            "PlayReady support check completed."
        }
        Log.d(TAG, "PlayReady check result: $result")
        return result
    }

    fun checkDrmSupport(
        uuid: UUID,
        drmName: String,
        additionalChecks: (MediaDrm) -> String = { "" }
    ): Pair<Boolean, String> {
        return try {
            if (MediaDrm.isCryptoSchemeSupported(uuid)) {
                val mediaDrm = MediaDrm(uuid)
                val details = try {
                    additionalChecks(mediaDrm)
                } catch (e: MediaDrmException) {
                    "Error with $drmName MediaDrm: ${e.message}"
                } finally {
                    mediaDrm.close()
                }
                Pair(true, details)
            } else {
                Pair(false, "$drmName is NOT supported on this device.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking $drmName support", e)
            Pair(false, "Error checking $drmName support: ${e.message}")
        }
    }
}

// Preview Parameter Provider for DRMCheckItem
class DRMCheckItemPreviewParameterProvider : PreviewParameterProvider<Triple<String, Boolean?, String?>> {
    override val values: Sequence<Triple<String, Boolean?, String?>> = sequenceOf(
        Triple("Widevine", true, "Security Level: L1\nWidevine L1 support: true\nWidevine L2 support: false\nWidevine L3 support: false"),
        Triple("Widevine", false, "Widevine is NOT supported on this device."),
        Triple("Widevine", null, null), // Example for null state
        Triple("ClearKey", true, "ClearKey CDM is present."),
        Triple("ClearKey", false, "ClearKey is NOT supported on this device."),
        Triple("PlayReady", true, "PlayReady support check completed."),
        Triple("PlayReady", false, "PlayReady is NOT supported on this device.")
    )
}

@Preview(showBackground = true)
@Composable
fun DeviceInfoSectionPreview() {
    CheckDrmInfoTheme {
        DeviceInfoSection(
            deviceInfo = DeviceInfo(
                model = "Pixel 7",
                manufacturer = "Google",
                androidVersion = "14",
                sdkInt = 34,
                fingerprint = "google/cheetah/cheetah:14/UP1A.231005.007/10754064:user/release-keys"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DRMCheckItemPreview(@PreviewParameter(DRMCheckItemPreviewParameterProvider::class) params: Triple<String, Boolean?, String?>) {
    CheckDrmInfoTheme {
        DRMCheckItem(drmName = params.first, isSupported = params.second, details = params.third)
    }
}

@Preview(showBackground = true)
@Composable
fun DRMInfoScreenPreview() {
    CheckDrmInfoTheme {
        DRMInfoContent(
            modifier = Modifier,
            widevineInfo = Pair(true, "Security Level: L1\nWidevine L1 support: true\nWidevine L2 support: false\nWidevine L3 support: false"),
            clearKeyInfo = Pair(true, "ClearKey CDM is present."),
            playReadyInfo = Pair(false, "PlayReady is NOT supported on this device."),
            deviceInfo = DeviceInfo("Pixel 7", "Google", "14", 34, "google/cheetah/cheetah:14/UP1A.231005.007/10754064:user/release-keys")
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DRMInfoContentPreview() {
    CheckDrmInfoTheme {
        DRMInfoContent(
            modifier = Modifier,
            widevineInfo = Pair(true, "Security Level: L1\nWidevine L1 support: true\nWidevine L2 support: false\nWidevine L3 support: false"),
            clearKeyInfo = Pair(true, "ClearKey CDM is present."),
            playReadyInfo = Pair(false, "PlayReady is NOT supported on this device."),
            deviceInfo = DeviceInfo("Pixel 7", "Google", "14", 34, "google/cheetah/cheetah:14/UP1A.231005.007/10754064:user/release-keys")
        )
    }
}