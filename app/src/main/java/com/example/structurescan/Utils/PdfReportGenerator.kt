package com.example.structurescan.Utils
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class PdfAssessmentData(
    val assessmentName: String,
    val date: String,
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
    val buildingType: String = "",
    val constructionYear: String = "",
    val renovationYear: String = "",
    val floors: String = "",
    val material: String = "",
    val foundation: String = "",
    val environment: String = "",
    val previousIssues: String = "",
    val occupancy: String = "",
    val environmentalRisks: String = "",
    val notes: String = "",
    val imageUrls: List<String> = emptyList()
)

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40
    private const val LINE_HEIGHT = 20

    suspend fun generatePdfReport(
        context: Context,
        data: PdfAssessmentData
    ): String? = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            var pageNumber = 1
            var yPosition = MARGIN

            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            yPosition = drawHeader(canvas, data.assessmentName, data.date, yPosition)
            yPosition += 20

            yPosition = drawRiskBadge(canvas, data.overallRisk, yPosition)
            yPosition += 30

            yPosition = drawSummaryText(canvas, data, yPosition)
            yPosition += 30

            if (data.buildingType.isNotEmpty() || data.material.isNotEmpty()) {
                yPosition = checkAndCreateNewPage(pdfDocument, page, canvas, yPosition, 150, ++pageNumber)
                if (yPosition == MARGIN) {
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                }

                yPosition = drawSectionHeader(canvas, "Building Information", yPosition)
                yPosition += 10
                yPosition = drawBuildingInfo(canvas, data, yPosition)
                yPosition += 30
            }

            yPosition = checkAndCreateNewPage(pdfDocument, page, canvas, yPosition, 150, ++pageNumber)
            if (yPosition == MARGIN) {
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
            }

            yPosition = drawSectionHeader(canvas, "Detection Summary", yPosition)
            yPosition += 10
            yPosition = drawDetectionSummary(canvas, data, yPosition)
            yPosition += 30

            yPosition = checkAndCreateNewPage(pdfDocument, page, canvas, yPosition, 200, ++pageNumber)
            if (yPosition == MARGIN) {
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
            }

            yPosition = drawSectionHeader(canvas, "Recommendations", yPosition)
            yPosition += 10
            yPosition = drawRecommendations(canvas, data, yPosition)
            yPosition += 30

            if (data.imageUrls.isNotEmpty()) {
                pdfDocument.finishPage(page)

                data.imageUrls.forEachIndexed { index, imageUrl ->
                    if (imageUrl.isNotEmpty()) {
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas

                        drawImagePage(canvas, imageUrl, index + 1, data.imageUrls.size, context)
                        pdfDocument.finishPage(page)
                    }
                }
            } else {
                pdfDocument.finishPage(page)
            }

            val fileName = "Assessment_${data.assessmentName.replace(" ", "_")}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    pdfDocument.close()
                    Log.d("PdfGenerator", "PDF saved to Downloads: $fileName")

                    val tempFile = File(context.cacheDir, fileName)
                    tempFile.absolutePath
                } else {
                    throw Exception("Failed to create file in Downloads")
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)

                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()

                Log.d("PdfGenerator", "PDF saved to Downloads: ${file.absolutePath}")
                file.absolutePath
            }

        } catch (e: Exception) {
            Log.e("PdfGenerator", "Error generating PDF: ${e.message}", e)
            null
        }
    }

    private fun checkAndCreateNewPage(
        pdfDocument: PdfDocument,
        currentPage: PdfDocument.Page,
        canvas: Canvas,
        currentY: Int,
        requiredSpace: Int,
        newPageNumber: Int
    ): Int {
        return if (currentY + requiredSpace > PAGE_HEIGHT - MARGIN) {
            pdfDocument.finishPage(currentPage)
            MARGIN
        } else {
            currentY
        }
    }

    private fun drawHeader(canvas: Canvas, title: String, date: String, startY: Int): Int {
        var yPos = startY
        val paint = Paint().apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.parseColor("#0288D1")
        }

        canvas.drawText(title, MARGIN.toFloat(), yPos.toFloat(), paint)
        yPos += 30

        paint.apply {
            textSize = 14f
            typeface = Typeface.DEFAULT
            color = android.graphics.Color.GRAY
        }
        canvas.drawText(date, MARGIN.toFloat(), yPos.toFloat(), paint)
        yPos += LINE_HEIGHT

        return yPos
    }

    private fun drawRiskBadge(canvas: Canvas, riskLevel: String, startY: Int): Int {
        val paint = Paint()
        val color = when (riskLevel) {
            "High Risk" -> android.graphics.Color.parseColor("#D32F2F")
            "Moderate Risk" -> android.graphics.Color.parseColor("#F57C00")
            else -> android.graphics.Color.parseColor("#388E3C")
        }

        paint.color = color
        val rect = Rect(MARGIN, startY, MARGIN + 200, startY + 40)
        canvas.drawRoundRect(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat(),
            10f, 10f, paint
        )

        paint.color = android.graphics.Color.WHITE
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(riskLevel, (MARGIN + 20).toFloat(), (startY + 27).toFloat(), paint)

        return startY + 50
    }

    private fun drawSummaryText(canvas: Canvas, data: PdfAssessmentData, startY: Int): Int {
        var yPos = startY
        val paint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.BLACK
        }

        val summaryText = buildString {
            append("Analysis completed. ")
            if (data.totalIssues > 0) {
                append("${data.totalIssues} areas of concern detected. ")
                val details = mutableListOf<String>()
                if (data.crackHighCount > 0) details.add("${data.crackHighCount} high-risk cracks")
                if (data.paintHighCount > 0) details.add("${data.paintHighCount} high-risk paint damage")
                if (data.algaeHighCount > 0) details.add("${data.algaeHighCount} high-risk algae/moss")
                if (details.isNotEmpty()) append(details.joinToString(", ") + ".")
            } else {
                append("No significant issues detected.")
            }
        }

        val lines = wrapText(summaryText, paint, PAGE_WIDTH - 2 * MARGIN)
        lines.forEach { line ->
            canvas.drawText(line, MARGIN.toFloat(), yPos.toFloat(), paint)
            yPos += LINE_HEIGHT
        }

        return yPos
    }

    private fun drawSectionHeader(canvas: Canvas, title: String, startY: Int): Int {
        val paint = Paint().apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.BLACK
        }
        canvas.drawText(title, MARGIN.toFloat(), startY.toFloat(), paint)
        return startY + 25
    }

    private fun drawBuildingInfo(canvas: Canvas, data: PdfAssessmentData, startY: Int): Int {
        var yPos = startY
        val labelPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.GRAY
        }
        val valuePaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.BLACK
        }

        if (data.buildingType.isNotEmpty()) {
            canvas.drawText("Type:", MARGIN.toFloat(), yPos.toFloat(), labelPaint)
            canvas.drawText(data.buildingType, (MARGIN + 150).toFloat(), yPos.toFloat(), valuePaint)
            yPos += LINE_HEIGHT
        }

        if (data.material.isNotEmpty()) {
            canvas.drawText("Material:", MARGIN.toFloat(), yPos.toFloat(), labelPaint)
            canvas.drawText(data.material, (MARGIN + 150).toFloat(), yPos.toFloat(), valuePaint)
            yPos += LINE_HEIGHT
        }

        if (data.constructionYear.isNotEmpty()) {
            canvas.drawText("Built:", MARGIN.toFloat(), yPos.toFloat(), labelPaint)
            canvas.drawText(data.constructionYear, (MARGIN + 150).toFloat(), yPos.toFloat(), valuePaint)
            yPos += LINE_HEIGHT
        }

        if (data.floors.isNotEmpty()) {
            canvas.drawText("Floors:", MARGIN.toFloat(), yPos.toFloat(), labelPaint)
            canvas.drawText(data.floors, (MARGIN + 150).toFloat(), yPos.toFloat(), valuePaint)
            yPos += LINE_HEIGHT
        }

        if (data.foundation.isNotEmpty()) {
            canvas.drawText("Foundation:", MARGIN.toFloat(), yPos.toFloat(), labelPaint)
            canvas.drawText(data.foundation, (MARGIN + 150).toFloat(), yPos.toFloat(), valuePaint)
            yPos += LINE_HEIGHT
        }

        if (data.environment.isNotEmpty()) {
            canvas.drawText("Environment:", MARGIN.toFloat(), yPos.toFloat(), labelPaint)
            canvas.drawText(data.environment, (MARGIN + 150).toFloat(), yPos.toFloat(), valuePaint)
            yPos += LINE_HEIGHT
        }

        if (data.notes.isNotEmpty()) {
            yPos += 10
            canvas.drawText("Additional Notes:", MARGIN.toFloat(), yPos.toFloat(), labelPaint)
            yPos += LINE_HEIGHT
            val noteLines = wrapText(data.notes, valuePaint, PAGE_WIDTH - 2 * MARGIN)
            noteLines.forEach { line ->
                canvas.drawText(line, MARGIN.toFloat(), yPos.toFloat(), valuePaint)
                yPos += LINE_HEIGHT
            }
        }

        return yPos
    }

    private fun drawDetectionSummary(canvas: Canvas, data: PdfAssessmentData, startY: Int): Int {
        var yPos = startY
        val paint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.BLACK
        }

        if (data.crackHighCount + data.crackModerateCount + data.crackLowCount > 0) {
            canvas.drawText("Crack Damage:", MARGIN.toFloat(), yPos.toFloat(), paint)
            canvas.drawText(
                "High: ${data.crackHighCount}  Mod: ${data.crackModerateCount}  Low: ${data.crackLowCount}",
                (MARGIN + 200).toFloat(), yPos.toFloat(), paint
            )
            yPos += LINE_HEIGHT
        }

        if (data.paintHighCount + data.paintModerateCount + data.paintLowCount > 0) {
            canvas.drawText("Paint Damage:", MARGIN.toFloat(), yPos.toFloat(), paint)
            canvas.drawText(
                "High: ${data.paintHighCount}  Mod: ${data.paintModerateCount}  Low: ${data.paintLowCount}",
                (MARGIN + 200).toFloat(), yPos.toFloat(), paint
            )
            yPos += LINE_HEIGHT
        }

        if (data.algaeHighCount + data.algaeModerateCount + data.algaeLowCount > 0) {
            canvas.drawText("Algae/Moss:", MARGIN.toFloat(), yPos.toFloat(), paint)
            canvas.drawText(
                "High: ${data.algaeHighCount}  Mod: ${data.algaeModerateCount}  Low: ${data.algaeLowCount}",
                (MARGIN + 200).toFloat(), yPos.toFloat(), paint
            )
            yPos += LINE_HEIGHT
        }

        if (data.crackHighCount + data.crackModerateCount + data.crackLowCount == 0 &&
            data.paintHighCount + data.paintModerateCount + data.paintLowCount == 0 &&
            data.algaeHighCount + data.algaeModerateCount + data.algaeLowCount == 0) {
            canvas.drawText("No damage detected", MARGIN.toFloat(), yPos.toFloat(), paint)
            yPos += LINE_HEIGHT
        }

        return yPos
    }

    private fun drawRecommendations(canvas: Canvas, data: PdfAssessmentData, startY: Int): Int {
        var yPos = startY
        val titlePaint = Paint().apply {
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.BLACK
        }
        val textPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.BLACK
        }
        val confidencePaint = Paint().apply {
            textSize = 10f
            color = android.graphics.Color.GRAY
        }

        val recommendations = mutableListOf<Triple<String, Int, String>>()

        // ✅ FIXED: Now includes ALL levels (High, Moderate, AND Low)
        if (data.crackHighCount > 0) recommendations.add(Triple("Structural Crack (High Risk)", data.crackHighCount, "HIGH"))
        if (data.crackModerateCount > 0) recommendations.add(Triple("Thermal Cracking (Moderate Risk)", data.crackModerateCount, "MODERATE"))
        if (data.crackLowCount > 0) recommendations.add(Triple("Surface Wear (Low Risk)", data.crackLowCount, "LOW"))

        if (data.paintHighCount > 0) recommendations.add(Triple("Severe Paint Deterioration (High Risk)", data.paintHighCount, "HIGH"))
        if (data.paintModerateCount > 0) recommendations.add(Triple("Paint Damage (Moderate Risk)", data.paintModerateCount, "MODERATE"))
        if (data.paintLowCount > 0) recommendations.add(Triple("Minor Paint Wear (Low Risk)", data.paintLowCount, "LOW"))

        if (data.algaeHighCount > 0) recommendations.add(Triple("Severe Algae/Moss (High Risk)", data.algaeHighCount, "HIGH"))
        if (data.algaeModerateCount > 0) recommendations.add(Triple("Moderate Algae/Moss", data.algaeModerateCount, "MODERATE"))
        if (data.algaeLowCount > 0) recommendations.add(Triple("Minor Algae/Moss (Low Risk)", data.algaeLowCount, "LOW"))

        if (recommendations.isEmpty()) {
            canvas.drawText("✓ No Issues Detected", MARGIN.toFloat(), yPos.toFloat(), titlePaint)
            yPos += LINE_HEIGHT
            canvas.drawText("Your structure is in good condition.", (MARGIN + 10).toFloat(), yPos.toFloat(), textPaint)
            yPos += LINE_HEIGHT
            return yPos
        }

        recommendations.forEach { (title, count, severity) ->
            canvas.drawText("• $title", MARGIN.toFloat(), yPos.toFloat(), titlePaint)
            yPos += LINE_HEIGHT

            if (count > 1) {
                canvas.drawText("  Detected in $count locations", (MARGIN + 10).toFloat(), yPos.toFloat(), confidencePaint)
                yPos += LINE_HEIGHT
            }

            val actions = getRecommendationActions(title)
            actions.forEach { action ->
                canvas.drawText("  - $action", (MARGIN + 10).toFloat(), yPos.toFloat(), textPaint)
                yPos += LINE_HEIGHT
            }
            yPos += 5
        }

        return yPos
    }

    private fun getRecommendationActions(title: String): List<String> {
        return when {
            title.contains("Structural Crack") -> listOf(
                "Consult structural engineer within 30 days",
                "Monitor crack progression weekly"
            )
            title.contains("Thermal Cracking") -> listOf(
                "Monitor during seasonal changes",
                "Seal cracks to prevent water infiltration"
            )
            title.contains("Surface Wear") -> listOf(
                "Regular building maintenance",
                "Monitor for progression during routine inspections"
            )
            title.contains("Severe Paint") -> listOf(
                "Check for moisture damage and mold",
                "Strip to substrate",
                "Prime and repaint"
            )
            title.contains("Paint Damage") && !title.contains("Minor") -> listOf(
                "Scrape and clean",
                "Prime and repaint",
                "Investigate moisture source"
            )
            title.contains("Minor Paint") -> listOf(
                "Clean surface",
                "Touch-up paint as needed",
                "Schedule repainting during regular maintenance"
            )
            title.contains("Severe Algae") -> listOf(
                "Professional biocide cleaning",
                "Fix moisture source",
                "Improve drainage"
            )
            title.contains("Moderate Algae") -> listOf(
                "Clean affected areas",
                "Improve drainage",
                "Increase sunlight exposure"
            )
            title.contains("Minor Algae") -> listOf(
                "Periodic cleaning",
                "Enhance air circulation",
                "Monitor growth patterns"
            )
            else -> listOf("Inspect closely", "Address moisture", "Schedule professional assessment")
        }
    }

    private fun drawImagePage(canvas: Canvas, imageUrl: String, imageNumber: Int, totalImages: Int, context: Context) {
        try {
            val bitmap = when {
                imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> {
                    val url = URL(imageUrl)
                    BitmapFactory.decodeStream(url.openConnection().getInputStream())
                }
                imageUrl.startsWith("content://") -> {
                    val uri = android.net.Uri.parse(imageUrl)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    bmp
                }
                else -> {
                    throw Exception("Invalid image URL format: $imageUrl")
                }
            }

            if (bitmap == null) {
                throw Exception("Failed to decode bitmap from: $imageUrl")
            }

            val titlePaint = Paint().apply {
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.BLACK
            }
            canvas.drawText("Analyzed Image $imageNumber of $totalImages", MARGIN.toFloat(), 60f, titlePaint)

            val maxWidth = PAGE_WIDTH - 2 * MARGIN
            val maxHeight = PAGE_HEIGHT - 150

            val scaleFactor = minOf(
                maxWidth.toFloat() / bitmap.width,
                maxHeight.toFloat() / bitmap.height
            )

            val scaledWidth = (bitmap.width * scaleFactor).toInt()
            val scaledHeight = (bitmap.height * scaleFactor).toInt()

            val left = (PAGE_WIDTH - scaledWidth) / 2
            val top = 100

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            canvas.drawBitmap(scaledBitmap, left.toFloat(), top.toFloat(), null)

            bitmap.recycle()
            scaledBitmap.recycle()

        } catch (e: Exception) {
            Log.e("PdfGenerator", "Error loading image from URL: $imageUrl", e)
            val paint = Paint().apply {
                textSize = 14f
                color = android.graphics.Color.RED
            }
            canvas.drawText("Failed to load image", MARGIN.toFloat(), 150f, paint)
            canvas.drawText("Error: ${e.message}", MARGIN.toFloat(), 170f, paint)
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)

            if (width > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}
