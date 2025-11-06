package com.example.structurescan
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Activity that displays the AI analysis loading screen
 * This screen shows animated progress while simulating structure analysis
 */
class AnalysisLoadingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Receive images from intent
        val capturedImages = intent.getStringArrayListExtra(IntentKeys.CAPTURED_IMAGES) ?: arrayListOf()
        val finalImages = intent.getStringArrayListExtra(IntentKeys.FINAL_IMAGES) ?: arrayListOf()

        // ✅ Receive assessment name
        val assessmentName = intent.getStringExtra(IntentKeys.ASSESSMENT_NAME) ?: "Unnamed Assessment"

        // ✅ Receive building information
        val buildingType = intent.getStringExtra(IntentKeys.BUILDING_TYPE) ?: ""
        val constructionYear = intent.getStringExtra(IntentKeys.CONSTRUCTION_YEAR) ?: ""
        val renovationYear = intent.getStringExtra(IntentKeys.RENOVATION_YEAR) ?: ""
        val floors = intent.getStringExtra(IntentKeys.FLOORS) ?: ""
        val material = intent.getStringExtra(IntentKeys.MATERIAL) ?: ""
        val foundation = intent.getStringExtra(IntentKeys.FOUNDATION) ?: ""
        val environment = intent.getStringExtra(IntentKeys.ENVIRONMENT) ?: ""
        val previousIssues = intent.getStringArrayListExtra(IntentKeys.PREVIOUS_ISSUES) ?: arrayListOf()
        val occupancy = intent.getStringExtra(IntentKeys.OCCUPANCY) ?: ""
        val environmentalRisks = intent.getStringArrayListExtra(IntentKeys.ENVIRONMENTAL_RISKS) ?: arrayListOf()
        val notes = intent.getStringExtra(IntentKeys.NOTES) ?: ""

        setContent {
            StructureScanLoadingScreen(
                onAnalysisComplete = {
                    // ✅ Pass all data to AssessmentResultsActivity
                    val next = Intent(this, AssessmentResultsActivity::class.java).apply {
                        // Images and assessment name
                        putStringArrayListExtra(IntentKeys.CAPTURED_IMAGES, ArrayList(capturedImages))
                        putStringArrayListExtra(IntentKeys.FINAL_IMAGES, ArrayList(finalImages))
                        putExtra(IntentKeys.ASSESSMENT_NAME, assessmentName)

                        // Building information
                        putExtra(IntentKeys.BUILDING_TYPE, buildingType)
                        putExtra(IntentKeys.CONSTRUCTION_YEAR, constructionYear)
                        putExtra(IntentKeys.RENOVATION_YEAR, renovationYear)
                        putExtra(IntentKeys.FLOORS, floors)
                        putExtra(IntentKeys.MATERIAL, material)
                        putExtra(IntentKeys.FOUNDATION, foundation)
                        putExtra(IntentKeys.ENVIRONMENT, environment)
                        putStringArrayListExtra(IntentKeys.PREVIOUS_ISSUES, ArrayList(previousIssues))
                        putExtra(IntentKeys.OCCUPANCY, occupancy)
                        putStringArrayListExtra(IntentKeys.ENVIRONMENTAL_RISKS, ArrayList(environmentalRisks))
                        putExtra(IntentKeys.NOTES, notes)
                    }
                    startActivity(next)
                    finish()
                }
            )
        }
    }
}

/**
 * Main loading screen composable that displays animated progress
 * This simulates AI analysis with visual feedback to the user
 *
 * @param onAnalysisComplete Callback function called when loading animation completes
 */
@Composable
fun StructureScanLoadingScreen(
    onAnalysisComplete: () -> Unit
) {
    // State variables to track progress and current step
    var currentProgress by remember { mutableStateOf(0f) } // Progress from 0.0 to 1.0
    var currentStep by remember { mutableStateOf(0) }      // Current step index (0-3)

    // List of steps to display during analysis
    val steps = listOf(
        "Image Upload Complete",
        "Detecting Structural Elements",
        "AI Analysis in Progress",
        "Generating Assessment Report"
    )

    // LaunchedEffect runs once when composable is first created
    // This simulates the AI analysis process with animated progress
    LaunchedEffect(Unit) {
        repeat(4) { step ->
            currentStep = step
            // Animate progress for each step (25% per step)
            repeat(25) { progress ->
                currentProgress = (step * 25f + progress) / 100f
                delay(80) // Controls how fast progress animates (80ms per 1%)
            }
            delay(800) // Pause between steps (800ms = 0.8 seconds)
        }
        // Analysis complete - navigate to results after small delay
        delay(500)
        onAnalysisComplete()
    }

    // Main container with gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A), // Dark blue at top
                        Color(0xFF3B82F6)  // Lighter blue at bottom
                    )
                )
            )
            .padding(24.dp), // Add padding around entire screen
        contentAlignment = Alignment.Center // Center all content
    ) {
        // Main content column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp) // 32dp space between elements
        ) {

            // App Logo and Title Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular progress indicator with icon in center
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated circular progress ring
                    CircularProgressIndicator(
                        progress = { currentProgress }, // Shows current analysis progress
                        modifier = Modifier.size(120.dp),
                        color = Color.White,
                        strokeWidth = 4.dp,
                    )

                    // Icon in center of progress circle
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // App name
                Text(
                    text = "StructureScan",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                // App description
                Text(
                    text = "AI-Powered Building Assessment",
                    color = Color.White.copy(alpha = 0.8f), // Slightly transparent white
                    fontSize = 16.sp
                )
            }

            // Status messages section
            Text(
                text = "Analyzing Your Structure",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Our AI is processing your images...",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            // Progress percentage display
            Text(
                text = "Processing... ${(currentProgress * 100).toInt()}%",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // List of analysis steps with progress indicators
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(steps) { index, step ->
                    StepIndicator(
                        step = step,
                        isCompleted = index < currentStep,    // Steps before current are completed
                        isCurrentStep = index == currentStep, // Highlight current step
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom instruction text
            Text(
                text = "Please wait while we analyze your structure...",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Individual step indicator component that shows progress for each analysis step
 *
 * @param step The text description of this step
 * @param isCompleted Whether this step has been completed (shows green checkmark)
 * @param isCurrentStep Whether this is the currently active step (shows loading spinner)
 * @param modifier Modifier for styling the component
 */
@Composable
fun StepIndicator(
    step: String,
    isCompleted: Boolean,
    isCurrentStep: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,        // Align icon and text vertically
        horizontalArrangement = Arrangement.spacedBy(12.dp)    // 12dp space between icon and text
    ) {
        // Step status icon (circle with different states)
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = when {
                        isCompleted -> Color.Green                    // Green for completed steps
                        isCurrentStep -> Color.White                 // White for current step
                        else -> Color.White.copy(alpha = 0.3f)      // Transparent white for future steps
                    },
                    shape = CircleShape // Make it circular
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                // Show checkmark for completed steps
                isCompleted -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Step completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                // Show loading spinner for current step
                isCurrentStep -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF1E3A8A), // Dark blue spinner
                        strokeWidth = 2.dp
                    )
                }
                // Empty circle for future steps (no content needed)
            }
        }

        // Step description text
        Text(
            text = step,
            color = when {
                isCompleted -> Color.White                    // Bright white for completed
                isCurrentStep -> Color.White                 // Bright white for current
                else -> Color.White.copy(alpha = 0.6f)      // Dim white for future steps
            },
            fontSize = 16.sp,
            fontWeight = if (isCurrentStep) FontWeight.Medium else FontWeight.Normal // Bold current step
        )
    }
}

// ========================================
// PREVIEW FUNCTIONS FOR DEVELOPMENT
// ========================================

/**
 * Preview of the complete loading screen
 * Use this to see how the screen looks in Android Studio design view
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StructureScanLoadingScreenPreview() {
    StructureScanLoadingScreen(
        onAnalysisComplete = {
            // Empty function for preview - no actual navigation happens
        }
    )
}

/**
 * Preview of individual step indicators in different states
 * Useful for testing the step indicator appearance
 */
@Preview(showBackground = true)
@Composable
fun StepIndicatorPreview() {
    Column(
        modifier = Modifier
            .background(Color(0xFF1E3A8A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Show all possible states of step indicators
        StepIndicator(
            step = "Image Upload Complete",
            isCompleted = true,      // Completed step (green checkmark)
            isCurrentStep = false,
            modifier = Modifier.fillMaxWidth()
        )

        StepIndicator(
            step = "Detecting Structural Elements",
            isCompleted = false,
            isCurrentStep = true,    // Current step (loading spinner)
            modifier = Modifier.fillMaxWidth()
        )

        StepIndicator(
            step = "AI Analysis in Progress",
            isCompleted = false,
            isCurrentStep = false,   // Future step (empty circle)
            modifier = Modifier.fillMaxWidth()
        )
    }
}