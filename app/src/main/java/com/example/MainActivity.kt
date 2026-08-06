package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

