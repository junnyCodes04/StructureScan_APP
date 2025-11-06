package com.example.structurescan
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

class SavedPhotoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val images: ArrayList<Uri>? = intent.getParcelableArrayListExtra(IntentKeys.CAPTURED_IMAGES)
        val assessmentName = intent.getStringExtra(IntentKeys.ASSESSMENT_NAME) ?: "Unnamed Assessment"

        setContent {
            MaterialTheme {
                SavedPhotoScreen(
                    initialImages = images ?: emptyList(),
                    onBack = { updatedImages ->
                        // ✅ Return updated images to CaptureImagesActivity
                        val resultIntent = Intent()
                        resultIntent.putParcelableArrayListExtra(IntentKeys.UPDATED_IMAGES, ArrayList(updatedImages))
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onProceed = {
                        val intent = Intent(this, BuildingInfoActivity::class.java)
                        intent.putParcelableArrayListExtra(IntentKeys.FINAL_IMAGES, ArrayList(it))
                        intent.putExtra(IntentKeys.ASSESSMENT_NAME, assessmentName)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SavedPhotoScreen(
    initialImages: List<Uri>,
    onBack: (List<Uri>) -> Unit,
    onProceed: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val images = remember { mutableStateListOf<Uri>().apply { addAll(initialImages) } }
    var selectedImage by remember { mutableStateOf(images.firstOrNull()) }
    val canProceed = images.size in 1..7

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full preview of selected image - FIXED SIZE with aspect ratio
        selectedImage?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .align(Alignment.Center),
                contentScale = ContentScale.Crop
            )
        }

        // Top bar with back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack(images) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Saved Photos (${images.size})",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Proceed button
        IconButton(
            onClick = {
                when {
                    images.isEmpty() -> {
                        Toast.makeText(context, "Please add at least 1 photo.", Toast.LENGTH_SHORT).show()
                    }
                    images.size > 7 -> {
                        Toast.makeText(context, "You can't add more than 7 photos.", Toast.LENGTH_SHORT).show()
                    }
                    images.size < 3 -> {
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
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(60.dp)
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Proceed",
                tint = if (canProceed) Color(0xFF10B981) else Color.Gray,
                modifier = Modifier.size(30.dp)
            )
        }

        // Thumbnail gallery - FIXED SIZE THUMBNAILS
        if (images.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .padding(end = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images) { uri ->
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .aspectRatio(1f)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .border(
                                    3.dp,
                                    if (uri == selectedImage) Color.Green else Color.White,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedImage = uri },
                            contentScale = ContentScale.Crop
                        )

                        IconButton(
                            onClick = {
                                images.remove(uri)
                                if (selectedImage == uri) {
                                    selectedImage = images.firstOrNull()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SavedPhotoPreview() {
    val fakeUris = listOf(
        Uri.parse("content://sample1"),
        Uri.parse("content://sample2")
    )
    SavedPhotoScreen(
        initialImages = fakeUris,
        onBack = {},
        onProceed = {}
    )
}
