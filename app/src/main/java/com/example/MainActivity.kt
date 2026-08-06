package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // C1: Edge-to-edge immersive UI
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val permissionState = rememberPermissionState(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                )

                val folderUri by viewModel.folderUri.collectAsStateWithLifecycle()
                
                LaunchedEffect(permissionState.status) {
                    if (!permissionState.status.isGranted) {
                        permissionState.launchPermissionRequest()
                    }
                }

                // Initialize login state from Firebase (Bug A4 fix: persist login state)
                var isLoggedIn by androidx.compose.runtime.remember { 
                    androidx.compose.runtime.mutableStateOf(
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
                    ) 
                }

                if (!isLoggedIn) {
                    com.example.ui.LoginScreen(
                        onLoginSuccess = { isLoggedIn = true }
                    )
                } else {
                    if (permissionState.status.isGranted || folderUri != null) {
                        if (folderUri == null) {
                            com.example.ui.SetupScreen(
                                onSetupComplete = { uri ->
                                    viewModel.scanMusic(uri.toString())
                                }
                            )
                        } else {
                            LaunchedEffect(folderUri) {
                                viewModel.scanMusic(folderUri)
                            }
                            MainScreen(viewModel)
                        }
                    } else {
                        com.example.ui.SetupScreen(
                            onSetupComplete = { uri ->
                                viewModel.scanMusic(uri.toString())
                            }
                        )
                    }
                }
            }
        }
    }
}

