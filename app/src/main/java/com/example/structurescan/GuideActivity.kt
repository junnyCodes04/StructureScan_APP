package com.example.structurescan
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class GuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the assessment name from ScanActivity using the constant
        val assessmentName = intent.getStringExtra(IntentKeys.ASSESSMENT_NAME) ?: "Unnamed Assessment"

        setContent {
            MaterialTheme {
                GuideScreen(
                    onBackClick = {
                        val intent = Intent(this, ScanActivity::class.java)
                        startActivity(intent)
                    },
                    onScanNow = {
                        val intent = Intent(this, CaptureImagesActivity::class.java)
                        intent.putExtra(IntentKeys.ASSESSMENT_NAME, assessmentName)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun GuideScreen(onBackClick: () -> Unit, onScanNow: () -> Unit) {

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // Enable scrolling
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Top bar with back button & title ---
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
                text = "Create New Assessment",
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF0288D1),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // --- Photo Guide Title ---
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "Photo Guide Icon",
            tint = Color(0xFF0288D1),
            modifier = Modifier.size(50.dp)
        )
        Text("Photo Guide", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Take better photos for more accurate results.",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- Do's Box ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("✅ Do's", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val dos = listOf(
                    "Use daylight or bright lighting.",
                    "Hold your phone steady for clear shots.",
                    "Take wide shots of the whole wall or area.",
                    "Capture multiple angles (front and sides).",
                    "Zoom in on cracks or damage."
                )
                dos.forEach { Text("• $it", fontSize = 14.sp) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Don'ts Box ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("❌ Don'ts", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val donts = listOf(
                    "Don't take blurry or dark photos.",
                    "Don't stand too close (avoid extreme zoom).",
                    "Don't crop out parts of the structure.",
                    "Minimize obstruction."
                )
                donts.forEach { Text("• $it", fontSize = 14.sp) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Steps to Follow Box ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📋 Steps to Follow", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val steps = listOf(
                    "Start with a wide shot of the building or wall.",
                    "Take photos from different sides.",
                    "Add close-ups of visible cracks or damage.",
                    "Review before uploading – make sure it's clear."
                )
                steps.forEachIndexed { i, step ->
                    Text("${i + 1}. $step", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Tip Box ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("💡 Tip", color = Color(0xFFF57F17), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Clear, well-lit photos from multiple angles help the AI give you the most accurate safety assessment.",
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Start Taking Photos Button ---
        Button(
            onClick = { onScanNow() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Start Taking Photos", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GuideScreenPreview() {
    GuideScreen(onBackClick = {}, onScanNow = {})
}