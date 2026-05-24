package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.DebtViewModel
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: DebtViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Intelligently bind dynamic local dark mode toggle
            MyApplicationTheme(darkTheme = viewModel.darkModeEnabled.value) {
                // Handle system Android Back Key to pop custom screen navigation stack
                BackHandler(enabled = viewModel.navigationStack.size > 1) {
                    viewModel.navigateBack()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Main layout content binding
                    MainAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}
