package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.SongEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectSongsScreen(
    allSongs: List<SongEntity>,
    existingSongs: List<SongEntity>,
    onBack: () -> Unit,
    onAddSongs: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedSongPaths = remember { mutableStateListOf<String>() }
    
    val existingSongPaths = existingSongs.map { it.filePath }.toSet()

    val filteredSongs = allSongs.filter {
        !existingSongPaths.contains(it.filePath) &&
        (it.title.contains(searchQuery, ignoreCase = true) || 
         it.artist.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Songs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedSongPaths.isNotEmpty()) {
                        IconButton(onClick = { 
                            onAddSongs(selectedSongPaths.toList())
                            onBack()
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Done")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search songs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(filteredSongs) { song ->
                    val isSelected = selectedSongPaths.contains(song.filePath)
                    ListItem(
                        headlineContent = { Text(song.title, maxLines = 1) },
                        supportingContent = { Text(song.artist, maxLines = 1) },
                        trailingContent = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedSongPaths.add(song.filePath)
                                    } else {
                                        selectedSongPaths.remove(song.filePath)
                                    }
                                }
                            )
                        },
                        modifier = Modifier.clickable {
                            if (isSelected) {
                                selectedSongPaths.remove(song.filePath)
                            } else {
                                selectedSongPaths.add(song.filePath)
                            }
                        }
                    )
                }
            }
        }
    }
}
