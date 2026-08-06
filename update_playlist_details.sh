#!/bin/bash
sed -i 's/onEditPlaylistName: (String) -> Unit,/onEditPlaylistName: (String) -> Unit,\n    onMoveSong: (Int, Int) -> Unit,/' app/src/main/java/com/example/ui/PlaylistDetailsScreen.kt
