package com.example.structurescan
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

class AnalyzedImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Receive analyzed images from AssessmentResultsActivity
        val images: ArrayList<Uri>? = intent.getParcelableArrayListExtra("analyzed_images")

        setContent {
            MaterialTheme {
                AnalyzedImageScreen(
                    images = images ?: emptyList(),
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun AnalyzedImageScreen(
    images: List<Uri>,
    onBack: () -> Unit
) {
    // Currently selected image
    var selectedImage by remember { mutableStateOf(images.firstOrNull()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full preview of selected image
        selectedImage?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Analyzed Images (${images.size})",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Thumbnail gallery (no delete buttons)
        if (images.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images) { uri ->
                    // Thumbnail (clickable but no delete)
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
                }
            }
        }

        // Empty state when no images
        if (images.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No analyzed images available",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Images will appear here after analysis is complete",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnalyzedImagePreview() {
    // Fake preview with sample URIs
    val fakeUris = listOf(
        Uri.parse("content://sample1"),
        Uri.parse("content://sample2"),
        Uri.parse("content://sample3")
    )
    AnalyzedImageScreen(
        images = fakeUris,
        onBack = {}
    )
}