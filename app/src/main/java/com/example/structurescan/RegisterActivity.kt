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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

// ✅ Password Validation Data Class
data class PasswordValidation(
    val hasMinLength: Boolean = false,
    val hasUpperCase: Boolean = false,
    val hasLowerCase: Boolean = false,
    val hasDigit: Boolean = false,
    val hasSpecialChar: Boolean = false
) {
    fun isValid(): Boolean = hasMinLength && hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
}

// ✅ Password Validator Function
fun validatePassword(password: String): PasswordValidation {
    return PasswordValidation(
        hasMinLength = password.length >= 8,
        hasUpperCase = password.any { it.isUpperCase() },
        hasLowerCase = password.any { it.isLowerCase() },
        hasDigit = password.any { it.isDigit() },
        hasSpecialChar = password.any { !it.isLetterOrDigit() }
    )
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

    // ✅ Password validation state
    var passwordValidation by remember { mutableStateOf(PasswordValidation()) }
    var showPasswordRequirements by remember { mutableStateOf(false) }

    // ✅ NEW: Terms dialog states
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

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

            isGoogleSignUpLoading = true

            auth.signInWithCredential(credential)
                .addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid
                        val userData = hashMapOf(
                            "fullName" to (user?.displayName ?: ""),
                            "email" to (user?.email ?: ""),
                            "profession" to "",
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

    // ✅ Terms and Conditions Dialog
    if (showTermsDialog) {
        TermsDialog(
            onDismiss = { showTermsDialog = false },
            title = "Terms and Conditions"
        )
    }

    // ✅ Privacy Policy Dialog
    if (showPrivacyDialog) {
        PrivacyDialog(
            onDismiss = { showPrivacyDialog = false },
            title = "Privacy Policy"
        )
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
            onValueChange = {
                password = it
                passwordValidation = validatePassword(it)
                showPasswordRequirements = it.isNotEmpty()
            },
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

        // ✅ Password Requirements Indicator
        if (showPasswordRequirements) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Password must contain:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    PasswordRequirementItem("At least 8 characters", passwordValidation.hasMinLength)
                    PasswordRequirementItem("One uppercase letter (A-Z)", passwordValidation.hasUpperCase)
                    PasswordRequirementItem("One lowercase letter (a-z)", passwordValidation.hasLowerCase)
                    PasswordRequirementItem("One number (0-9)", passwordValidation.hasDigit)
                    PasswordRequirementItem("One special character (!@#$%^&*)", passwordValidation.hasSpecialChar)
                }
            }
        }

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
            isError = confirmPassword.isNotEmpty() && password != confirmPassword,
            supportingText = {
                if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                    Text(
                        text = "Passwords do not match",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Terms Checkbox with Clickable Links
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
                    showTermsDialog = true
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ Sign Up Button
        Button(
            onClick = {
                if (fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    Toast.makeText(activity, "All fields are required.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(activity, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!passwordValidation.isValid()) {
                    Toast.makeText(activity, "Password does not meet the requirements.", Toast.LENGTH_LONG).show()
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
                                "profession" to "",
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

        // Google Sign-In Button
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

// ✅ NEW: Terms and Conditions Dialog
@Composable
fun TermsDialog(onDismiss: () -> Unit, title: String) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0288D1),
                    modifier = Modifier.padding(16.dp)
                )

                Divider()

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = """
                        Last Updated: November 13, 2025

                        1. Acceptance of Terms
                        By creating an account and using StructureScan, you agree to be bound by these Terms and Conditions.

                        2. User Accounts
                        • You must provide accurate and complete information during registration
                        • You are responsible for maintaining the confidentiality of your account credentials
                        • You must be at least 18 years old to create an account

                        3. User Conduct
                        You agree not to:
                        • Upload false, inaccurate, or misleading information
                        • Use the service for any illegal purposes
                        • Attempt to gain unauthorized access to our systems
                        • Share your account with others

                        4. Data and Privacy
                        • We collect and process your personal data in accordance with our Privacy Policy
                        • Profile photos and inspection data are stored securely on Firebase servers
                        • You retain ownership of your uploaded content

                        5. Service Availability
                        • We strive to provide 24/7 service availability
                        • We do not guarantee uninterrupted access
                        • Scheduled maintenance may occur with prior notice

                        6. Intellectual Property
                        • The StructureScan app and all related content are protected by copyright
                        • You may not copy, modify, or distribute our software

                        7. Limitation of Liability
                        StructureScan is provided "as is" without warranties. We are not liable for any damages arising from the use of our service.

                        8. Changes to Terms
                        We reserve the right to modify these terms at any time. Continued use of the service constitutes acceptance of modified terms.

                        9. Termination
                        We reserve the right to suspend or terminate accounts that violate these terms.

                        10. Contact
                        For questions about these terms, please contact our support team through the app.
                        """.trimIndent(),
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 20.sp
                    )
                }

                Divider()

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

// ✅ NEW: Privacy Policy Dialog
@Composable
fun PrivacyDialog(onDismiss: () -> Unit, title: String) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0288D1),
                    modifier = Modifier.padding(16.dp)
                )

                Divider()

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = """
                        Last Updated: November 13, 2025

                        1. Information We Collect
                        We collect the following information:
                        • Full name and email address
                        • Professional information (Engineer, Architect, etc.)
                        • Profile photos (optional)
                        • Structural inspection photos and data
                        • Device information and usage data

                        2. How We Use Your Information
                        Your data is used to:
                        • Provide and maintain our services
                        • Authenticate your account
                        • Store and organize your inspection data
                        • Improve app functionality and user experience
                        • Send important service notifications

                        3. Data Storage and Security
                        • All data is encrypted and stored on Firebase servers
                        • We use industry-standard security measures
                        • Profile images are stored in Firebase Cloud Storage
                        • User credentials are handled by Firebase Authentication

                        4. Data Sharing
                        We do NOT:
                        • Sell your personal information to third parties
                        • Share your data for advertising purposes
                        • Disclose your information without consent

                        We MAY share data:
                        • When required by law
                        • To protect our legal rights
                        • With your explicit consent

                        5. Your Rights
                        You have the right to:
                        • Access your personal data
                        • Update or correct your information
                        • Delete your account and associated data
                        • Export your data

                        6. Cookies and Tracking
                        • We use Firebase Analytics to understand app usage
                        • No third-party advertising cookies are used
                        • Session data helps maintain login status

                        7. Third-Party Services
                        We use:
                        • Firebase (Google) for authentication and data storage
                        • Google Sign-In for authentication

                        8. Children's Privacy
                        Our service is not intended for users under 18 years of age.

                        9. Changes to Privacy Policy
                        We will notify users of any material changes to this policy.

                        10. Contact Us
                        For privacy concerns, contact us through the app support feature.
                        """.trimIndent(),
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 20.sp
                    )
                }

                Divider()

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

// ✅ Password Requirement Item Composable
@Composable
fun PasswordRequirementItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else Color(0xFFE57373),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (isMet) Color(0xFF4CAF50) else Color(0xFF666666)
        )
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
