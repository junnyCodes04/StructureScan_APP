package com.example.structurescan
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirestoreInstance.db

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            RegisterScreen(this, auth, db, googleSignInClient)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    activity: ComponentActivity,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    googleSignInClient: GoogleSignInClient
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var agreeTerms by remember { mutableStateOf(false) }

    // ✅ Loading states
    var isEmailSignUpLoading by remember { mutableStateOf(false) }
    var isGoogleSignUpLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            // ✅ Start Google loading
            isGoogleSignUpLoading = true

            auth.signInWithCredential(credential)
                .addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid
                        val userData = hashMapOf(
                            "fullName" to (user?.displayName ?: ""),
                            "email" to (user?.email ?: ""),
                            "role" to "user",
                            "photoUrl" to (user?.photoUrl?.toString() ?: "")
                        )

                        if (userId != null) {
                            db.collection("users").document(userId)
                                .set(userData)
                                .addOnSuccessListener {
                                    isGoogleSignUpLoading = false
                                    Toast.makeText(activity, "Google Sign-Up Successful!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(activity, DetailActivity::class.java)
                                    activity.startActivity(intent)
                                    activity.finish()
                                }
                                .addOnFailureListener {
                                    isGoogleSignUpLoading = false
                                    Toast.makeText(activity, "Failed to save user data.", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        isGoogleSignUpLoading = false
                        Toast.makeText(activity, "Google Sign-Up Failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: ApiException) {
            isGoogleSignUpLoading = false
            Toast.makeText(activity, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Create new account",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0288D1)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Full Name Field
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            placeholder = { Text("John Doe") },
            singleLine = true,
            enabled = !isEmailSignUpLoading && !isGoogleSignUpLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            placeholder = { Text("example@gmail.com") },
            singleLine = true,
            enabled = !isEmailSignUpLoading && !isGoogleSignUpLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            enabled = !isEmailSignUpLoading && !isGoogleSignUpLoading,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    enabled = !isEmailSignUpLoading && !isGoogleSignUpLoading
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle Password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password Field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            singleLine = true,
            enabled = !isEmailSignUpLoading && !isGoogleSignUpLoading,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                    enabled = !isEmailSignUpLoading && !isGoogleSignUpLoading
                ) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle Password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Terms Checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = agreeTerms,
                onCheckedChange = { agreeTerms = it },
                enabled = !isEmailSignUpLoading && !isGoogleSignUpLoading
            )
            Text(
                text = buildAnnotatedString {
                    append("I agree to ")
                    withStyle(style = SpanStyle(color = Color(0xFF0288D1), fontWeight = FontWeight.Bold)) {
                        append("Terms and Conditions")
                    }
                    append(" and have read the ")
                    withStyle(style = SpanStyle(color = Color(0xFF0288D1), fontWeight = FontWeight.Bold)) {
                        append("Privacy Policy")
                    }
                },
                fontSize = 14.sp,
                color = if (isEmailSignUpLoading || isGoogleSignUpLoading) Color.Gray else Color.Black,
                modifier = Modifier.clickable(enabled = !isEmailSignUpLoading && !isGoogleSignUpLoading) {
                    agreeTerms = !agreeTerms
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ Sign Up Button with Loading Spinner
        Button(
            onClick = {
                if (fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    Toast.makeText(activity, "All fields are required.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password != confirmPassword) {
                    Toast.makeText(activity, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!agreeTerms) {
                    Toast.makeText(activity, "You must agree to the terms.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isEmailSignUpLoading = true

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val userId = user?.uid

                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build()
                            user?.updateProfile(profileUpdates)

                            val userData = hashMapOf(
                                "fullName" to fullName,
                                "email" to email,
                                "role" to "user",
                                "photoUrl" to ""
                            )

                            if (userId != null) {
                                db.collection("users").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        isEmailSignUpLoading = false
                                        Toast.makeText(activity, "Account created!", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(activity, DetailActivity::class.java)
                                        activity.startActivity(intent)
                                        activity.finish()
                                    }
                                    .addOnFailureListener {
                                        isEmailSignUpLoading = false
                                        Toast.makeText(activity, "Failed to save user data.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            isEmailSignUpLoading = false
                            Toast.makeText(activity, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            enabled = !isEmailSignUpLoading && !isGoogleSignUpLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0288D1),
                disabledContainerColor = Color(0xFF0288D1).copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp)
        ) {
            if (isEmailSignUpLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("SIGN UP", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Or continue with",
            color = if (isEmailSignUpLoading || isGoogleSignUpLoading) Color.Gray.copy(alpha = 0.5f) else Color.Gray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Google Sign-In Button with Loading Spinner
        Box(
            modifier = Modifier.size(50.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isGoogleSignUpLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(50.dp),
                    color = Color(0xFF0288D1),
                    strokeWidth = 3.dp
                )
            } else {
                Surface(
                    shape = CircleShape,
                    border = ButtonDefaults.outlinedButtonBorder,
                    modifier = Modifier
                        .size(50.dp)
                        .clickable(enabled = !isEmailSignUpLoading) {
                            val signInIntent = googleSignInClient.signInIntent
                            launcher.launch(signInIntent)
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.google_logo),
                        contentDescription = "Google Sign In",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text("Already have an account? ", fontSize = 14.sp, color = Color.Gray)
            Text(
                text = "Back to Log In",
                color = if (isEmailSignUpLoading || isGoogleSignUpLoading) Color.Gray else Color(0xFF0288D1),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !isEmailSignUpLoading && !isGoogleSignUpLoading) {
                    val intent = Intent(activity, LoginActivity::class.java)
                    activity.startActivity(intent)
                    activity.finish()
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(
        activity = object : ComponentActivity() {},
        auth = FirebaseAuth.getInstance(),
        db = FirebaseFirestore.getInstance(),
        googleSignInClient = GoogleSignIn.getClient(
            object : ComponentActivity() {},
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
    )
}