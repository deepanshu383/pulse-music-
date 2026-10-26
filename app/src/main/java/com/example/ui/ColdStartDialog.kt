package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ColdStartDialog(
    onDismiss: () -> Unit,
    onSavePreferences: (Set<String>, Set<String>) -> Unit
) {
    var artist1 by remember { mutableStateOf("") }
    var artist2 by remember { mutableStateOf("") }
    var artist3 by remember { mutableStateOf("") }
    var languages by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Welcome to Pulse Music") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Tell us your preferences to jumpstart recommendations:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = artist1,
                    onValueChange = { artist1 = it },
                    label = { Text("Favorite Artist 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = artist2,
                    onValueChange = { artist2 = it },
                    label = { Text("Favorite Artist 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = artist3,
                    onValueChange = { artist3 = it },
                    label = { Text("Favorite Artist 3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = languages,
                    onValueChange = { languages = it },
                    label = { Text("Preferred Languages (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val artists = setOf(artist1.trim(), artist2.trim(), artist3.trim()).filter { it.isNotEmpty() }.toSet()
                    val langs = languages.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                    onSavePreferences(artists, langs)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }
    )
}
