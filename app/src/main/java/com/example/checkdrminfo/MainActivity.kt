package com.example.checkdrminfo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.checkdrminfo.ui.DrmInfoScreen
import com.example.checkdrminfo.ui.theme.CheckDrmInfoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheckDrmInfoTheme {
                DrmInfoScreen()
            }
        }
    }
}
