package com.example.structurescan
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class EnterCodeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                EnterCodeScreen(
                    onBackClick = {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                    },
                    onSendCode = { code ->
                        // Navigate to ChangePasswordActivity when code is entered
                        val intent = Intent(this, ChangePasswordActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun EnterCodeScreen(
    onBackClick: () -> Unit,
    onSendCode: (String) -> Unit
) {
    // State for the 5 code digits
    var codeDigits by remember { mutableStateOf(List(5) { "" }) }
    // State for validation error
    var showError by remember { mutableStateOf(false) }

    // Function to check if all fields are filled
    fun isCodeComplete(): Boolean {
        return codeDigits.all { it.isNotBlank() && it.length == 1 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // --------------------------
        // Back Button + Title
        // --------------------------
        // Back Button + Title
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
                text = "Enter Code",
                fontSize = 20.sp,
                color = Color(0xFF0288D1),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center) // valid in BoxScope
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --------------------------
        // Big Lock Image
        // --------------------------
        Image(
            painter = painterResource(id = R.drawable.lock),
            contentDescription = null,
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --------------------------
        // Instructions
        // --------------------------
        Text(
            text = "Enter the code associated with \n" +
                    "your email address and we'll navigate\n" +
                    "to reset your password.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --------------------------
        // Code Input Boxes (5 boxes)
        // --------------------------
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            codeDigits.forEachIndexed { index, value ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        // Reset error when user starts typing
                        if (showError) showError = false

                        // Only allow 1 digit character per box
                        if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                            codeDigits = codeDigits.toMutableList().also {
                                it[index] = newValue
                            }
                        }
                    },
                    singleLine = true,
                    isError = showError && value.isBlank(),
                    modifier = Modifier
                        .width(50.dp)
                        .height(60.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        errorBorderColor = Color.Red,
                        focusedBorderColor = if (showError && value.isBlank()) Color.Red else Color(0xFF0288D1)
                    )
                )
            }
        }

        // Error message
        if (showError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please enter all 5 digits of the verification code",
                color = Color.Red,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --------------------------
        // Enter Code Button
        // --------------------------
        Button(
            onClick = {
                if (isCodeComplete()) {
                    val enteredCode = codeDigits.joinToString("")
                    onSendCode(enteredCode)
                } else {
                    showError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCodeComplete()) Color(0xFF0288D1) else Color(0xFF0288D1).copy(alpha = 0.6f)
            )
        ) {
            Text(text = "ENTER CODE", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --------------------------
        // Resend Code Option (Optional)
        // --------------------------
        TextButton(
            onClick = {
                // Handle resend code logic here
            }
        ) {
            Text(
                text = "Didn't receive the code? Resend",
                color = Color(0xFF0288D1),
                fontSize = 14.sp
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EnterCodeScreenPreview() {
    EnterCodeScreen(
        onBackClick = {},
        onSendCode = {}
    )
}