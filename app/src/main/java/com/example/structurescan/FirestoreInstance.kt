// FirestoreInstance.kt
package com.example.structurescan

import com.google.firebase.firestore.FirebaseFirestore

object FirestoreInstance {
    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()  // ✅ no argument
    }
}
