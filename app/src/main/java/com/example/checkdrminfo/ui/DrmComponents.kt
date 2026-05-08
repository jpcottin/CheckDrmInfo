package com.example.checkdrminfo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.example.checkdrminfo.R
import com.example.checkdrminfo.model.DeviceInfo
import com.example.checkdrminfo.model.DrmEntry
import com.example.checkdrminfo.model.DrmResult
import com.example.checkdrminfo.ui.theme.CheckDrmInfoTheme

// ── Device Info ───────────────────────────────────────────────────────────────

@Composable
fun DeviceInfoSection(deviceInfo: DeviceInfo, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Device Info",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
        DeviceInfoItem("Manufacturer", deviceInfo.manufacturer)
        DeviceInfoItem("Model", deviceInfo.model)
        DeviceInfoItem("Android", "${deviceInfo.androidVersion} (SDK ${deviceInfo.sdkInt})")
        DeviceInfoItem("Fingerprint", deviceInfo.fingerprint)
    }
}

@Composable
fun DeviceInfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("$label: ")
            pop()
            append(value)
        },
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.padding(vertical = 1.dp),
    )
}

// ── DRM Check Item ────────────────────────────────────────────────────────────

@Composable
fun DrmCheckItem(entry: DrmEntry, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        DrmStatusIcon(entry.result)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(entry.name, style = MaterialTheme.typography.titleMedium)
            DrmResultDetails(entry.result)
        }
    }
    HorizontalDivider()
}

@Composable
private fun DrmStatusIcon(result: DrmResult) {
    when (result) {
        is DrmResult.Loading -> CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
        )
        is DrmResult.Supported -> Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = "Supported",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        is DrmResult.NotSupported -> Icon(
            painter = painterResource(R.drawable.ic_close_circle),
            contentDescription = "Not Supported",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp),
        )
        is DrmResult.CheckError -> Icon(
            painter = painterResource(R.drawable.ic_close_circle),
            contentDescription = "Check Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun DrmResultDetails(result: DrmResult) {
    when (result) {
        is DrmResult.Loading -> Text(
            "Checking…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is DrmResult.Supported -> {
            if (result.details.isNotBlank()) {
                Text(result.details.trim(), style = MaterialTheme.typography.bodySmall)
            }
        }
        is DrmResult.NotSupported -> Text(
            result.reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is DrmResult.CheckError -> Text(
            result.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val sampleDevice = DeviceInfo(
    model = "Pixel 7 Pro",
    manufacturer = "Google",
    androidVersion = "14",
    sdkInt = 34,
    fingerprint = "google/cheetah/cheetah:14/UP1A.231005.007/10754064:user/release-keys",
)

class DrmEntryPreviewProvider : PreviewParameterProvider<DrmEntry> {
    override val values = sequenceOf(
        DrmEntry("Widevine", DrmResult.Supported("Security Level: L1 (L1=true L2=false L3=false)\nHDCP Level: HW_SECURE_ALL\nVendor: Google\nCDM Version: 16.0.0")),
        DrmEntry("ClearKey", DrmResult.Supported("ClearKey CDM present")),
        DrmEntry("PlayReady", DrmResult.NotSupported("PlayReady is not supported on this device.")),
        DrmEntry("Marlin", DrmResult.NotSupported("Marlin is not supported on this device.")),
        DrmEntry("PrimeTime", DrmResult.CheckError("Error checking PrimeTime: initialization failed")),
        DrmEntry("Widevine", DrmResult.Loading),
    )
}

@Preview(showBackground = true, name = "DRM Check Items")
@Composable
fun DrmCheckItemPreview(
    @PreviewParameter(DrmEntryPreviewProvider::class) entry: DrmEntry,
) {
    CheckDrmInfoTheme {
        DrmCheckItem(entry)
    }
}

@PreviewLightDark
@Composable
fun DrmCheckItemLightDarkPreview() {
    CheckDrmInfoTheme {
        Column {
            DrmCheckItem(DrmEntry("Widevine", DrmResult.Supported("Security Level: L1\nHDCP: HW_SECURE_ALL")))
            DrmCheckItem(DrmEntry("PlayReady", DrmResult.NotSupported("Not supported")))
            DrmCheckItem(DrmEntry("Marlin", DrmResult.Loading))
        }
    }
}

@Preview(showBackground = true, name = "Device Info Section")
@Composable
fun DeviceInfoSectionPreview() {
    CheckDrmInfoTheme {
        DeviceInfoSection(sampleDevice)
    }
}
