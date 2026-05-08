package com.example.checkdrminfo.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.checkdrminfo.DRMViewModel
import com.example.checkdrminfo.model.DeviceInfo
import com.example.checkdrminfo.model.DrmEntry
import com.example.checkdrminfo.model.DrmInfoState
import com.example.checkdrminfo.model.DrmResult
import com.example.checkdrminfo.ui.theme.CheckDrmInfoTheme

// ── Screen (stateful entry point) ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrmInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: DRMViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val useTwoPane = windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DRM Check") },
                actions = {
                    IconButton(onClick = { shareReport(context, viewModel.getShareableReport()) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share DRM report")
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        DrmInfoContent(
            state = uiState,
            useTwoPane = useTwoPane,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

// ── Content (stateless — drives adaptive layout) ──────────────────────────────

@Composable
fun DrmInfoContent(
    state: DrmInfoState,
    useTwoPane: Boolean,
    modifier: Modifier = Modifier,
) {
    if (useTwoPane) {
        Row(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(end = 16.dp),
            ) {
                DeviceInfoSection(state.deviceInfo)
            }
            VerticalDivider()
            Column(
                modifier = Modifier
                    .weight(2f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp),
            ) {
                DrmSectionContent(state)
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            DeviceInfoSection(state.deviceInfo)
            Spacer(modifier = Modifier.height(24.dp))
            DrmSectionContent(state)
        }
    }
}

@Composable
private fun DrmSectionContent(state: DrmInfoState) {
    Text(
        text = "DRM Support",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    if (state.isLoading && state.drmEntries.isEmpty()) {
        CircularProgressIndicator()
    } else {
        state.drmEntries.forEach { entry ->
            DrmCheckItem(entry)
        }
    }
}

private fun shareReport(context: Context, report: String) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, report)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(intent, null))
}

// ── Preview data ──────────────────────────────────────────────────────────────

private val previewState = DrmInfoState(
    drmEntries = listOf(
        DrmEntry("Widevine",  DrmResult.Supported("Security Level: L1 (L1=true L2=false L3=false)\nHDCP: HW_SECURE_ALL\nVendor: Google\nCDM Version: 16.0")),
        DrmEntry("ClearKey",  DrmResult.Supported("ClearKey CDM present")),
        DrmEntry("PlayReady", DrmResult.NotSupported("PlayReady is not supported on this device.")),
        DrmEntry("Marlin",    DrmResult.NotSupported("Marlin is not supported on this device.")),
        DrmEntry("PrimeTime", DrmResult.CheckError("Error checking PrimeTime: initialization failed")),
    ),
    deviceInfo = DeviceInfo(
        model = "Pixel 7 Pro",
        manufacturer = "Google",
        androidVersion = "14",
        sdkInt = 34,
        fingerprint = "google/cheetah/cheetah:14/UP1A.231005.007/10754064:user/release-keys",
    ),
    isLoading = false,
)

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "Phone — Portrait", showSystemUi = true, device = "spec:width=411dp,height=891dp")
@Composable
fun DrmInfoContentPhonePortraitPreview() {
    CheckDrmInfoTheme {
        DrmInfoContent(state = previewState, useTwoPane = false)
    }
}

@Preview(name = "Phone — Landscape", showSystemUi = true, device = "spec:width=891dp,height=411dp")
@Composable
fun DrmInfoContentPhoneLandscapePreview() {
    CheckDrmInfoTheme {
        DrmInfoContent(state = previewState, useTwoPane = false)
    }
}

@Preview(name = "Tablet — Portrait (two-pane)", showSystemUi = true, device = "spec:width=800dp,height=1280dp")
@Composable
fun DrmInfoContentTabletPortraitPreview() {
    CheckDrmInfoTheme {
        DrmInfoContent(state = previewState, useTwoPane = true)
    }
}

@Preview(name = "Tablet — Landscape (two-pane)", showSystemUi = true, device = "spec:width=1280dp,height=800dp")
@Composable
fun DrmInfoContentTabletLandscapePreview() {
    CheckDrmInfoTheme {
        DrmInfoContent(state = previewState, useTwoPane = true)
    }
}

@Preview(name = "Foldable — Unfolded (two-pane)", showSystemUi = true, device = Devices.FOLDABLE)
@Composable
fun DrmInfoContentFoldablePreview() {
    CheckDrmInfoTheme {
        DrmInfoContent(state = previewState, useTwoPane = true)
    }
}

@Preview(name = "TV 1080p (two-pane)", showSystemUi = true, device = Devices.TV_1080p)
@Composable
fun DrmInfoContentTvPreview() {
    CheckDrmInfoTheme {
        DrmInfoContent(state = previewState, useTwoPane = true)
    }
}

@Preview(name = "Loading state", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun DrmInfoContentLoadingPreview() {
    CheckDrmInfoTheme {
        DrmInfoContent(
            state = DrmInfoState(
                drmEntries = listOf(
                    DrmEntry("Widevine",  DrmResult.Loading),
                    DrmEntry("ClearKey",  DrmResult.Loading),
                    DrmEntry("PlayReady", DrmResult.Loading),
                    DrmEntry("Marlin",    DrmResult.Loading),
                    DrmEntry("PrimeTime", DrmResult.Loading),
                ),
                deviceInfo = previewState.deviceInfo,
                isLoading = true,
            ),
            useTwoPane = false,
        )
    }
}
