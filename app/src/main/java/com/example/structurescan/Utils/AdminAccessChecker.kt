package com.example.structurescan.Utils
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.structurescan.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AdminAccessChecker {

    /**
     * Checks if the current user is an admin account
     * Admins are NOT allowed on mobile app - they should use web dashboard
     *
     * @param context - Activity context for navigation
     * @param onNotAdmin - Callback if user is NOT an admin (allowed to proceed)
     * @param onAdmin - Optional callback if user IS an admin (blocked)
     */
    fun checkIfAdmin(
        context: Context,
        onNotAdmin: () -> Unit,
        onAdmin: (() -> Unit)? = null
    ) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        // If no user is logged in, skip the check
        if (currentUser == null) {
            onNotAdmin()
            return
        }

        // Get user document from Firestore
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Get the isAdmin field (default to false if not found)
                    val isAdmin = document.getBoolean("isAdmin") ?: false

                    if (isAdmin) {
                        // 🚫 User IS an admin - BLOCK!
                        handleAdminBlocked(context, auth)
                        onAdmin?.invoke()
                    } else {
                        // ✅ User is NOT an admin - proceed normally
                        onNotAdmin()
                    }
                } else {
                    // Document doesn't exist - allow user to proceed
                    onNotAdmin()
                }
            }
            .addOnFailureListener { exception ->
                // Log error but don't block the user
                android.util.Log.e("AdminAccessChecker", "Error checking admin status", exception)
                onNotAdmin()
            }
    }

    /**
     * Handles an admin trying to access mobile app
     * Signs them out and shows security-conscious message
     */
    private fun handleAdminBlocked(context: Context, auth: FirebaseAuth) {
        // Sign out the admin immediately
        auth.signOut()

        // Show security-conscious message (doesn't reveal it's an admin account)
        Toast.makeText(
            context,
            "This account type is not supported on mobile. Please use the web portal.",
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
