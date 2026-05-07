package com.boodschappen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boodschappen.app.ui.navigation.BoodschappenNavigation
import com.boodschappen.app.ui.screens.UpdateDialog
import com.boodschappen.app.ui.theme.BoodschappenTheme
import com.boodschappen.app.viewmodel.ShoppingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ShoppingViewModel = viewModel()
            val isDarkTheme  by viewModel.isDarkTheme.collectAsState()
            val updateState  by viewModel.updateState.collectAsState()

            // Check voor update bij opstarten
            LaunchedEffect(Unit) {
                viewModel.checkForUpdate(BuildConfig.VERSION_CODE)
            }

            BoodschappenTheme(darkTheme = isDarkTheme) {
                BoodschappenNavigation(viewModel = viewModel)

                // Update popup bovenop alles
                UpdateDialog(
                    updateState = updateState,
                    isDark      = isDarkTheme,
                    onDownload  = { url -> viewModel.downloadAndInstall(url) },
                    onDismiss   = { viewModel.dismissUpdate() }
                )
            }
        }
    }
}
