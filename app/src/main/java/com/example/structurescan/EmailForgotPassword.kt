package com.example.structurescan
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth

class EmailForgotPassword : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ForgotPasswordScreen(
                    onBackClick = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val firebaseAuth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Email validation functions
    fun validateEmail(email: String): String {
        return when {
            email.isBlank() -> "Email address is required"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Please enter a valid email address"
            else -> ""
        }
    }

    // ✅ Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onBackClick()
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.mail),
                    contentDescription = null,
                    tint = Color(0xFF0288D1),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Check Your Email",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "If an account exists with this email, we've sent a password reset link to:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        email,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Click the link in the email to reset your password. The link will expire in 1 hour.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Don't see the email? Check your spam folder.",
                        fontSize = 12.sp,
                        color = Color(0xFFF57C00),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Note: If this account uses Google Sign-In, no password reset is needed.",
                        fontSize = 11.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0288D1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK, Got It!")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Back Button + Title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            IconButton(
                onClick = { onBackClick() },
                modifier = Modifier.align(Alignment.CenterStart),
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Text(
                text = "Forgot Password",
                fontSize = 20.sp,
                color = Color(0xFF0288D1),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Big Mail Image
        Image(
            painter = painterResource(id = R.drawable.mail),
            contentDescription = null,
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Instructions
        Text(
            text = "Enter the email address associated with your account, and we'll send you a link to reset your password.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email Input with validation
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (showError) {
                    showError = false
                    errorMessage = ""
                }
            },
            label = { Text("Email Address") },
            placeholder = { Text("example@gmail.com") },
            singleLine = true,
            isError = showError,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                errorBorderColor = Color.Red,
                errorLabelColor = Color.Red,
                focusedBorderColor = if (showError) Color.Red else Color(0xFF0288D1)
            ),
            supportingText = if (showError && errorMessage.isNotEmpty()) {
                {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            } else null
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ SIMPLIFIED: Just send reset email directly
        Button(
            onClick = {
                val validation = validateEmail(email)
                if (validation.isEmpty()) {
                    isLoading = true

                    // Send password reset email directly
                    firebaseAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener {
                            isLoading = false
                            showSuccessDialog = true
                        }
                        .addOnFailureListener { e ->
                            isLoading = false

                            // Handle specific errors
                            when {
                                e.message?.contains("user-not-found", ignoreCase = true) == true -> {
                                    // ✅ Don't reveal if account exists - show success anyway for security
                                    showSuccessDialog = true
                                }
                                e.message?.contains("invalid-email", ignoreCase = true) == true -> {
                                    showError = true
                                    errorMessage = "Please enter a valid email address"
                                }
                                e.message?.contains("network", ignoreCase = true) == true -> {
                                    showError = true
                                    errorMessage = "Network error. Please check your connection"
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    showError = true
                                    errorMessage = "Failed to send reset email. Please try again."
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                } else {
                    showError = true
                    errorMessage = validation
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0288D1),
                disabledContainerColor = Color(0xFF0288D1).copy(alpha = 0.6f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "SEND RESET LINK",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back to Login option
        TextButton(
            onClick = { onBackClick() },
            enabled = !isLoading
        ) {
            Text(
                text = "Remember your password? Sign In",
                color = Color(0xFF0288D1),
                fontSize = 14.sp
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewForgotPasswordScreen() {
    ForgotPasswordScreen(
        onBackClick = {}
    )
}
