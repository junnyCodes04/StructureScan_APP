package com.example.structurescan.Utils
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.structurescan.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UserSuspensionChecker{

    /**
     * Checks if the current user is suspended
     * @param context - Activity context for navigation
     * @param onNotSuspended - Callback function to run if user is NOT suspended
     * @param onSuspended - Optional callback function if user IS suspended
     */
    fun checkUserSuspension(
        context: Context,
        onNotSuspended: () -> Unit,
        onSuspended: (() -> Unit)? = null
    ) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        // If no user is logged in, skip the check
        if (currentUser == null) {
            onNotSuspended()
            return
        }

        // Get user document from Firestore
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Get the isSuspended field (default to false if not found)
                    val isSuspended = document.getBoolean("isSuspended") ?: false

                    if (isSuspended) {
                        // User IS suspended!
                        handleSuspendedUser(context, auth)
                        onSuspended?.invoke()
                    } else {
                        // User is NOT suspended - proceed normally
                        onNotSuspended()
                    }
                } else {
                    // Document doesn't exist - allow user to proceed
                    onNotSuspended()
                }
            }
            .addOnFailureListener { exception ->
                // Log error but don't block the user
                android.util.Log.e("SuspensionChecker", "Error checking suspension status", exception)
                onNotSuspended()
            }
    }

    /**
     * Handles a suspended user - signs them out and redirects to login
     */
    private fun handleSuspendedUser(context: Context, auth: FirebaseAuth) {
        // Sign out the user immediately
        auth.signOut()

        // Show message to user
        Toast.makeText(
            context,
            "Your account has been suspended. Contact admin for assistance.",
            Toast.LENGTH_LONG
        ).show()

        // Redirect to LoginActivity and clear the back stack
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)

        // If context is an Activity, finish it
        if (context is android.app.Activity) {
            context.finish()
        }
    }
}
