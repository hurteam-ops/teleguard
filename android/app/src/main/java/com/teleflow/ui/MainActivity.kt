package com.teleflow.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.teleflow.data.SecureStorage
import com.teleflow.ui.theme.TeleFlowTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themePref = SecureStorage.getInstance(this).themePreference
        val dark = when (themePref) {
            "light" -> false
            "dark" -> true
            else -> isSystemInDarkTheme()
        }

        setContent {
            TeleFlowTheme(dark = dark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    TeleFlowNavHost(navController = navController)
                }
            }
        }
    }
}
