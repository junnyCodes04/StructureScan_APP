package com.example.structurescan
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --------------------------
// Activity Entry Point
// --------------------------
class ChangePasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set Jetpack Compose UI as the content
        setContent {
            MaterialTheme {
                ChangePasswordScreen(
                    onBackClick = {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                    }, // go back to previous screen
                    onUpdatePassword = { newPass, confirmPass ->
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                    },
                    onForgotPassword = {
                        val intent = Intent(this, EmailForgotPassword::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

// --------------------------
// Composable: Change Password Screen
// --------------------------
@Composable
fun ChangePasswordScreen(
    onBackClick: () -> Unit,
    onUpdatePassword: (String, String) -> Unit,
    onForgotPassword: () -> Unit
) {
    // State variables for text fields
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // ✅ Get context for Toast
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // --------------------------
        // Top Row: Back Button + Title
        // --------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            IconButton(
                onClick = { onBackClick() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Text(
                text = "Change Password",
                fontSize = 20.sp,
                color = Color(0xFF0288D1),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center) // valid in BoxScope
            )
        }

        // --------------------------
        // New Password Field
        // --------------------------
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New Password") },
            placeholder = { Text("Enter new password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --------------------------
        // Confirm Password Field
        // --------------------------
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm New Password") },
            placeholder = { Text("Confirm new password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --------------------------
        // Password Requirements
        // --------------------------
        Text(
            text = "Password Requirements:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• At least 8 characters\n" +
                    "• Include uppercase and lowercase letters\n" +
                    "• Include at least one number\n" +
                    "• Include at least one special character",
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --------------------------
        // Update Password Button
        // --------------------------
        Button(
            onClick = {
                when {
                    // ❌ Check if any field is empty
                    newPassword.isBlank() || confirmPassword.isBlank() -> {
                        Toast.makeText(
                            context,
                            "Fields cannot be left empty!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    // ❌ Check if new passwords do not match
                    newPassword != confirmPassword -> {
                        Toast.makeText(
                            context,
                            "Passwords do not match.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        // ✅ All checks passed — proceed
                        Toast.makeText(
                            context,
                            "Password successfully changed!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Call navigation ONLY when valid
                        onUpdatePassword(newPassword, confirmPassword)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
        ) {
            Text(
                text = "Update Password",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --------------------------
// Preview for Android Studio
// --------------------------
@Preview(showBackground = true)
@Composable
fun ChangePasswordPreview() {
    MaterialTheme {
        ChangePasswordScreen(
            onBackClick = {},
            onUpdatePassword = { _, _ -> },
            onForgotPassword = {}
        )
    }
}