package com.example.structurescan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.DataType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.example.structurescan.ml.ModelUnquant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.example.structurescan.Utils.PdfReportGenerator
import com.example.structurescan.Utils.PdfAssessmentData

data class DetectedIssue(
    val damageType: String,
    val damageLevel: String,
    val confidence: Float
)

data class ImageAssessment(
    val imageUri: Uri,
    val damageType: String,
    val damageLevel: String,
    val confidence: Float,
    val crackLowConf: Float,
    val crackModerateConf: Float,
    val crackHighConf: Float,
    val paintLowConf: Float,
    val paintModerateConf: Float,
    val paintHighConf: Float,
    val algaeLowConf: Float = 0f,
    val algaeModerateConf: Float = 0f,
    val algaeHighConf: Float = 0f,
    val firebaseImageUrl: String = "",
    val detectedIssues: List<DetectedIssue> = emptyList(),
    val imageRisk: String = "Low"
)

data class AssessmentSummary(
    val overallRisk: String,
    val totalIssues: Int,
    val crackHighCount: Int,
    val crackModerateCount: Int,
    val crackLowCount: Int,
    val paintHighCount: Int,
    val paintModerateCount: Int,
    val paintLowCount: Int,
    val algaeHighCount: Int,
    val algaeModerateCount: Int,
    val algaeLowCount: Int,
    val assessments: List<ImageAssessment>
)

data class DamageRecommendation(
    val title: String,
    val description: String,
    val severity: String,
    val actions: List<String>,
    val severityColor: Color,
    val severityBgColor: Color
)

class AssessmentResultsActivity : ComponentActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var currentAssessmentId: String? = null

    // Place these at the top of your activity, just after `onCreate`
    lateinit var currentAssessmentName: MutableState<String>
    lateinit var currentBuildingType: MutableState<String>
    lateinit var currentConstructionYear: MutableState<String>
    lateinit var currentRenovationYear: MutableState<String>
    lateinit var currentFloors: MutableState<String>
    lateinit var currentMaterial: MutableState<String>
    lateinit var currentFoundation: MutableState<String>
    lateinit var currentEnvironment: MutableState<String>
    lateinit var currentPreviousIssues: MutableState<ArrayList<String>>
    lateinit var currentOccupancy: MutableState<String>
    lateinit var currentEnvironmentalRisks: MutableState<ArrayList<String>>
    lateinit var currentNotes: MutableState<String>

    // ✅ Activity result launcher to handle edits (final version)
    private val editBuildingInfoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data?.getBooleanExtra("UPDATED", false) == true) {
                lifecycleScope.launch {
                    try {
                        val userId = firebaseAuth.currentUser?.uid ?: return@launch
                        val updatedName = data.getStringExtra("assessmentName") ?: return@launch

                        val documents = firestore.collection("users")
                            .document(userId)
                            .collection("assessments")
                            .whereEqualTo("assessmentName", updatedName)
                            .get()
                            .await()

                        if (!documents.isEmpty) {
                            val doc = documents.documents[0]

                            // ✅ Directly update your mutable states (NO recreate, NO intent.putExtra)
                            currentAssessmentName.value = doc.getString("assessmentName") ?: ""
                            currentBuildingType.value = doc.getString("buildingType") ?: ""
                            currentConstructionYear.value = doc.getString("constructionYear") ?: ""
                            currentRenovationYear.value = doc.getString("renovationYear") ?: ""
                            currentFloors.value = doc.getString("floors") ?: ""
                            currentMaterial.value = doc.getString("material") ?: ""
                            currentFoundation.value = doc.getString("foundation") ?: ""
                            currentEnvironment.value = doc.getString("environment") ?: ""
                            currentPreviousIssues.value = ArrayList((doc.get("previousIssues") as? List<*>)?.mapNotNull { it as? String } ?: emptyList())
                            currentOccupancy.value = doc.getString("occupancy") ?: ""
                            currentEnvironmentalRisks.value = ArrayList((doc.get("environmentalRisks") as? List<*>)?.mapNotNull { it as? String } ?: emptyList())
                            currentNotes.value = doc.getString("notes") ?: ""

                            Toast.makeText(this@AssessmentResultsActivity, "✓ Assessment info updated", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@AssessmentResultsActivity, "Error fetching updated data", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val capturedImages: ArrayList<Uri>? = intent.getParcelableArrayListExtra(IntentKeys.FINAL_IMAGES)
        val assessmentName = intent.getStringExtra(IntentKeys.ASSESSMENT_NAME) ?: "Unnamed Assessment"
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

        // Initialize mutable states here
        currentAssessmentName = mutableStateOf(assessmentName)
        currentBuildingType = mutableStateOf(buildingType)
        currentConstructionYear = mutableStateOf(constructionYear)
        currentRenovationYear = mutableStateOf(renovationYear)
        currentFloors = mutableStateOf(floors)
        currentMaterial = mutableStateOf(material)
        currentFoundation = mutableStateOf(foundation)
        currentEnvironment = mutableStateOf(environment)
        currentPreviousIssues = mutableStateOf(previousIssues)
        currentOccupancy = mutableStateOf(occupancy)
        currentEnvironmentalRisks = mutableStateOf(environmentalRisks)
        currentNotes = mutableStateOf(notes)

        setContent {
            MaterialTheme {
                AssessmentResultsScreen(
                    capturedImages = capturedImages ?: emptyList(),
                    assessmentName = currentAssessmentName.value,
                    buildingType = currentBuildingType.value,
                    constructionYear = currentConstructionYear.value,
                    renovationYear = currentRenovationYear.value,
                    floors = currentFloors.value,
                    material = currentMaterial.value,
                    foundation = currentFoundation.value,
                    environment = currentEnvironment.value,
                    previousIssues = currentPreviousIssues.value,
                    occupancy = currentOccupancy.value,
                    environmentalRisks = currentEnvironmentalRisks.value,
                    notes = currentNotes.value,
                    onSaveToFirebase = { summary ->
                        saveAssessmentToFirebase(
                            assessmentName, summary, buildingType, constructionYear,
                            renovationYear, floors, material, foundation, environment,
                            previousIssues, occupancy, environmentalRisks, notes
                        )
                    },
                    onEditBuildingInfo = {
                        val intent = Intent(this, EditBuildingInfoActivity::class.java).apply {
                            putExtra("assessmentName", currentAssessmentName.value)
                            putExtra(IntentKeys.BUILDING_TYPE, currentBuildingType.value)
                            putExtra(IntentKeys.CONSTRUCTION_YEAR, currentConstructionYear.value)
                            putExtra(IntentKeys.RENOVATION_YEAR, currentRenovationYear.value)
                            putExtra(IntentKeys.FLOORS, currentFloors.value)
                            putExtra(IntentKeys.MATERIAL, currentMaterial.value)
                            putExtra(IntentKeys.FOUNDATION, currentFoundation.value)
                            putExtra(IntentKeys.ENVIRONMENT, currentEnvironment.value)
                            putStringArrayListExtra(IntentKeys.PREVIOUS_ISSUES, currentPreviousIssues.value)
                            putExtra(IntentKeys.OCCUPANCY, currentOccupancy.value)
                            putStringArrayListExtra(IntentKeys.ENVIRONMENTAL_RISKS, currentEnvironmentalRisks.value)
                            putExtra(IntentKeys.NOTES, currentNotes.value)
                        }
                        editBuildingInfoLauncher.launch(intent)
                    }
                )
            }
        }
    }

    private suspend fun uploadImageToStorage(imageUri: Uri, userId: String, assessmentId: String, imageIndex: Int): String? {
        return try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap == null) return null

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val imageData = baos.toByteArray()
            bitmap.recycle()

            val storageRef = storage.reference.child("users/$userId/assessments/$assessmentId/image_$imageIndex.jpg")
            storageRef.putBytes(imageData).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("FirebaseUpload", "uploadImageToStorage failed", e)
            null
        }
    }

    fun saveAssessmentToFirebase(
        assessmentName: String, summary: AssessmentSummary, buildingType: String, constructionYear: String,
        renovationYear: String, floors: String, material: String, foundation: String, environment: String,
        previousIssues: List<String>, occupancy: String, environmentalRisks: List<String>, notes: String,
        isReanalysis: Boolean = false  // ✅ ADD THIS PARAMETER
    ): Boolean {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            return false
        }
        val userId = currentUser.uid

        return try {
            // ✅ MODIFIED: Reuse existing ID if re-analyzing, otherwise create new
            val assessmentId = if (isReanalysis && currentAssessmentId != null) {
                currentAssessmentId!!
            } else {
                UUID.randomUUID().toString().also { currentAssessmentId = it }
            }

            val uploadedAssessments = mutableListOf<HashMap<String, Any>>()

            summary.assessments.forEachIndexed { index, assessment ->
                val firebaseImageUrl = kotlinx.coroutines.runBlocking {
                    uploadImageToStorage(assessment.imageUri, userId, assessmentId, index)
                } ?: throw Exception("Failed to upload image ${index + 1}")

                val recommendationsForImage = assessment.detectedIssues.map { issue ->
                    val rec = getRecommendation(issue.damageType, issue.damageLevel)
                    hashMapOf(
                        "title" to rec.title,
                        "description" to rec.description,
                        "severity" to rec.severity,
                        "actions" to rec.actions
                    )
                }

                uploadedAssessments.add(hashMapOf(
                    "damageType" to assessment.damageType,
                    "damageLevel" to assessment.damageLevel,
                    "confidence" to assessment.confidence,
                    "crackLowConf" to assessment.crackLowConf,
                    "crackModerateConf" to assessment.crackModerateConf,
                    "crackHighConf" to assessment.crackHighConf,
                    "paintLowConf" to assessment.paintLowConf,
                    "paintModerateConf" to assessment.paintModerateConf,
                    "paintHighConf" to assessment.paintHighConf,
                    "algaeLowConf" to assessment.algaeLowConf,
                    "algaeModerateConf" to assessment.algaeModerateConf,
                    "algaeHighConf" to assessment.algaeHighConf,
                    "imageUri" to firebaseImageUrl,
                    "localImageUri" to assessment.imageUri.toString(),
                    "detectedIssues" to assessment.detectedIssues.map {
                        mapOf("type" to it.damageType, "level" to it.damageLevel, "confidence" to it.confidence)
                    },
                    "imageRisk" to assessment.imageRisk,
                    "recommendations" to recommendationsForImage
                ))
            }

            val assessmentData = hashMapOf(
                "assessmentId" to assessmentId,
                "assessmentName" to assessmentName,
                "userId" to userId,
                "timestamp" to System.currentTimeMillis(),
                "date" to SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date()),
                "overallRisk" to summary.overallRisk,
                "totalIssues" to summary.totalIssues,
                "crackHighCount" to summary.crackHighCount,
                "crackModerateCount" to summary.crackModerateCount,
                "crackLowCount" to summary.crackLowCount,
                "paintHighCount" to summary.paintHighCount,
                "paintModerateCount" to summary.paintModerateCount,
                "paintLowCount" to summary.paintLowCount,
                "algaeHighCount" to summary.algaeHighCount,
                "algaeModerateCount" to summary.algaeModerateCount,
                "algaeLowCount" to summary.algaeLowCount,
                "imageCount" to summary.assessments.size,
                "buildingType" to buildingType,
                "constructionYear" to constructionYear,
                "renovationYear" to renovationYear,
                "floors" to floors,
                "material" to material,
                "foundation" to foundation,
                "environment" to environment,
                "previousIssues" to previousIssues,
                "occupancy" to occupancy,
                "environmentalRisks" to environmentalRisks,
                "notes" to notes,
                "assessments" to uploadedAssessments
            )

            kotlinx.coroutines.runBlocking {
                firestore.collection("users").document(userId).collection("assessments").document(assessmentId).set(assessmentData).await()
            }
            true
        } catch (e: Exception) {
            Log.e("FirebaseUpload", "Upload failed", e)
            false
        }
    }

    fun getRecommendation(damageType: String, damageLevel: String): DamageRecommendation {
        val key = "$damageType-$damageLevel"
        return when (key) {
            "Crack-High" -> DamageRecommendation("Structural Crack", "Vertical crack in foundation area indicating possible settlement.", "HIGH",
                listOf("Consult structural engineer within 30 days", "Monitor crack progression weekly"), Color(0xFFD32F2F), Color(0xFFFFEBEE))
            "Crack-Moderate" -> DamageRecommendation("Thermal Cracking", "Hairline cracks from thermal expansion.", "MODERATE",
                listOf("Monitor during seasonal changes", "Seal cracks to prevent water infiltration"), Color(0xFFF57C00), Color(0xFFFFF3E0))
            "Crack-Low" -> DamageRecommendation("Surface Wear", "Minimal wear consistent with age.", "LOW",
                listOf("Regular building maintenance", "Monitor for progression during routine inspections"), Color(0xFF388E3C), Color(0xFFE8F5E9))
            "Paint-High" -> DamageRecommendation("Severe Paint Deterioration", "Extensive peeling; possible moisture issues.", "HIGH",
                listOf("Check moisture/mold", "Strip to substrate", "Prime and repaint"), Color(0xFFD32F2F), Color(0xFFFFEBEE))
            "Paint-Moderate" -> DamageRecommendation("Paint Damage", "Moderate peeling/flaking.", "MODERATE",
                listOf("Scrape and clean", "Prime and repaint", "Investigate moisture source"), Color(0xFFF57C00), Color(0xFFFFF3E0))
            "Paint-Low" -> DamageRecommendation("Minor Paint Wear", "Cosmetic wear.", "LOW",
                listOf("Clean surface", "Touch-up paint as needed", "Schedule repainting during regular maintenance"), Color(0xFF388E3C), Color(0xFFE8F5E9))
            "Algae-High" -> DamageRecommendation("Severe Algae/Moss", "Heavy growth indicating persistent moisture.", "HIGH",
                listOf("Professional biocide cleaning", "Fix moisture source", "Improve drainage"), Color(0xFFD32F2F), Color(0xFFFFEBEE))
            "Algae-Moderate" -> DamageRecommendation("Moderate Algae/Moss", "Visible growth in damp areas.", "MODERATE",
                listOf("Clean affected areas", "Improve drainage", "Increase sunlight exposure"), Color(0xFFF57C00), Color(0xFFFFF3E0))
            "Algae-Low" -> DamageRecommendation("Minor Algae/Moss", "Light growth in shaded areas.", "LOW",
                listOf("Periodic cleaning", "Enhance air circulation", "Monitor growth patterns"), Color(0xFF388E3C), Color(0xFFE8F5E9))
            else -> DamageRecommendation("General Surface Issue", "Condition requires attention.", "MODERATE",
                listOf("Inspect closely", "Address moisture", "Schedule professional assessment"), Color(0xFFF57C00), Color(0xFFFFF3E0))
        }
    }
}

// Helper function to get confidence bar color based on severity level
fun getConfidenceColor(damageLevel: String): Color {
    return when (damageLevel) {
        "High" -> Color(0xFFD32F2F) // Red
        "Moderate" -> Color(0xFFF57C00) // Orange
        else -> Color(0xFF2E7D32) // Green
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentResultsScreen(
    capturedImages: List<Uri>,
    assessmentName: String = "Unnamed Assessment",
    buildingType: String = "",
    constructionYear: String = "",
    renovationYear: String = "",
    floors: String = "",
    material: String = "",
    foundation: String = "",
    environment: String = "",
    previousIssues: List<String> = emptyList(),
    occupancy: String = "",
    environmentalRisks: List<String> = emptyList(),
    notes: String = "",
    onSaveToFirebase: (AssessmentSummary) -> Boolean = { false },
    onEditBuildingInfo: () -> Unit = {} // ✅ ADD: Callback for editing
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isAnalyzing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var assessmentSummary by remember { mutableStateOf<AssessmentSummary?>(null) }
    var analysisError by remember { mutableStateOf<String?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isSavedToFirebase by remember { mutableStateOf(false) }
    var showReanalyzeDialog by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageViewer by remember { mutableStateOf(false) }

    val SHOW_THRESHOLD = 0.30f

    // ✅ SINGLE BackHandler - blocks during saving, navigates to Dashboard otherwise
    BackHandler(enabled = true) {
        if (isSaving) {
            // Do nothing - block back button during saving
        } else {
            // Navigate to Dashboard when back is pressed
            val intent = Intent(context, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            (context as? ComponentActivity)?.finish()
        }
    }

    // FIXED: When values are equal, always prefer higher severity
    fun selectLevelFromConfidences(low: Float, moderate: Float, high: Float): Pair<String, Float> {
        return when {
            high >= moderate && high >= low -> "High" to high
            moderate >= low -> "Moderate" to moderate
            else -> "Low" to low
        }
    }

    fun analyzeImageWithTensorFlow(imageUri: Uri): ImageAssessment? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (originalBitmap == null) throw Exception("Failed to load image")

            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 224, 224, true)
            val byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3).apply { order(ByteOrder.nativeOrder()) }

            val intValues = IntArray(224 * 224)
            resizedBitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)
            var pixel = 0
            for (i in 0 until 224) {
                for (j in 0 until 224) {
                    val value = intValues[pixel++]
                    byteBuffer.putFloat(((value shr 16) and 0xFF) / 255.0f)
                    byteBuffer.putFloat(((value shr 8) and 0xFF) / 255.0f)
                    byteBuffer.putFloat((value and 0xFF) / 255.0f)
                }
            }

            val model = ModelUnquant.newInstance(context)
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            inputFeature0.loadBuffer(byteBuffer)
            val confidences = model.process(inputFeature0).outputFeature0AsTensorBuffer.floatArray

            val algaeHigh = confidences.getOrNull(0) ?: 0f
            val algaeLow = confidences.getOrNull(1) ?: 0f
            val algaeMod = confidences.getOrNull(2) ?: 0f
            val crackHigh = confidences.getOrNull(3) ?: 0f
            val crackLow = confidences.getOrNull(4) ?: 0f
            val crackMod = confidences.getOrNull(5) ?: 0f
            val paintHigh = confidences.getOrNull(6) ?: 0f
            val paintLow = confidences.getOrNull(7) ?: 0f
            val paintMod = confidences.getOrNull(8) ?: 0f

            val maxIndex = confidences.indices.maxByOrNull { confidences[it] } ?: 0
            val maxConfidence = confidences.getOrNull(maxIndex) ?: 0f

            val (primaryDamageType, primaryDamageLevel) = when (maxIndex) {
                0 -> "Algae" to "High"; 1 -> "Algae" to "Low"; 2 -> "Algae" to "Moderate"
                3 -> "Crack" to "High"; 4 -> "Crack" to "Low"; 5 -> "Crack" to "Moderate"
                6 -> "Paint" to "High"; 7 -> "Paint" to "Low"; 8 -> "Paint" to "Moderate"
                else -> "Crack" to "Low"
            }

            val detectedIssuesMutable = mutableListOf<DetectedIssue>()

            val (algaeLevel, algaeBest) = selectLevelFromConfidences(algaeLow, algaeMod, algaeHigh)
            if (algaeBest > SHOW_THRESHOLD) detectedIssuesMutable.add(DetectedIssue("Algae", algaeLevel, algaeBest))

            val (crackLevel, crackBest) = selectLevelFromConfidences(crackLow, crackMod, crackHigh)
            if (crackBest > SHOW_THRESHOLD) detectedIssuesMutable.add(DetectedIssue("Crack", crackLevel, crackBest))

            val (paintLevel, paintBest) = selectLevelFromConfidences(paintLow, paintMod, paintHigh)
            if (paintBest > SHOW_THRESHOLD) detectedIssuesMutable.add(DetectedIssue("Paint", paintLevel, paintBest))

            val imageRisk = when {
                detectedIssuesMutable.any { it.damageLevel == "High" } -> "High"
                detectedIssuesMutable.any { it.damageLevel == "Moderate" } -> "Moderate"
                detectedIssuesMutable.isNotEmpty() -> "Low"
                else -> "Low"
            }

            model.close()
            originalBitmap.recycle()
            resizedBitmap.recycle()

            ImageAssessment(imageUri, primaryDamageType, primaryDamageLevel, maxConfidence,
                crackLow, crackMod, crackHigh, paintLow, paintMod, paintHigh,
                algaeLow, algaeMod, algaeHigh, "", detectedIssuesMutable.toList(), imageRisk)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ✅ MODIFIED: Only run once on initial load
    LaunchedEffect(Unit) { // Changed from LaunchedEffect(capturedImages)
        if (capturedImages.isNotEmpty() && !isSavedToFirebase) {
            isAnalyzing = true
            analysisError = null
            try {
                val imageAssessments = capturedImages.mapNotNull { analyzeImageWithTensorFlow(it) }
                if (imageAssessments.isEmpty()) {
                    analysisError = "Failed to analyze images. Please try again."
                } else {
                    var crackHighCount = 0; var crackModerateCount = 0; var crackLowCount = 0
                    var paintHighCount = 0; var paintModerateCount = 0; var paintLowCount = 0
                    var algaeHighCount = 0; var algaeModerateCount = 0; var algaeLowCount = 0

                    imageAssessments.forEach { ia ->
                        ia.detectedIssues.forEach { issue ->
                            when (issue.damageType) {
                                "Crack" -> when (issue.damageLevel) {
                                    "High" -> crackHighCount++; "Moderate" -> crackModerateCount++; else -> crackLowCount++
                                }
                                "Paint" -> when (issue.damageLevel) {
                                    "High" -> paintHighCount++; "Moderate" -> paintModerateCount++; else -> paintLowCount++
                                }
                                "Algae" -> when (issue.damageLevel) {
                                    "High" -> algaeHighCount++; "Moderate" -> algaeModerateCount++; else -> algaeLowCount++
                                }
                            }
                        }
                    }

                    val overallRisk = when {
                        imageAssessments.any { it.imageRisk == "High" } -> "High Risk"
                        imageAssessments.any { it.imageRisk == "Moderate" } -> "Moderate Risk"
                        imageAssessments.any { it.imageRisk == "Low" } -> "Low Risk"
                        else -> "Low Risk"
                    }

                    val totalIssues = crackHighCount + crackModerateCount + crackLowCount +
                            paintHighCount + paintModerateCount + paintLowCount +
                            algaeHighCount + algaeModerateCount + algaeLowCount

                    val summary = AssessmentSummary(overallRisk, totalIssues,
                        crackHighCount, crackModerateCount, crackLowCount,
                        paintHighCount, paintModerateCount, paintLowCount,
                        algaeHighCount, algaeModerateCount, algaeLowCount, imageAssessments)

                    isAnalyzing = false

                    // Now save to Firebase
                    isSaving = true
                    withContext(Dispatchers.IO) {
                        val success = onSaveToFirebase(summary)
                        withContext(Dispatchers.Main) {
                            isSaving = false
                            if (success) {
                                assessmentSummary = summary
                                isSavedToFirebase = true
                                Toast.makeText(context, "✓ Assessment saved successfully!", Toast.LENGTH_LONG).show()
                            } else {
                                analysisError = "Failed to save assessment to database"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                analysisError = "Analysis failed: ${e.message}"
                isAnalyzing = false
            }
        }
    }

    // Loading Dialog
    if (isAnalyzing || isSaving) {
        Dialog(
            onDismissRequest = { /* Prevent dismissal */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = Color(0xFF0288D1),
                        strokeWidth = 5.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (isAnalyzing) "Analyzing images..." else "Saving assessment to database",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Please be patient, this may take a moment",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF9FAFB))) {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (!isSaving) {
                                    val intent = Intent(context, DashboardActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                    (context as? ComponentActivity)?.finish()
                                }
                            }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black) }
                        }
                        Text("Assessment Results", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0288D1))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(assessmentName, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    Text(SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date()), fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
                }

                when {
                    analysisError != null -> {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFD32F2F), modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Analysis Error", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD32F2F))
                                Text(analysisError!!, fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                    assessmentSummary != null -> {
                        val summary = assessmentSummary!!
                        val riskColor = when (summary.overallRisk) {
                            "High Risk" -> Color(0xFFD32F2F)
                            "Moderate Risk" -> Color(0xFFF57C00)
                            else -> Color(0xFF388E3C)
                        }

                        Column(modifier = Modifier.fillMaxWidth().border(1.dp, riskColor, RoundedCornerShape(12.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.background(riskColor, RoundedCornerShape(20.dp)).padding(horizontal = 24.dp, vertical = 8.dp)) {
                                Text(summary.overallRisk, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = buildString {
                                    append("Analysis of ${summary.assessments.size} images completed. ")
                                    if (summary.totalIssues > 0) append("${summary.totalIssues} areas of concern detected.") else append("No significant issues detected.")
                                },
                                fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center
                            )
                        }

                            Spacer(Modifier.height(20.dp))

                            // ✅ ALWAYS show Building Information section
                            ExpandableSection(title = "Building Information", trailingContent = {
                                IconButton(onClick = onEditBuildingInfo) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit",
                                        tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                                }
                            }) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        val hasBuildingInfo = buildingType.isNotEmpty() || constructionYear.isNotEmpty() ||
                                                renovationYear.isNotEmpty() || floors.isNotEmpty() || material.isNotEmpty() ||
                                                foundation.isNotEmpty() || environment.isNotEmpty() || previousIssues.isNotEmpty() ||
                                                occupancy.isNotEmpty() || environmentalRisks.isNotEmpty() || notes.isNotEmpty()

                                        if (!hasBuildingInfo) {
                                            // ✅ Show friendly message when no building info provided
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = "No info",
                                                    tint = Color(0xFF9CA3AF),
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(Modifier.height(12.dp))
                                                Text(
                                                    "No Building Information Available",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF374151),
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    "Tap the edit icon above to add details about your structure for more accurate assessments.",
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF6B7280),
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 20.sp
                                                )
                                            }
                                        } else {
                                            // ✅ Show building info when available
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                                            ) {
                                                if (buildingType.isNotEmpty()) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text("TYPE", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                                        Spacer(Modifier.height(6.dp))
                                                        Text(buildingType, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                                if (floors.isNotEmpty()) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text("FLOORS", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                                        Spacer(Modifier.height(6.dp))
                                                        Text(floors.replace(" Floor", "").replace(" Floors", ""), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                                            ) {
                                                if (material.isNotEmpty()) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text("MATERIAL", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                                        Spacer(Modifier.height(6.dp))
                                                        Text(material, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                                if (constructionYear.isNotEmpty()) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text("BUILT", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                                        Spacer(Modifier.height(6.dp))
                                                        Text(constructionYear, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }

                                            if (renovationYear.isNotEmpty()) BuildingInfoRow("Last Renovation", renovationYear.ifEmpty { "Never" })
                                            if (foundation.isNotEmpty()) BuildingInfoRow("Foundation Type", foundation)
                                            if (environment.isNotEmpty()) BuildingInfoRow("Environment", environment)
                                            if (occupancy.isNotEmpty()) BuildingInfoRow("Occupancy Level", occupancy)
                                            if (previousIssues.isNotEmpty()) BuildingInfoRow("Previous Issues", previousIssues.joinToString(", ").ifEmpty { "None" })
                                            if (environmentalRisks.isNotEmpty()) BuildingInfoRow("Environmental Risk", environmentalRisks.firstOrNull() ?: "None")

                                            if (notes.isNotEmpty()) {
                                                HorizontalDivider(color = Color(0xFFE0E0E0), modifier = Modifier.padding(vertical = 12.dp))
                                                Text("Additional Notes", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                                Spacer(Modifier.height(8.dp))
                                                Text(notes, fontSize = 13.sp, color = Color.Black, lineHeight = 20.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Detection Summary", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                                if (summary.crackHighCount + summary.crackModerateCount + summary.crackLowCount > 0) SummaryRow("Crack Damage:", "High: ${summary.crackHighCount}  Mod: ${summary.crackModerateCount}  Low: ${summary.crackLowCount}")
                                if (summary.paintHighCount + summary.paintModerateCount + summary.paintLowCount > 0) SummaryRow("Paint Damage:", "High: ${summary.paintHighCount}  Mod: ${summary.paintModerateCount}  Low: ${summary.paintLowCount}")
                                if (summary.algaeHighCount + summary.algaeModerateCount + summary.algaeLowCount > 0) SummaryRow("Algae/Moss:", "High: ${summary.algaeHighCount}  Mod: ${summary.algaeModerateCount}  Low: ${summary.algaeLowCount}")
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        ExpandableSection(title = "Recommendations", trailingContent = {
                            Box(modifier = Modifier.background(Color(0xFF6366F1), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("${summary.totalIssues} Issues", fontSize = 12.sp, color = Color.White)
                            }
                        }) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Color Legend
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Confidence Level Legend:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                        Spacer(Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(16.dp).background(Color(0xFFD32F2F), RoundedCornerShape(2.dp)))
                                                Spacer(Modifier.width(6.dp))
                                                Text("High", fontSize = 12.sp, color = Color.Gray)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(16.dp).background(Color(0xFFF57C00), RoundedCornerShape(2.dp)))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Moderate", fontSize = 12.sp, color = Color.Gray)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(16.dp).background(Color(0xFF2E7D32), RoundedCornerShape(2.dp)))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Low", fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                // ✅ NEW: Group identical issues and merge them
                                val allIssues = summary.assessments.flatMap { it.detectedIssues }

                                if (allIssues.isEmpty()) {
                                    Card(modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(0.dp)) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "No Significant Issues", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                Box(modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                    Text(text = "GOOD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Text("This area appears to be in good condition.", fontSize = 13.sp, color = Color.Gray)
                                        }
                                    }
                                } else {
                                    // Group by "DamageType-DamageLevel" key
                                    val groupedIssues = allIssues.groupBy { "${it.damageType}-${it.damageLevel}" }

                                    groupedIssues.forEach { (key, issuesList) ->
                                        val firstIssue = issuesList.first()
                                        val averageConfidence = issuesList.map { it.confidence }.average().toFloat()
                                        val locationCount = issuesList.size

                                        val rec = (context as AssessmentResultsActivity).getRecommendation(
                                            firstIssue.damageType,
                                            firstIssue.damageLevel
                                        )

                                        MergedRecommendationCard(
                                            recommendation = rec,
                                            averageConfidence = averageConfidence,
                                            locationCount = locationCount,
                                            damageLevel = firstIssue.damageLevel
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        ExpandableSection(title = "AI Analysis Results", trailingContent = {
                            Box(modifier = Modifier.background(Color(0xFF6366F1), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("${summary.assessments.size} Images", fontSize = 12.sp, color = Color.White)
                            }
                        }) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                summary.assessments.forEachIndexed { index, assessment ->
                                    AnalyzedImageCardV2(imageNumber = index + 1, assessment = assessment, onImageClick = {
                                        selectedImageUri = assessment.imageUri
                                        showImageViewer = true
                                    })
                                    if (index < summary.assessments.size - 1) HorizontalDivider(color = Color(0xFFE0E0E0))
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = {
                                showReanalyzeDialog = true
                            }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Re-analyze", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Re-analyze", fontSize = 14.sp)
                            }

                            OutlinedButton(onClick = { showDownloadDialog = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Download", fontSize = 14.sp)
                            }
                        }

                        // ✅ Re-analyze confirmation dialog
                        if (showReanalyzeDialog) {
                            AlertDialog(
                                onDismissRequest = { showReanalyzeDialog = false },
                                icon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(48.dp))
                                },
                                title = {
                                    Text("Re-analyze Images?", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                },
                                text = {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text("This will:", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("• Re-run AI analysis on all ${summary.assessments.size} images", fontSize = 13.sp, color = Color(0xFF6B7280))
                                        Text("• Generate new detection results", fontSize = 13.sp, color = Color(0xFF6B7280))
                                        Text("• Update the saved assessment", fontSize = 13.sp, color = Color(0xFF6B7280))
                                        Text("• Keep assessment name and building info", fontSize = 13.sp, color = Color(0xFF6B7280))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Previous results will be overwritten.", fontSize = 13.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Medium)
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showReanalyzeDialog = false
                                            coroutineScope.launch {
                                                isAnalyzing = true
                                                val imageAssessments = capturedImages.mapNotNull { analyzeImageWithTensorFlow(it) }
                                                if (imageAssessments.isEmpty()) {
                                                    isAnalyzing = false
                                                    analysisError = "Failed to re-analyze images"
                                                } else {
                                                    var crackHigh = 0; var crackMod = 0; var crackLow = 0
                                                    var paintHigh = 0; var paintMod = 0; var paintLow = 0
                                                    var algaeHigh = 0; var algaeMod = 0; var algaeLow = 0
                                                    imageAssessments.forEach { ia ->
                                                        ia.detectedIssues.forEach { issue ->
                                                            when (issue.damageType) {
                                                                "Crack" -> when (issue.damageLevel) {
                                                                    "High" -> crackHigh++; "Moderate" -> crackMod++; else -> crackLow++
                                                                }
                                                                "Paint" -> when (issue.damageLevel) {
                                                                    "High" -> paintHigh++; "Moderate" -> paintMod++; else -> paintLow++
                                                                }
                                                                "Algae" -> when (issue.damageLevel) {
                                                                    "High" -> algaeHigh++; "Moderate" -> algaeMod++; else -> algaeLow++
                                                                }
                                                            }
                                                        }
                                                    }
                                                    val newRisk = when {
                                                        imageAssessments.any { it.imageRisk == "High" } -> "High Risk"
                                                        imageAssessments.any { it.imageRisk == "Moderate" } -> "Moderate Risk"
                                                        else -> "Low Risk"
                                                    }
                                                    val totalIssues = crackHigh + crackMod + crackLow + paintHigh + paintMod + paintLow + algaeHigh + algaeMod + algaeLow
                                                    val newSummary = AssessmentSummary(newRisk, totalIssues, crackHigh, crackMod, crackLow, paintHigh, paintMod, paintLow, algaeHigh, algaeMod, algaeLow, imageAssessments)
                                                    isAnalyzing = false
                                                    isSaving = true
                                                    val success = withContext(Dispatchers.IO) {
                                                        (context as AssessmentResultsActivity).saveAssessmentToFirebase(
                                                            assessmentName, newSummary, buildingType, constructionYear,
                                                            renovationYear, floors, material, foundation, environment,
                                                            previousIssues, occupancy, environmentalRisks, notes,
                                                            isReanalysis = true  // ✅ THIS IS THE KEY!
                                                        )
                                                    }
                                                    isSaving = false
                                                    if (success) {
                                                        assessmentSummary = newSummary
                                                        Toast.makeText(context, "✓ Re-analysis complete! Results updated.", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        analysisError = "Failed to save re-analyzed results"
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Re-analyze Now")
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(
                                        onClick = { showReanalyzeDialog = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(onClick = {
                            val intent = Intent(context, DashboardActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                            (context as? ComponentActivity)?.finish()
                        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))) {
                            Icon(Icons.Default.Home, contentDescription = "Dashboard", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Back to Dashboard", fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        if (showImageViewer && selectedImageUri != null) {
            FullScreenImageViewer(imageUri = selectedImageUri!!, onDismiss = { showImageViewer = false; selectedImageUri = null })
        }

        if (showDownloadDialog) {
            DownloadDialog(onDismiss = { showDownloadDialog = false }, onDownload = {
                coroutineScope.launch {
                    try {
                        val localImageUris = assessmentSummary?.assessments?.map { it.imageUri.toString() }.orEmpty()
                        val pdfData = PdfAssessmentData(
                            assessmentName = assessmentName,
                            date = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date()),
                            overallRisk = assessmentSummary?.overallRisk ?: "Unknown",
                            totalIssues = assessmentSummary?.totalIssues ?: 0,
                            crackHighCount = assessmentSummary?.crackHighCount ?: 0,
                            crackModerateCount = assessmentSummary?.crackModerateCount ?: 0,
                            crackLowCount = assessmentSummary?.crackLowCount ?: 0,
                            paintHighCount = assessmentSummary?.paintHighCount ?: 0,
                            paintModerateCount = assessmentSummary?.paintModerateCount ?: 0,
                            paintLowCount = assessmentSummary?.paintLowCount ?: 0,
                            algaeHighCount = assessmentSummary?.algaeHighCount ?: 0,
                            algaeModerateCount = assessmentSummary?.algaeModerateCount ?: 0,
                            algaeLowCount = assessmentSummary?.algaeLowCount ?: 0,
                            buildingType = buildingType, constructionYear = constructionYear, renovationYear = renovationYear,
                            floors = floors, material = material, foundation = foundation, environment = environment,
                            previousIssues = previousIssues.joinToString(", "), occupancy = occupancy,
                            environmentalRisks = environmentalRisks.joinToString(", "), notes = notes, imageUrls = localImageUris
                        )
                        val pdfPath = PdfReportGenerator.generatePdfReport(context, pdfData)
                        if (pdfPath != null) {
                            Toast.makeText(context, "✓ PDF saved to Downloads folder!", Toast.LENGTH_LONG).show()
                            showDownloadDialog = false
                        } else {
                            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                            showDownloadDialog = false
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        showDownloadDialog = false
                    }
                }
            })
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun BuildingInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black, textAlign = TextAlign.End, modifier = Modifier.weight(1.5f))
    }
}

@Composable
fun FullScreenImageViewer(imageUri: Uri, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() }) {
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Image(painter = rememberAsyncImagePainter(imageUri), contentDescription = "Full screen image", modifier = Modifier.fillMaxSize().clickable { onDismiss() }, contentScale = ContentScale.Fit)
        }
    }
}

@Composable
fun RecommendationCard(recommendation: DamageRecommendation, aiConfidence: Float, damageLevel: String) {
    Card(modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = recommendation.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.background(recommendation.severityBgColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text = recommendation.severity, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = recommendation.severityColor)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(recommendation.description, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("AI Confidence", fontSize = 12.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { aiConfidence },
                        modifier = Modifier.width(100.dp).height(6.dp),
                        color = getConfidenceColor(damageLevel),
                        trackColor = Color(0xFFE0E0E0)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${(aiConfidence * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Recommended Actions:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            recommendation.actions.forEach { action ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("•", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                    Text(action, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
fun AnalyzedImageCardV2(imageNumber: Int, assessment: ImageAssessment, onImageClick: () -> Unit = {}) {
    val issues = assessment.detectedIssues

    val imageOverallLevel = remember(issues) {
        when {
            issues.any { it.damageLevel == "High" } -> "High"
            issues.any { it.damageLevel == "Moderate" } -> "Moderate"
            issues.any { it.damageLevel == "Low" } -> "Low"
            else -> "Low"
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(80.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).clickable { onImageClick() }) {
            Image(painter = rememberAsyncImagePainter(assessment.imageUri), contentDescription = "Analyzed image $imageNumber", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Image $imageNumber", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                LevelPill(level = imageOverallLevel)
            }
            Spacer(Modifier.height(6.dp))

            if (issues.isEmpty()) {
                Text("No issues above 30% detected.", fontSize = 12.sp, color = Color.Gray)
            } else {
                issues.sortedByDescending { it.confidence }.forEach { issue ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("${issue.damageType} Damage", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { issue.confidence.coerceIn(0f, 1f) },
                                modifier = Modifier.weight(1f).height(8.dp),
                                color = getConfidenceColor(issue.damageLevel),
                                trackColor = Color(0xFFE0E0E0)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${(issue.confidence * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelPill(level: String) {
    val colors = when (level) {
        "High" -> Pair(Color(0xFFFFEBEE), Color(0xFFD32F2F))
        "Moderate" -> Pair(Color(0xFFFFF3E0), Color(0xFFF57C00))
        else -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
    }
    Box(modifier = Modifier.background(colors.first, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(level.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.second)
    }
}

@Composable
fun DownloadDialog(onDismiss: () -> Unit, onDownload: () -> Unit) {
    var isGenerating by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Download Assessment", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp)) }
                }
                Spacer(Modifier.height(24.dp))
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), color = Color(0xFF0288D1))
                    Spacer(Modifier.height(16.dp))
                    Text("Generating PDF...", fontSize = 14.sp, color = Color.Gray)
                } else {
                    Row(modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)).clickable {
                        isGenerating = true
                        onDownload()
                    }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(Color(0xFFFF5252), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Assessment Report (PDF)", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                            Text("Download detailed analysis results", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray), border = BorderStroke(1.dp, Color(0xFFE0E0E0))) {
                        Text("Cancel", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableSection(title: String, trailingContent: @Composable (() -> Unit)? = null, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (trailingContent != null) trailingContent()
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(rotation).size(24.dp))
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) { content() }
            }
        }
    }
}

@Composable
fun MergedRecommendationCard(
    recommendation: DamageRecommendation,
    averageConfidence: Float,
    locationCount: Int,
    damageLevel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(recommendation.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(recommendation.severityBgColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(recommendation.severity, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = recommendation.severityColor)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(recommendation.description, fontSize = 13.sp, color = Color.Gray)

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AI Confidence", fontSize = 12.sp, color = Color.Gray)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { averageConfidence },
                        modifier = Modifier.width(100.dp).height(6.dp),
                        color = getConfidenceColor(damageLevel),
                        trackColor = Color(0xFFE0E0E0)
                    )
                    Text(
                        text = "${(averageConfidence * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        modifier = Modifier.widthIn(min = 40.dp)  // ← FIXES THE 45% ALIGNMENT
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Recommended Actions:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            recommendation.actions.forEach { action ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("•", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                    Text(action, fontSize = 13.sp, color = Color.Gray)
                }
            }

            // ✅ Location counter (only show if > 1)
            if (locationCount > 1) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE0E0E0))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Detected Locations:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEEF2FF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("$locationCount images", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 2000)
@Composable
fun AssessmentResultsPreview() {
    MaterialTheme {
        AssessmentResultsScreen(capturedImages = emptyList())
    }
}