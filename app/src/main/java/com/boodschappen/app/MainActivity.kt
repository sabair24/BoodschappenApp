package com.boodschappen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boodschappen.app.ui.navigation.BoodschappenNavigation
import com.boodschappen.app.ui.theme.BoodschappenTheme
import com.boodschappen.app.viewmodel.ShoppingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ShoppingViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            BoodschappenTheme(darkTheme = isDarkTheme) {
                BoodschappenNavigation(viewModel = viewModel)
            }
        }
    }
}
