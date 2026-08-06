package com.example.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseSyncManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun syncUserProfile() {
        val user = auth.currentUser ?: return
        val userRef = firestore.collection("users").document(user.uid)
        
        val userData = hashMapOf(
            "displayName" to user.displayName,
            "email" to user.email,
            "photoUrl" to user.photoUrl?.toString(),
            "lastLogin" to System.currentTimeMillis()
        )
        
        try {
            userRef.set(userData).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
