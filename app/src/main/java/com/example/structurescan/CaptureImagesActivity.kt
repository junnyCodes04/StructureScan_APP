package com.example.structurescan
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import java.io.File

class CaptureImagesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the assessment name from GuideActivity using the constant
        val assessmentName = intent.getStringExtra(IntentKeys.ASSESSMENT_NAME) ?: "Unnamed Assessment"

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        }

        setContent {
            MaterialTheme {
                CameraScreen(
                    assessmentName = assessmentName,
                    onBack = {
                        val intent = Intent(this, GuideActivity::class.java)
                        startActivity(intent)
                    },
                    onViewImage = { images ->
                        val intent = Intent(this, SavedPhotoActivity::class.java)
                        // ✅ Convert Uri to String for SavedPhotoActivity
                        intent.putStringArrayListExtra(
                            IntentKeys.CAPTURED_IMAGES,
                            ArrayList(images.map { it.toString() })
                        )
                        intent.putExtra(IntentKeys.ASSESSMENT_NAME, assessmentName)
                        startActivity(intent)
                    },
                    onProceed = { images ->
                        val intent = Intent(this, BuildingInfoActivity::class.java)
                        // ✅ Convert Uri to String for BuildingInfoActivity
                        intent.putStringArrayListExtra(
                            IntentKeys.FINAL_IMAGES,
                            ArrayList(images.map { it.toString() })
                        )
                        intent.putExtra(IntentKeys.ASSESSMENT_NAME, assessmentName)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun CameraScreen(
    assessmentName: String,
    onBack: () -> Unit,
    onViewImage: (List<Uri>) -> Unit,
    onProceed: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val images = remember { mutableStateListOf<Uri>() }

    // ✅ FIXED: Activity Result Launcher to receive updated images from SavedPhotoActivity
    val savedPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            // ✅ Get String list and convert back to Uri
            val updatedImagesStrings = result.data?.getStringArrayListExtra(IntentKeys.UPDATED_IMAGES)
            if (updatedImagesStrings != null) {
                images.clear()
                images.addAll(updatedImagesStrings.map { Uri.parse(it) })
            }
        }
    }

    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && pendingPhotoUri != null) {
                images.add(pendingPhotoUri!!)
                Toast.makeText(context, "Photo captured", Toast.LENGTH_SHORT).show()
            }
            pendingPhotoUri = null
        }

    // Extract the capture logic into a function to avoid duplication
    val capturePhoto = {
        if (images.size >= 7) {
            Toast.makeText(context, "Max 7 photos allowed", Toast.LENGTH_SHORT).show()
        } else {
            val photoFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "photo_${System.currentTimeMillis()}.jpg"
            )
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.provider", photoFile
            )
            pendingPhotoUri = uri
            takePicture.launch(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Text(
                "Capture Photos • ${images.size}/7",
                color = Color.Black,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // --- Camera Preview Area ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
        ) {
            if (images.isEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(200.dp)
                        .clickable { capturePhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.Gray,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(20f, 20f)
                                )
                            )
                        )
                    }
                    Text(
                        text = "+",
                        color = Color.Gray,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(images.last()),
                    contentDescription = "Last Captured",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // --- Bottom Controls Section ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (images.isNotEmpty()) {
                Text(
                    text = "${images.size} photo${if (images.size != 1) "s" else ""} captured",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Button - ✅ Now uses ActivityResultLauncher
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, SavedPhotoActivity::class.java)
                            // ✅ Convert Uri to String when passing to SavedPhotoActivity
                            intent.putStringArrayListExtra(
                                IntentKeys.CAPTURED_IMAGES,
                                ArrayList(images.map { it.toString() })
                            )
                            intent.putExtra(IntentKeys.ASSESSMENT_NAME, assessmentName)
                            savedPhotoLauncher.launch(intent)
                        },
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Saved Photos",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = "Gallery",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Capture Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = capturePhoto,
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                if (images.size < 7) Color(0xFF6366F1) else Color.Gray,
                                androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Capture",
                            tint = Color.White,
                            modifier = Modifier.size(35.dp)
                        )
                    }
                    Text(
                        text = "Capture",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Proceed Button
                IconButton(
                    onClick = {
                        when {
                            images.isEmpty() -> {
                                Toast.makeText(context, "Please capture an image", Toast.LENGTH_SHORT).show()
                            }
                            images.size in 1..2 -> {
                                Toast.makeText(
                                    context,
                                    "It's recommended to upload at least 3 photos for better analysis.",
                                    Toast.LENGTH_LONG
                                ).show()
                                onProceed(images)
                            }
                            else -> {
                                onProceed(images)
                            }
                        }
                    },
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Proceed",
                        tint = if (images.isNotEmpty()) Color(0xFF10B981) else Color.Gray,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun CameraPreview() {
    MaterialTheme {
        CameraScreen(
            assessmentName = "Sample Assessment",
            onBack = {},
            onViewImage = {},
            onProceed = {}
        )
    }
}
