package com.asme.receiving.data.export

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Environment
import android.provider.MediaStore
import android.os.Build
import com.asme.receiving.AppContextHolder
import com.asme.receiving.data.JobItem
import com.asme.receiving.data.MaterialItem
import com.asme.receiving.data.MaterialRepository
import com.asme.receiving.data.JobRepository
import com.asme.receiving.data.customization.CustomizationRepository
import com.asme.receiving.data.customization.SurfaceFinishUnit
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

data class ExportResult(
    val internalPath: String,
    val downloadsFolder: String,
    val materialPacketCount: Int,
    val scanSourceCount: Int,
    val photoCount: Int
)

class ExportService(
    private val context: Context = AppContextHolder.appContext,
    private val jobRepository: JobRepository = JobRepository(),
    private val materialRepository: MaterialRepository = MaterialRepository()
) {
    private val dateFormatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private val customizationRepository = CustomizationRepository(context)

    suspend fun exportJob(jobNumber: String): ExportResult {
        PDFBoxResourceLoader.init(context)
        val job = jobRepository.get(jobNumber) ?: throw IllegalStateException("Job not found")
        val materials = materialRepository.streamMaterialsForJob(jobNumber).first()
        val exportRoot = File(context.filesDir, "exports/${sanitize(job.jobNumber)}")
        if (exportRoot.exists()) {
            exportRoot.deleteRecursively()
        }
        exportRoot.mkdirs()

        val packetDir = File(exportRoot, "material_packets").also { it.mkdirs() }
        var materialPacketCount = 0
        var scanSourceCount = 0
        var photoCount = 0

        materials.forEachIndexed { index, material ->
            val suffix = material.description.ifBlank { "material_${index + 1}" }
            val baseName = truncate(
                sanitize("${job.jobNumber}_${index + 1}_$suffix"),
                60
            )
            val outputFile = File(packetDir, "${baseName}_packet.pdf")
            val packetResult = exportMaterialPacket(job, material, outputFile)
            if (packetResult.created) {
                materialPacketCount += 1
            }
            scanSourceCount += packetResult.scanSourceCount
            photoCount += packetResult.photoCount
        }

        writeExportNotice(
            exportRoot = exportRoot,
            job = job,
            materialPacketCount = materialPacketCount,
            scanSourceCount = scanSourceCount,
            photoCount = photoCount
        )
        val downloadsFolder = copyToDownloads(exportRoot, job.jobNumber)
        jobRepository.updateExportStatus(job.jobNumber, downloadsFolder)
        return ExportResult(
            internalPath = exportRoot.absolutePath,
            downloadsFolder = downloadsFolder,
            materialPacketCount = materialPacketCount,
            scanSourceCount = scanSourceCount,
            photoCount = photoCount
        )
    }

    private fun exportMaterialPacket(job: JobItem, material: MaterialItem, outputFile: File): MaterialPacketResult {
        val scanPaths = decodeStoredScanSourcePaths(material.scanPaths)
        val photoPaths = decodeStoredPhotoPaths(material.photoPaths)
        var created = false
        val retainedScanDocuments = mutableListOf<PDDocument>()
        PDDocument().use { document ->
            try {
                appendReceivingReport(document, job, material)
                val includedScans = appendScanPages(document, scanPaths, retainedScanDocuments)
                val includedPhotos = appendPhotoPages(document, photoPaths)
                if (document.numberOfPages > 0) {
                    FileOutputStream(outputFile).use { out -> document.save(out) }
                    created = true
                }
                return MaterialPacketResult(
                    created = created,
                    scanSourceCount = includedScans,
                    photoCount = includedPhotos
                )
            } finally {
                retainedScanDocuments.forEach { source ->
                    runCatching { source.close() }
                }
            }
        }
    }

    private fun appendReceivingReport(document: PDDocument, job: JobItem, material: MaterialItem) {
        val customization = customizationRepository.load()
        val margin = 36f
        val width = PDRectangle.LETTER.width - margin * 2
        var stream = PDPageContentStream(document, PDPage(PDRectangle.LETTER).also(document::addPage))
        var y = PDRectangle.LETTER.height - margin
        var continuedPage = false

        fun drawPageHeader(continued: Boolean) {
            if (!continued) {
                drawReportLogo(
                    document = document,
                    stream = stream,
                    logoPath = customization.companyLogoPath,
                    x = PDRectangle.LETTER.width - margin - 118f,
                    y = y - 40f,
                    maxWidth = 118f,
                    maxHeight = 40f
                )
                stream.setFont(PDType1Font.HELVETICA_BOLD, 16f)
                drawText(stream, margin, y, "RECEIVING INSPECTION REPORT")
                y -= 20f
            } else {
                stream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
                drawText(stream, margin, y, "RECEIVING INSPECTION REPORT (CONT.)")
                y -= 18f
            }
            stream.setFont(PDType1Font.HELVETICA, 11f)
            drawText(stream, margin, y, "Job#: ${job.jobNumber}")
            y -= 16f
        }

        fun startContinuationPage() {
            stream.close()
            stream = PDPageContentStream(document, PDPage(PDRectangle.LETTER).also(document::addPage))
            y = PDRectangle.LETTER.height - margin
            continuedPage = true
            drawPageHeader(continued = true)
        }

        fun ensureSpace(requiredHeight: Float) {
            if (y - requiredHeight < margin) {
                startContinuationPage()
            }
        }

        try {
            drawPageHeader(continued = false)

            y = drawSectionTitle(stream, margin, y, "Material Details")
            y = drawLabelBoxRow(
                stream,
                margin,
                y,
                width,
                listOf(
                    LabelValue("Material Description", material.description, 0.6f),
                    LabelValue("PO#", material.poNumber, 0.2f),
                    LabelValue("Vendor", material.vendor, 0.2f)
                )
            )
            y = drawLabelBoxRow(
                stream,
                margin,
                y,
                width,
                listOf(
                    LabelValue("Qty", material.quantity, 0.15f),
                    LabelValue("Product", material.productType, 0.25f),
                    LabelValue("Specification", material.specificationPrefix.ifBlank { material.specificationNumbers }, 0.2f),
                    LabelValue("Grade/Type", material.gradeType, 0.2f),
                    LabelValue(
                        "Fitting",
                        material.fittingDisplayValue(),
                        0.2f
                    )
                )
            )

            y = drawSectionTitle(stream, margin, y, "Dimensions")
            y = drawToggleRow(
                stream,
                margin,
                y,
                "Unit",
                listOf(
                    Toggle("Imperial", material.dimensionUnit == "imperial"),
                    Toggle("Metric", material.dimensionUnit == "metric")
                ),
                optionYOffset = 10f
            )
            y = drawLabelBoxRow(
                stream,
                margin,
                y,
                width,
                listOf(
                    LabelValue("TH 1", material.thickness1, 0.25f),
                    LabelValue("TH 2", material.thickness2, 0.25f),
                    LabelValue("TH 3", material.thickness3, 0.25f),
                    LabelValue("TH 4", material.thickness4, 0.25f)
                )
            )
            y = drawLabelBoxRow(
                stream,
                margin,
                y,
                width,
                listOf(
                    LabelValue("Width", material.width, 0.25f),
                    LabelValue("Length", material.length, 0.25f),
                    LabelValue("Diameter", material.diameter, 0.25f),
                    LabelValue("O.D./I.D.", material.diameterType, 0.25f)
                )
            )

            y = drawSectionTitle(stream, margin, y, "Inspection")
            y = drawToggleRow(
                stream,
                margin,
                y,
                "Visual Inspection Acceptable",
                listOf(
                    Toggle("Yes", material.visualInspectionAcceptable),
                    Toggle("No", !material.visualInspectionAcceptable)
                )
            )
            y -= 4f
            if (material.b16DimensionsAcceptable.isNotBlank()) {
                y = drawLabelBoxRow(
                    stream,
                    margin,
                    y,
                    width,
                    listOf(
                        LabelValue("B16 Dimensions Acceptable", material.b16DimensionsAcceptable, 1f)
                    )
                )
            }
            if (material.hasSurfaceFinishData()) {
                val surfaceFinishItems = mutableListOf<LabelValue>()
                if (material.surfaceFinishCode.isNotBlank()) {
                    surfaceFinishItems += LabelValue(
                        label = "Surface Finish",
                        value = material.surfaceFinishCode,
                        widthFraction = if (material.surfaceFinishReading.isNotBlank()) 0.35f else 1f
                    )
                }
                if (material.surfaceFinishReading.isNotBlank()) {
                    surfaceFinishItems += LabelValue(
                        label = "Surface Finish Reading",
                        value = material.formattedSurfaceFinishReading(),
                        widthFraction = if (material.surfaceFinishCode.isNotBlank()) 0.65f else 1f
                    )
                }
                y = drawLabelBoxRow(
                    stream,
                    margin,
                    y,
                    width,
                    surfaceFinishItems
                )
            }
            y = drawLabelBoxRow(
                stream,
                margin,
                y,
                width,
                listOf(
                    LabelValue("Marking Actual", material.markings, 1f)
                ),
                boxHeight = 40f
            )
            val inspectionSectionDrop = 8f

            y = drawToggleRow(
                stream,
                margin,
                y,
                "Marking Acceptable to Code/Standard",
                listOf(
                    Toggle("Yes", material.markingAcceptable && !material.markingAcceptableNa),
                    Toggle("No", !material.markingAcceptable && !material.markingAcceptableNa),
                    Toggle("N/A", material.markingAcceptableNa)
                ),
                labelYOffset = -inspectionSectionDrop
            )
            y = drawToggleRow(
                stream,
                margin,
                y,
                "MTR/CoC Acceptable to Specification",
                listOf(
                    Toggle("Yes", material.mtrAcceptable && !material.mtrAcceptableNa),
                    Toggle("No", !material.mtrAcceptable && !material.mtrAcceptableNa),
                    Toggle("N/A", material.mtrAcceptableNa)
                ),
                labelYOffset = -inspectionSectionDrop
            )
            y = drawToggleRow(
                stream,
                margin,
                y,
                "Disposition",
                listOf(
                    Toggle("Accept", material.acceptanceStatus == "accept"),
                    Toggle("Reject", material.acceptanceStatus == "reject")
                ),
                labelYOffset = -inspectionSectionDrop
            )

            val commentText = listOf(material.comments, material.description)
                .filter { it.isNotBlank() }
                .joinToString(" | ")
            y -= inspectionSectionDrop
            ensureSpace(150f)
            y = drawSectionTitle(stream, margin, y, "Comments") + 6f
            y = drawMultiLineBox(stream, margin, y, width, commentText, 40f)

            ensureSpace(128f)
            y = drawSectionTitle(stream, margin, y, "Quality Control")
            y = drawToggleRow(
                stream,
                margin,
                y,
                "Material Disposition",
                listOf(
                    Toggle("Approved", material.materialApproval == "approved"),
                    Toggle("Rejected", material.materialApproval == "rejected")
                )
            )
            ensureSpace(104f)
            y = drawSectionTitle(stream, margin, y, "Signatures")
            y -= 2f
            y = drawSignatureRow(
                document = document,
                stream = stream,
                x = margin,
                y = y,
                label = "QC Inspector",
                printedName = material.qcInitials,
                signaturePath = material.qcSignaturePath,
                dateText = dateFormatter.format(Date(material.qcDate))
            )
            ensureSpace(52f)
            drawSignatureRow(
                document = document,
                stream = stream,
                x = margin,
                y = y,
                label = "QC Manager",
                printedName = material.qcManager.ifBlank { material.qcManagerInitials },
                signaturePath = material.qcManagerSignaturePath,
                dateText = dateFormatter.format(Date(material.qcManagerDate))
            )
        } finally {
            stream.close()
        }
    }

    private fun drawSectionTitle(
        stream: PDPageContentStream,
        x: Float,
        y: Float,
        title: String
    ): Float {
        stream.setFont(PDType1Font.HELVETICA_BOLD, 12f)
        drawText(stream, x, y, title)
        return y - 12f
    }

    private fun drawLabelBoxRow(
        stream: PDPageContentStream,
        x: Float,
        y: Float,
        totalWidth: Float,
        items: List<LabelValue>,
        boxHeight: Float = 24f
    ): Float {
        val spacing = 8f
        var cursor = x
        stream.setFont(PDType1Font.HELVETICA, 9f)
        items.forEach { item ->
            val boxWidth = (totalWidth - spacing * (items.size - 1)) * item.widthFraction
            drawText(stream, cursor, y, item.label)
            drawRect(stream, cursor, y - 2f - boxHeight, boxWidth, boxHeight)
            stream.setFont(PDType1Font.HELVETICA, 10f)
            drawWrappedText(stream, cursor + 4f, y - 13f, boxWidth - 8f, item.value)
            stream.setFont(PDType1Font.HELVETICA, 9f)
            cursor += boxWidth + spacing
        }
        return y - (boxHeight + 18f)
    }

    private fun drawToggleRow(
        stream: PDPageContentStream,
        x: Float,
        y: Float,
        label: String,
        toggles: List<Toggle>,
        labelYOffset: Float = 0f,
        optionYOffset: Float = 0f
    ): Float {
        stream.setFont(PDType1Font.HELVETICA, 10f)
        drawText(stream, x, y + labelYOffset, label)
        var cursor = x + 200f
        toggles.forEach { toggle ->
            val optionY = y + optionYOffset
            drawRect(stream, cursor, optionY - 12f, 12f, 12f)
            if (toggle.selected) {
                stream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
                drawText(stream, cursor + 3f, optionY - 9f, "X")
                stream.setFont(PDType1Font.HELVETICA, 10f)
            }
            drawText(stream, cursor + 18f, optionY, toggle.label)
            cursor += 90f
        }
        return y - 18f
    }

    private fun drawMultiLineBox(
        stream: PDPageContentStream,
        x: Float,
        y: Float,
        width: Float,
        text: String,
        height: Float
    ): Float {
        drawRect(stream, x, y - height, width, height)
        stream.setFont(PDType1Font.HELVETICA, 10f)
        drawWrappedText(stream, x + 4f, y - 14f, width - 8f, text)
        return y - (height + 12f)
    }

    private fun drawSignatureRow(
        document: PDDocument,
        stream: PDPageContentStream,
        x: Float,
        y: Float,
        label: String,
        printedName: String,
        signaturePath: String,
        dateText: String
    ): Float {
        stream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
        drawText(stream, x, y, label)

        val labelY = y - 14f
        val printWidth = 160f
        val signWidth = 180f
        val dateWidth = 110f
        val gap = 12f
        val boxHeight = 30f

        drawText(stream, x, labelY, "Print")
        drawRect(stream, x, labelY - 2f - boxHeight, printWidth, boxHeight)
        stream.setFont(PDType1Font.HELVETICA, 10f)
        drawWrappedText(stream, x + 4f, labelY - 13f, printWidth - 8f, printedName)

        val signX = x + printWidth + gap
        stream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
        drawText(stream, signX, labelY, "Sign")
        drawRect(stream, signX, labelY - 2f - boxHeight, signWidth, boxHeight)
        drawSignatureImage(
            document = document,
            stream = stream,
            signaturePath = signaturePath,
            x = signX,
            y = labelY - 2f - boxHeight,
            width = signWidth,
            height = boxHeight
        )

        val dateX = signX + signWidth + gap
        drawText(stream, dateX, labelY, "Date")
        drawRect(stream, dateX, labelY - 2f - boxHeight, dateWidth, boxHeight)
        stream.setFont(PDType1Font.HELVETICA, 10f)
        drawWrappedText(stream, dateX + 4f, labelY - 13f, dateWidth - 8f, dateText)

        return y - 56f
    }

    private fun drawSignatureImage(
        document: PDDocument,
        stream: PDPageContentStream,
        signaturePath: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        if (signaturePath.isBlank()) return
        val file = File(signaturePath)
        if (!file.exists()) return
        val image = com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromFile(
            file.absolutePath,
            document
        )
        val scale = minOf((width - 6f) / image.width, (height - 6f) / image.height)
        val drawWidth = image.width * scale
        val drawHeight = image.height * scale
        val drawX = x + ((width - drawWidth) / 2f)
        val drawY = y + ((height - drawHeight) / 2f)
        stream.drawImage(image, drawX, drawY, drawWidth, drawHeight)
    }

    private fun drawReportLogo(
        document: PDDocument,
        stream: PDPageContentStream,
        logoPath: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        maxHeight: Float
    ) {
        if (logoPath.isBlank()) return
        val file = File(logoPath)
        if (!file.exists()) return
        val image = runCatching { createPdfImage(document, file) }.getOrNull() ?: return
        val scale = minOf(maxWidth / image.width, maxHeight / image.height)
        val drawWidth = image.width * scale
        val drawHeight = image.height * scale
        val drawX = x + ((maxWidth - drawWidth) / 2f)
        val drawY = y + ((maxHeight - drawHeight) / 2f)
        stream.drawImage(image, drawX, drawY, drawWidth, drawHeight)
    }

    private fun drawRect(stream: PDPageContentStream, x: Float, y: Float, w: Float, h: Float) {
        stream.addRect(x, y, w, h)
        stream.stroke()
    }

    private fun drawText(stream: PDPageContentStream, x: Float, y: Float, text: String) {
        val normalizedText = normalizePdfSingleLine(text)
        if (normalizedText.isBlank()) return
        stream.beginText()
        stream.newLineAtOffset(x, y)
        stream.showText(normalizedText)
        stream.endText()
    }

    private fun drawWrappedText(
        stream: PDPageContentStream,
        x: Float,
        y: Float,
        width: Float,
        text: String
    ) {
        var offsetY = 0f
        val paragraphs = normalizePdfMultiline(text).split('\n')
        paragraphs.forEach { paragraph ->
            val words = paragraph.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.isEmpty()) {
                offsetY += 12f
                return@forEach
            }

            var line = ""
            words.forEach { word ->
                val test = if (line.isBlank()) word else "$line $word"
                val testWidth = measureBodyTextWidth(test)
                if (testWidth > width && line.isNotBlank()) {
                    drawText(stream, x, y - offsetY, line)
                    offsetY += 12f
                    line = word
                } else {
                    line = test
                }
            }

            if (line.isNotBlank()) {
                drawText(stream, x, y - offsetY, line)
                offsetY += 12f
            }
        }
    }

    private data class LabelValue(
        val label: String,
        val value: String,
        val widthFraction: Float
    )

    private data class Toggle(
        val label: String,
        val selected: Boolean
    )

    private data class MaterialPacketResult(
        val created: Boolean,
        val scanSourceCount: Int,
        val photoCount: Int
    )

    private fun appendScanPages(
        document: PDDocument,
        scanPaths: List<String>,
        retainedScanDocuments: MutableList<PDDocument>
    ): Int {
        var includedCount = 0
        scanPaths.forEach { path ->
            val file = File(path)
            if (!file.exists()) return@forEach
            if (path.endsWith(".pdf", true)) {
                val source = PDDocument.load(file)
                retainedScanDocuments += source
                source.pages.forEach { page ->
                    document.importPage(page)
                }
            } else {
                val image = createPdfImage(document, file)
                val page = PDPage(PDRectangle.LETTER)
                document.addPage(page)
                PDPageContentStream(document, page).use { stream ->
                    val scale = minOf(
                        PDRectangle.LETTER.width / image.width,
                        PDRectangle.LETTER.height / image.height
                    )
                    val width = image.width * scale
                    val height = image.height * scale
                    val x = (PDRectangle.LETTER.width - width) / 2
                    val y = (PDRectangle.LETTER.height - height) / 2
                    stream.drawImage(image, x, y, width, height)
                }
            }
            includedCount += 1
        }
        return includedCount
    }

    private fun appendPhotoPages(document: PDDocument, photoPaths: List<String>): Int {
        val existingPhotos = photoPaths.map(::File).filter { it.exists() }
        if (existingPhotos.isEmpty()) return 0

        val margin = 36f
        val headerGap = 30f
        val gutter = 12f
        val cellWidth = (PDRectangle.LETTER.width - (margin * 2) - gutter) / 2
        val cellHeight = (PDRectangle.LETTER.height - (margin * 2) - headerGap - gutter) / 2

        existingPhotos.chunked(4).forEachIndexed { pageIndex, chunk ->
            val page = PDPage(PDRectangle.LETTER)
            document.addPage(page)
            PDPageContentStream(document, page).use { stream ->
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12f)
                drawText(
                    stream,
                    margin,
                    PDRectangle.LETTER.height - margin,
                    "Material Photos${if (pageIndex == 0) "" else " (${pageIndex + 1})"}"
                )

                chunk.forEachIndexed { index, file ->
                    val row = index / 2
                    val column = index % 2
                    val cellX = margin + (column * (cellWidth + gutter))
                    val cellY = PDRectangle.LETTER.height - margin - headerGap - (row * (cellHeight + gutter)) - cellHeight
                    drawRect(stream, cellX, cellY, cellWidth, cellHeight)

                    val image = createPdfImage(document, file)
                    val scale = minOf(cellWidth / image.width, cellHeight / image.height)
                    val drawWidth = image.width * scale
                    val drawHeight = image.height * scale
                    val drawX = cellX + ((cellWidth - drawWidth) / 2f)
                    val drawY = cellY + ((cellHeight - drawHeight) / 2f)
                    stream.drawImage(image, drawX, drawY, drawWidth, drawHeight)
                }
            }
        }
        return existingPhotos.size
    }

    private fun writeExportNotice(
        exportRoot: File,
        job: JobItem,
        materialPacketCount: Int,
        scanSourceCount: Int,
        photoCount: Int
    ) {
        val notice = File(exportRoot, "export_info.txt")
        val content = buildString {
            appendLine("Job Export")
            appendLine("Job: ${job.jobNumber}")
            appendLine("Description: ${job.description}")
            appendLine("Exported: ${dateFormatter.format(Date())}")
            appendLine("Material packet PDFs: $materialPacketCount")
            appendLine("Included scan sources: $scanSourceCount")
            appendLine("Included photos: $photoCount")
        }
        notice.writeText(content)
    }

    private fun copyToDownloads(exportRoot: File, jobNumber: String): String {
        val downloadDir = "MaterialGuardian/${sanitize(jobNumber)}"
        val rootPath = exportRoot.absolutePath
        clearExistingDownloadsExport(downloadDir)
        exportRoot.walkTopDown().forEach { file ->
            if (file.isDirectory) return@forEach
            val relativePath = file.absolutePath.removePrefix(rootPath).trimStart(File.separatorChar)
            val relativeParent = File(relativePath).parent?.replace(File.separatorChar, '/')?.trim('/')
            val targetRelativeDir = listOfNotNull(downloadDir, relativeParent)
                .filter { it.isNotBlank() }
                .joinToString("/")
            val displayName = file.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, guessMimeType(file.name))
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/$targetRelativeDir"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    runCatching {
                        resolver.openOutputStream(uri)?.use { output ->
                            file.inputStream().use { it.copyTo(output) }
                        } ?: error("Could not open output stream for $displayName")
                        resolver.update(
                            uri,
                            ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                            null,
                            null
                        )
                    }.getOrElse { error ->
                        resolver.delete(uri, null, null)
                        throw error
                    }
                } else {
                    throw IllegalStateException("Could not create Downloads entry for $displayName")
                }
            } else {
                val legacyDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    targetRelativeDir
                )
                legacyDir.mkdirs()
                val target = File(legacyDir, displayName)
                file.copyTo(target, overwrite = true)
            }
        }
        return "Downloads/$downloadDir"
    }

    private fun guessMimeType(name: String): String {
        return when {
            name.endsWith(".pdf", true) -> "application/pdf"
            name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
            name.endsWith(".txt", true) -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    private fun measureBodyTextWidth(text: String): Float {
        val normalized = normalizePdfSingleLine(text)
        if (normalized.isBlank()) return 0f
        return PDType1Font.HELVETICA.getStringWidth(normalized) / 1000f * 10f
    }

    private fun normalizePdfSingleLine(text: String): String {
        return normalizePdfText(text, preserveLineBreaks = false)
    }

    private fun normalizePdfMultiline(text: String): String {
        return normalizePdfText(text, preserveLineBreaks = true)
    }

    private fun normalizePdfText(text: String, preserveLineBreaks: Boolean): String {
        if (text.isBlank()) return ""
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFKD)
        val builder = StringBuilder(normalized.length)
        var previousWasSpace = false
        var previousWasNewline = false

        normalized.forEach { char ->
            val mapped = when (char) {
                '\u2018', '\u2019', '\u2032' -> '\''
                '\u201C', '\u201D', '\u2033' -> '"'
                '\u2013', '\u2014', '\u2212' -> '-'
                '\u2022' -> '*'
                '\u00A0', '\t' -> ' '
                else -> char
            }

            when {
                Character.getType(mapped) == Character.NON_SPACING_MARK.toInt() -> Unit
                mapped == '\r' || mapped == '\n' -> {
                    if (preserveLineBreaks && !previousWasNewline) {
                        builder.append('\n')
                        previousWasNewline = true
                        previousWasSpace = false
                    } else if (!preserveLineBreaks && !previousWasSpace) {
                        builder.append(' ')
                        previousWasSpace = true
                    }
                }
                Character.isWhitespace(mapped) -> {
                    if (!previousWasSpace && !previousWasNewline) {
                        builder.append(' ')
                        previousWasSpace = true
                    }
                }
                Character.isISOControl(mapped) -> Unit
                mapped.code in 32..126 || mapped.code in 160..255 -> {
                    builder.append(mapped)
                    previousWasSpace = false
                    previousWasNewline = false
                }
                else -> {
                    builder.append('?')
                    previousWasSpace = false
                    previousWasNewline = false
                }
            }
        }

        return if (preserveLineBreaks) {
            builder.toString()
                .lines()
                .joinToString("\n") { it.trimEnd() }
                .trim()
        } else {
            builder.toString().trim()
        }
    }

    private fun createPdfImage(document: PDDocument, file: File): PDImageXObject {
        val extension = file.extension.lowercase(Locale.US)
        if (extension != "jpg" && extension != "jpeg") {
            return PDImageXObject.createFromFile(file.absolutePath, document)
        }

        val bitmap = decodeBitmapForPdf(file) ?: return PDImageXObject.createFromFile(file.absolutePath, document)
        return try {
            ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
                JPEGFactory.createFromByteArray(document, output.toByteArray())
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeBitmapForPdf(file: File): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = orientationMatrix(orientation) ?: return bitmap
        val rotatedBitmap = runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrNull()

        return if (rotatedBitmap != null) {
            bitmap.recycle()
            rotatedBitmap
        } else {
            bitmap
        }
    }

    private fun orientationMatrix(orientation: Int): Matrix? {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(90f) }
            ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(180f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(270f) }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { preScale(-1f, 1f) }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { preScale(1f, -1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply {
                preScale(-1f, 1f)
                postRotate(270f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply {
                preScale(-1f, 1f)
                postRotate(90f)
            }
            else -> null
        }
    }

    private fun clearExistingDownloadsExport(downloadDir: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePrefix = Environment.DIRECTORY_DOWNLOADS + "/$downloadDir/"
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                arrayOf("$relativePrefix%"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    resolver.delete(ContentUris.withAppendedId(collection, id), null, null)
                }
            }
        } else {
            val legacyDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                downloadDir
            )
            if (legacyDir.exists()) {
                legacyDir.deleteRecursively()
            }
        }
    }

    private fun sanitize(value: String): String {
        return value.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    private fun decodeStoredPhotoPaths(value: String): List<String> {
        return value.split("|").filter { it.isNotBlank() }
    }

    private fun decodeStoredScanSourcePaths(value: String): List<String> {
        return value.split("|")
            .filter { it.isNotBlank() }
            .map { it.substringBefore('\t') }
            .filter { it.isNotBlank() }
    }

    private fun truncate(value: String, length: Int): String {
        return if (value.length <= length) value else value.substring(0, length)
    }
}

private fun MaterialItem.fittingDisplayValue(): String {
    if (fittingStandard.isBlank() || fittingStandard == "N/A") return ""
    return listOf(fittingStandard, fittingSuffix)
        .filter { it.isNotBlank() }
        .joinToString(".")
}

private fun MaterialItem.hasSurfaceFinishData(): Boolean {
    return surfaceFinishCode.isNotBlank() || surfaceFinishReading.isNotBlank()
}

private fun MaterialItem.formattedSurfaceFinishReading(): String {
    val unitLabel = surfaceFinishUnit.takeIf { it.isNotBlank() }?.let(SurfaceFinishUnit::label).orEmpty()
    return listOf(surfaceFinishReading, unitLabel)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}
