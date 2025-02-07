package com.example.checkdrminfo

import android.media.MediaDrm
import android.media.MediaDrmException
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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.media3.common.C
import com.example.checkdrminfo.ui.theme.CheckDrmInfoTheme
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

@Composable
fun DRMInfoScreen(modifier: Modifier = Modifier) {
    var widevineInfo by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var clearKeyInfo by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var playReadyInfo by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val drmChecker = remember { DRMChecker() }
    widevineInfo = drmChecker.checkWidevineSupport()
    clearKeyInfo = drmChecker.checkClearKeySupport()
    playReadyInfo = drmChecker.checkPlayReadySupport()

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
    Divider()
}

class DRMChecker {

    private val TAG = "DRMChecker"
    private val WIDEVINE_UUID = C.WIDEVINE_UUID
    private val CLEARKEY_UUID = C.CLEARKEY_UUID
    private val PLAYREADY_UUID = C.PLAYREADY_UUID

    fun checkWidevineSupport(): Pair<Boolean, String> {
        val result = checkDrmSupport(WIDEVINE_UUID, "Widevine") { mediaDrm ->
            val securityLevel = mediaDrm.getPropertyString("securityLevel")
            val widevineL1 = securityLevel.equals("L1", true)
            val widevineL2 = securityLevel.equals("L2", true)
            val widevineL3 = securityLevel.equals("L3", true)

            val details = buildString {
                appendLine("Security Level: $securityLevel")
                appendLine("Widevine L1 support: $widevineL1")
                appendLine("Widevine L2 support: $widevineL2")
                appendLine("Widevine L3 support: $widevineL3")
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
fun DRMCheckItemPreview(@PreviewParameter(DRMCheckItemPreviewParameterProvider::class) params: Triple<String, Boolean?, String?>) {
    CheckDrmInfoTheme {
        DRMCheckItem(drmName = params.first, isSupported = params.second, details = params.third)
    }
}

// Preview Parameter Provider for the complete screen
class DRMInfoScreenPreviewParameterProvider : PreviewParameterProvider<Unit> {
    override val values: Sequence<Unit> = sequenceOf(Unit) // You can add more configurations if needed

}
@Preview(showBackground = true)
@Composable
fun DRMInfoScreenPreview() {
    CheckDrmInfoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        )
        {
            Text(
                text = buildAnnotatedString {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp))
                    append("DRM Check\n")
                    pop()
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            DRMCheckItem(
                drmName = "Widevine",
                isSupported = true,
                details = "Security Level: L1\nWidevine L1 support: true\nWidevine L2 support: false\nWidevine L3 support: false"
            )
            DRMCheckItem(
                drmName = "ClearKey",
                isSupported = true,
                details = "ClearKey CDM is present."
            )
            DRMCheckItem(
                drmName = "PlayReady",
                isSupported = false,
                details = "PlayReady is NOT supported on this device."
            )
        }
    }

}