#!/bin/bash
sed -i 's/import com.example.ui.HomeScreen/import com.example.ui.MainScreen\nimport com.example.ui.HomeScreen/' app/src/main/java/com/example/MainActivity.kt
