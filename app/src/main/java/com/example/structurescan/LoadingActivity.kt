package com.example.structurescan
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.structurescan.Utils.AdminAccessChecker
import com.example.structurescan.Utils.UserSuspensionChecker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class LoadingActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            MaterialTheme {
                LoadingScreen(
                    onTimeout = {
                        checkAuthAndNavigate()
                    }
                )
            }
        }
    }

    private fun checkAuthAndNavigate() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // ✅ User is logged in - CHECK ADMIN FIRST!
            AdminAccessChecker.checkIfAdmin(
                context = this,
                onNotAdmin = {
                    // ✅ Not an admin - NOW check suspension
                    UserSuspensionChecker.checkUserSuspension(
                        context = this,
                        onNotSuspended = {
                            // ✅ Not admin AND not suspended - GO TO DASHBOARD!
                            val intent = Intent(this, DashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        },
                        onSuspended = {
                            // 🚫 User IS suspended - already redirected to Login
                            finish()
                        }
                    )
                },
                onAdmin = {
                    // 🚫 User IS admin - already blocked with toast and redirected to Login
                    finish()
                }
            )
        } else {
            // ❌ No user logged in - go to Login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

@Composable
fun LoadingScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000) // 3 seconds splash
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "StructureScan",
                color = Color(0xFF1565C0),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            CircularProgressIndicator(
                color = Color(0xFF1565C0),
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Loading...",
                color = Color(0xFF1565C0),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    MaterialTheme {
        LoadingScreen(onTimeout = {})
    }
}