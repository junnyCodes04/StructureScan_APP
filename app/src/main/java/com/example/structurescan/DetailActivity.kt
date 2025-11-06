package com.example.structurescan

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DetailScreen(
                    onSkip = {
                        startActivity(Intent(this, Onboarding1Activity::class.java))
                        finish()
                    },
                    onBack = {
                        val intent = Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    },
                    onContinue = { role, imageUri, bitmap, onLoadingChange ->
                        uploadProfile(role, imageUri, bitmap, onLoadingChange)
                    }
                )
            }
        }
    }

    private fun uploadProfile(
        role: String,
        imageUri: Uri?,
        bitmap: Bitmap?,
        onLoadingChange: (Boolean) -> Unit
    ) {
        onLoadingChange(true)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            onLoadingChange(false)
            Toast.makeText(this, "User not logged in. Please log in first.", Toast.LENGTH_LONG).show()
            return
        }

        val userId = user.uid
        val db = FirebaseFirestore.getInstance()

        // ✅ FLEXIBLE: Check if user has image or role
        val hasImage = imageUri != null || bitmap != null
        val hasRole = role.isNotEmpty()

        if (hasImage) {
            // Upload image first, then save to Firestore
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference.child("profile_images/$userId.jpg")

            val uploadTask = when {
                imageUri != null -> storageRef.putFile(imageUri)
                bitmap != null -> {
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val data = baos.toByteArray()
                    storageRef.putBytes(data)
                }
                else -> null
            }

            uploadTask?.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Save both role and photo URL
                    val userData = hashMapOf<String, Any>(
                        "userId" to userId,
                        "photoUrl" to downloadUri.toString()
                    )
                    if (hasRole) {
                        userData["role"] = role
                    }

                    db.collection("users").document(userId)
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener {
                            onLoadingChange(false)
                            Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, Onboarding1Activity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            onLoadingChange(false)
                            Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }.addOnFailureListener { e ->
                    onLoadingChange(false)
                    Toast.makeText(this, "Failed to get download URL: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }?.addOnFailureListener { e ->
                onLoadingChange(false)
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else if (hasRole) {
            // ✅ Only save role without photo
            val userData = hashMapOf<String, Any>(
                "userId" to userId,
                "role" to role
            )

            db.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener {
                    onLoadingChange(false)
                    Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Onboarding1Activity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    onLoadingChange(false)
                    Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onContinue: (String, Uri?, Bitmap?, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var selectedRole by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            capturedImage = bitmap
        }
    }

    val roleOptions = listOf("Engineer", "Architect", "Inspector", "Manager", "Technician")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        CenterAlignedTopAppBar(
            title = {},
            navigationIcon = {
                IconButton(
                    onClick = { onBack() },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tell us about yourself",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "This helps us personalize your experience",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Add a profile photo", fontSize = 16.sp, fontWeight = FontWeight.Medium)

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray.copy(alpha = 0.3f))
                        .clickable(enabled = !isLoading) {
                            val options = listOf("Take Photo", "Choose from Gallery")
                            androidx.appcompat.app.AlertDialog
                                .Builder(context)
                                .setTitle("Select Option")
                                .setItems(options.toTypedArray()) { _, which ->
                                    when (which) {
                                        0 -> {
                                            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                            cameraLauncher.launch(cameraIntent)
                                        }
                                        1 -> galleryLauncher.launch("image/*")
                                    }
                                }
                                .show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        capturedImage != null -> Image(
                            bitmap = capturedImage!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        imageUri != null -> Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        else -> Text("+", fontSize = 40.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Role", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { if (!isLoading) isDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRole,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        placeholder = { Text("e.g. Engineer", color = Color.Gray) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        },
                        shape = RoundedCornerShape(8.dp)
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
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    // ✅ FLEXIBLE VALIDATION: At least one field must be filled
                    val hasRole = selectedRole.isNotEmpty()
                    val hasImage = imageUri != null || capturedImage != null

                    if (!hasRole && !hasImage) {
                        Toast.makeText(
                            context,
                            "Please add a profile photo or select a role to continue",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Button
                    }

                    onContinue(selectedRole, imageUri, capturedImage) { loading ->
                        isLoading = loading
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0288D1),
                    disabledContainerColor = Color(0xFF0288D1).copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "CONTINUE",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Skip for now",
                color = if (isLoading) Color.Gray else Color(0xFF0288D1),
                modifier = Modifier.clickable(enabled = !isLoading) { onSkip() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
