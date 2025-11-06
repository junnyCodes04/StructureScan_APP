package com.example.structurescan
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EditProfileScreen(
                onBackClick = { finish() },
                onSaveChanges = { _, _, _, _ ->
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    onSaveChanges: (String, String, String, Uri?) -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val currentUser = auth.currentUser

    // UI states
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Local image states
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var fullNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }

    // ✅ Loading state
    var isLoading by remember { mutableStateOf(false) }

    val isGoogleUser = currentUser?.providerData?.any { it.providerId == "google.com" } == true
    val roleOptions = listOf("Engineer", "Architect", "Inspector", "Manager", "Technician")

    val coroutineScope = rememberCoroutineScope()

    // Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            imageBitmap = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            imageBitmap = it
            imageUri = null
        }
    }

    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Load user data once
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                fullName = doc.getString("fullName") ?: currentUser.displayName ?: ""
                email = doc.getString("email") ?: currentUser.email ?: ""
                selectedRole = doc.getString("role") ?: "Engineer"
                photoUrl = doc.getString("photoUrl") ?: currentUser.photoUrl?.toString()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Validation helpers
    fun validateFullName(name: String): String {
        return when {
            name.isBlank() -> "Full name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            !name.matches(Regex("^[a-zA-Z\\s]+$")) -> "Only letters and spaces allowed"
            else -> ""
        }
    }

    fun validateEmailAddress(e: String): String {
        val pattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return when {
            e.isBlank() -> "Email required"
            !pattern.matcher(e).matches() -> "Invalid email"
            else -> ""
        }
    }

    // Upload helpers
    suspend fun uploadBitmap(uid: String, bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val bytes = baos.toByteArray()
        val ref = storage.reference.child("profile_images/$uid.jpg")
        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadUri(uid: String, uri: Uri): String {
        val ref = storage.reference.child("profile_images/$uid.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    val isFormValid = remember(fullNameError, emailError, fullName, email, selectedRole) {
        (fullNameError.isEmpty() && emailError.isEmpty() && fullName.isNotBlank() && email.isNotBlank() && selectedRole.isNotBlank())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TopAppBar(
            title = { Text("Edit Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black) },
            navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    enabled = !isLoading // ✅ Disable back while loading
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile picture
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray.copy(alpha = 0.25f))
                    .clickable(enabled = !isLoading) { // ✅ Disable while loading
                        showImageSourceDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    imageBitmap != null -> {
                        Image(
                            bitmap = imageBitmap!!.asImageBitmap(),
                            contentDescription = "Selected photo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    imageUri != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(model = imageUri),
                            contentDescription = "Selected photo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    photoUrl != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(model = photoUrl),
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Image(
                            painter = painterResource(id = android.R.drawable.ic_menu_camera),
                            contentDescription = "Placeholder",
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }

            Text(
                text = "Change Photo",
                color = if (isLoading) Color.Gray else Color(0xFF0288D1),
                modifier = Modifier.clickable(enabled = !isLoading) {
                    showImageSourceDialog = true
                }
            )

            // Image source dialog
            if (showImageSourceDialog) {
                AlertDialog(
                    onDismissRequest = { showImageSourceDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }) {
                            Text("Choose from Gallery")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showImageSourceDialog = false
                            cameraLauncher.launch(null)
                        }) {
                            Text("Take Photo")
                        }
                    },
                    title = { Text("Select Image") },
                    text = { Text("Choose an image from Gallery or take a photo with Camera.") }
                )
            }

            // Full name
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    fullNameError = validateFullName(it)
                },
                label = { Text("Full Name") },
                readOnly = isGoogleUser,
                enabled = !isLoading, // ✅ Disable while loading
                isError = fullNameError.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
            if (fullNameError.isNotEmpty()) {
                Text(fullNameError, color = Color.Red, fontSize = 12.sp)
            }

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = validateEmailAddress(it)
                },
                label = { Text("Email Address") },
                readOnly = isGoogleUser,
                enabled = !isLoading, // ✅ Disable while loading
                isError = emailError.isNotEmpty(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            if (emailError.isNotEmpty()) {
                Text(emailError, color = Color.Red, fontSize = 12.sp)
            }

            // Role dropdown
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { if (!isLoading) isDropdownExpanded = !isDropdownExpanded } // ✅ Disable while loading
            ) {
                OutlinedTextField(
                    value = selectedRole,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isLoading, // ✅ Disable while loading
                    label = { Text("Role") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    roleOptions.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role) },
                            onClick = {
                                selectedRole = role
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // ✅ Save button with loading spinner
            Button(
                onClick = {
                    fullNameError = validateFullName(fullName)
                    emailError = validateEmailAddress(email)
                    if (!isFormValid) return@Button

                    val uid = currentUser?.uid
                    if (uid == null) {
                        Toast.makeText(context, "No user signed in", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true // ✅ Start loading

                    coroutineScope.launch {
                        try {
                            val newPhotoUrl = when {
                                imageBitmap != null -> uploadBitmap(uid, imageBitmap!!)
                                imageUri != null -> uploadUri(uid, imageUri!!)
                                else -> photoUrl
                            }

                            val updateMap = mutableMapOf<String, Any>(
                                "role" to selectedRole,
                                "photoUrl" to (newPhotoUrl ?: "")
                            )
                            if (!isGoogleUser) {
                                updateMap["fullName"] = fullName
                                updateMap["email"] = email
                            }

                            firestore.collection("users")
                                .document(uid)
                                .set(updateMap, com.google.firebase.firestore.SetOptions.merge())
                                .await()

                            photoUrl = newPhotoUrl
                            imageBitmap = null
                            imageUri = null

                            isLoading = false // ✅ Stop loading

                            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            onSaveChanges(fullName, email, selectedRole, null)
                        } catch (e: Exception) {
                            isLoading = false // ✅ Stop loading on error
                            Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
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
                    Text("Save Changes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Cancel",
                color = if (isLoading) Color.Gray else Color(0xFF0288D1),
                modifier = Modifier
                    .clickable(enabled = !isLoading) { onBackClick() }
                    .padding(8.dp)
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun EditProfileScreenPreview() {
    EditProfileScreen(onBackClick = {}, onSaveChanges = { _, _, _, _ -> })
}