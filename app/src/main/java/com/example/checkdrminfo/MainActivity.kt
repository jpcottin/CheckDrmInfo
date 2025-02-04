package com.example.checkdrminfo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.checkdrminfo.ui.theme.CheckDrmInfoTheme
import android.media.MediaDrm
import android.util.Log
import java.util.UUID
import android.media.MediaDrmException
import androidx.media3.common.C
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val TAG = "DRMChecker"

    private val WIDEVINE_UUID = C.WIDEVINE_UUID
    private val CLEARKEY_UUID = C.CLEARKEY_UUID
    private val PLAYREADY_UUID = C.PLAYREADY_UUID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var drmInfoText by mutableStateOf("")

        checkWidevineSupport(drmInfoText) { newText ->
            drmInfoText = newText
        }
        checkClearKeySupport(drmInfoText) { newText ->
            drmInfoText = newText
        }
        checkPlayReadySupport(drmInfoText) { newText ->
            drmInfoText = newText
        }


        setContent {
            CheckDrmInfoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DisplayMessage(
                        name = drmInfoText,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }



    private fun checkWidevineSupport(currentText: String, updateText: (String) -> Unit) {
        checkDrmSupport(WIDEVINE_UUID, "Widevine", currentText, updateText) { mediaDrm, newText ->
            val securityLevel = mediaDrm.getPropertyString("securityLevel")
            val updatedText = "$newText\nWidevine Security Level: $securityLevel"
            securityLevel.let {
                updateText(updatedText +  "\nWidevine L1 support: ${it.equals("L1", true)}")
                updateText(updatedText +  "\nWidevine L2 support: ${it.equals("L2", true)}")
                updateText(updatedText +  "\nWidevine L3 support: ${it.equals("L3", true)}")
            }
            updateText("$updatedText\n---")

        }
    }
    private fun checkClearKeySupport(currentText: String, updateText: (String) -> Unit) {
        checkDrmSupport(CLEARKEY_UUID, "ClearKey", currentText, updateText) { _, newText ->
            updateText("$newText\nClearKey CDM is present.\n---")
        }
    }

    private fun checkPlayReadySupport(currentText: String, updateText: (String) -> Unit) {
        checkDrmSupport(PLAYREADY_UUID, "PlayReady", currentText, updateText) { _, newText ->
            updateText("$newText\nPlayReady support check completed.\n---")
        }
    }

    private fun checkDrmSupport(
        uuid: UUID,
        drmName: String,
        currentText: String,
        updateText: (String) -> Unit,
        additionalChecks: (MediaDrm, String) -> Unit = { _,_ -> }
    ) {
        var newText = currentText

        try {
            if (MediaDrm.isCryptoSchemeSupported(uuid)) {
                newText += "\n$drmName is supported on this device."
                updateText(newText)

                val mediaDrm = MediaDrm(uuid)
                try {
                    additionalChecks(mediaDrm, newText)
                } catch (e: MediaDrmException) {
                    newText += "\nError with $drmName MediaDrm: ${e.message}"
                    updateText(newText)
                    Log.e(TAG, "Error with $drmName MediaDrm", e)
                } finally {
                    mediaDrm.close()
                }

            } else {
                newText += "\n$drmName is NOT supported on this device."
                updateText("$newText\n---")
            }
        } catch (e: Exception) {
            newText += "\nError checking $drmName support: ${e.message}"
            updateText(newText)
            Log.e(TAG, "Error checking $drmName support", e)
        }
    }

}

@Composable
fun DisplayMessage(name: String, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp))
            append("DRM Check\n")
            pop()
            append(name)
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun DisplayMessagePreview() {
    CheckDrmInfoTheme {
        DisplayMessage("EXAMPLE")
    }
}