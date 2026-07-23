package com.example.checkdrminfo

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.checkdrminfo.ui.DeviceInfoSectionPreview
import com.example.checkdrminfo.ui.DrmCheckItemLightDarkPreview
import com.example.checkdrminfo.ui.DrmInfoContentPhonePortraitPreview
import com.example.checkdrminfo.ui.DrmInfoContentTabletPortraitPreview

// Wraps the existing main-source previews (self-contained sample state) so the
// screenshot plugin, which only scans this source set, picks them up.

@PreviewTest
@Preview(name = "PhonePortrait", device = "spec:width=411dp,height=891dp", showBackground = true)
@Composable
fun DrmInfoPhonePortraitScreenshot() = DrmInfoContentPhonePortraitPreview()

@PreviewTest
@Preview(name = "TabletTwoPane", device = "spec:width=800dp,height=1280dp", showBackground = true)
@Composable
fun DrmInfoTabletTwoPaneScreenshot() = DrmInfoContentTabletPortraitPreview()

@PreviewTest
@Preview(name = "CheckItems", showBackground = true)
@Composable
fun DrmCheckItemsScreenshot() = DrmCheckItemLightDarkPreview()

@PreviewTest
@Preview(name = "DeviceInfo", showBackground = true)
@Composable
fun DeviceInfoScreenshot() = DeviceInfoSectionPreview()
