package com.asme.receiving.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import com.asme.receiving.AppContextHolder
import com.asme.receiving.data.JobItem
import com.asme.receiving.data.JobRepository
import com.asme.receiving.data.MaterialItem
import com.asme.receiving.data.MaterialRepository
import com.asme.receiving.data.local.AppDatabaseProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class ExportServiceInstrumentedTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppContextHolder.init(context)
        PDFBoxResourceLoader.init(context)
        AppDatabaseProvider.resetForTests()
        File(context.getDatabasePath("material_guardian.db").path).delete()
    }

    @Test
    fun exportCreatesMaterialPacketPdf() = runBlocking {
        val jobRepo = JobRepository()
        val materialRepo = MaterialRepository()
        jobRepo.upsert(JobItem(jobNumber = "JOB-77", description = "Instrumented"))
        materialRepo.addMaterial(
            MaterialItem(
                jobNumber = "JOB-77",
                description = "Plate",
                comments = "Looks good"
            )
        )

        val exportResult = ExportService().exportJob("JOB-77")
        val exportDir = File(exportResult.internalPath, "material_packets")
        val files = exportDir.listFiles()?.toList().orEmpty()

        assertTrue(exportDir.exists())
        assertEquals(1, exportResult.materialPacketCount)
        assertTrue(files.any { it.name.endsWith("_packet.pdf") })
    }

    @Test
    fun exportsStaySeparatedByJobNumber() = runBlocking {
        val jobRepo = JobRepository()
        val materialRepo = MaterialRepository()

        jobRepo.upsert(JobItem(jobNumber = "JOB-100", description = "First"))
        jobRepo.upsert(JobItem(jobNumber = "JOB-200", description = "Second"))

        materialRepo.addMaterial(
            MaterialItem(
                jobNumber = "JOB-100",
                description = "Plate A"
            )
        )
        materialRepo.addMaterial(
            MaterialItem(
                jobNumber = "JOB-200",
                description = "Pipe B"
            )
        )

        val firstExport = ExportService().exportJob("JOB-100")
        val secondExport = ExportService().exportJob("JOB-200")
        val firstPackets = File(firstExport.internalPath, "material_packets").listFiles()?.map { it.name }.orEmpty()
        val secondPackets = File(secondExport.internalPath, "material_packets").listFiles()?.map { it.name }.orEmpty()

        assertTrue(firstExport.internalPath != secondExport.internalPath)
        assertTrue(firstPackets.any { it.contains("job_100") })
        assertTrue(firstPackets.none { it.contains("job_200") })
        assertTrue(secondPackets.any { it.contains("job_200") })
        assertTrue(secondPackets.none { it.contains("job_100") })
    }

    @Test
    fun exportCreatesDistinctPacketsForDuplicateMaterialDescriptions() = runBlocking {
        val jobRepo = JobRepository()
        val materialRepo = MaterialRepository()
        jobRepo.upsert(JobItem(jobNumber = "JOB-DUPE", description = "Duplicate descriptions"))

        materialRepo.addMaterial(
            MaterialItem(
                jobNumber = "JOB-DUPE",
                description = "Plate"
            )
        )
        materialRepo.addMaterial(
            MaterialItem(
                jobNumber = "JOB-DUPE",
                description = "Plate"
            )
        )

        val exportResult = ExportService().exportJob("JOB-DUPE")
        val packetNames = File(exportResult.internalPath, "material_packets")
            .listFiles()
            ?.map { it.name }
            .orEmpty()

        assertEquals(2, exportResult.materialPacketCount)
        assertEquals(2, packetNames.size)
        assertTrue(packetNames.distinct().size == 2)
    }

    @Test
    fun materialPacketContainsJobNumberAndMaterialDetails() = runBlocking {
        val jobRepo = JobRepository()
        val materialRepo = MaterialRepository()
        jobRepo.upsert(JobItem(jobNumber = "JOB-REPORT", description = "Report content"))
        materialRepo.addMaterial(
            MaterialItem(
                jobNumber = "JOB-REPORT",
                description = "Steel Plate",
                comments = "Looks good"
            )
        )

        val exportResult = ExportService().exportJob("JOB-REPORT")
        val pdfFile = File(exportResult.internalPath, "material_packets")
            .listFiles()
            ?.firstOrNull { it.name.endsWith("_packet.pdf") }
        requireNotNull(pdfFile)

        val text = PDDocument.load(pdfFile).use { document ->
            PDFTextStripper().getText(document)
        }

        assertTrue(text.contains("RECEIVING INSPECTION REPORT"))
        assertTrue(text.contains("Job#: JOB-REPORT"))
        assertTrue(text.contains("Steel Plate"))
        assertTrue(text.contains("Looks good"))
    }

    @Test
    fun materialPacketPageCountIncludesCoverScansAndPhotoSheets() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val jobRepo = JobRepository()
        val materialRepo = MaterialRepository()
        jobRepo.upsert(JobItem(jobNumber = "JOB-PAGES", description = "Packet page count"))

        val scanPdf = createSamplePdf(context.cacheDir, "sample_scan.pdf", 2)
        val photos = listOf(
            createSampleImage(context.cacheDir, "photo_1.jpg", Color.RED),
            createSampleImage(context.cacheDir, "photo_2.jpg", Color.BLUE),
            createSampleImage(context.cacheDir, "photo_3.jpg", Color.GREEN),
            createSampleImage(context.cacheDir, "photo_4.jpg", Color.YELLOW),
            createSampleImage(context.cacheDir, "photo_5.jpg", Color.CYAN)
        )

        materialRepo.addMaterial(
            MaterialItem(
                jobNumber = "JOB-PAGES",
                description = "Packet Test",
                scanPaths = scanPdf.absolutePath,
                photoPaths = photos.joinToString("|") { it.absolutePath }
            )
        )

        val exportResult = ExportService().exportJob("JOB-PAGES")
        val pdfFile = File(exportResult.internalPath, "material_packets")
            .listFiles()
            ?.firstOrNull { it.name.endsWith("_packet.pdf") }
        requireNotNull(pdfFile)

        val pageCount = PDDocument.load(pdfFile).use { document -> document.numberOfPages }

        assertEquals(1, exportResult.scanSourceCount)
        assertEquals(5, exportResult.photoCount)
        assertEquals(5, pageCount)
    }

    @Test
    fun multiJobExportKeepsMaterialPacketsSeparatedAndEmbedsSignedContent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val jobRepo = JobRepository()
        val materialRepo = MaterialRepository()

        jobRepo.upsert(JobItem(jobNumber = "JOB ALPHA/101", description = "Alpha export"))
        jobRepo.upsert(JobItem(jobNumber = "JOB BETA/202", description = "Beta export"))

        val inspectorSignature = createSignatureImage(context.cacheDir, "inspector_signature.png")
        val managerSignature = createSignatureImage(context.cacheDir, "manager_signature.png")
        val scanPdf = createSamplePdf(context.cacheDir, "alpha_scan.pdf", 2)
        val photos = listOf(
            createSampleImage(context.cacheDir, "alpha_photo_1.jpg", Color.RED),
            createSampleImage(context.cacheDir, "alpha_photo_2.jpg", Color.BLUE),
            createSampleImage(context.cacheDir, "alpha_photo_3.jpg", Color.GREEN),
            createSampleImage(context.cacheDir, "alpha_photo_4.jpg", Color.YELLOW),
            createSampleImage(context.cacheDir, "alpha_photo_5.jpg", Color.CYAN)
        )

        materialRepo.addMaterial(
            MaterialItem(
                jobNumber = "JOB ALPHA/101",
                description = "Plate",
                comments = "Alpha first material",
                qcInitials = "KP",
                qcSignaturePath = inspectorSignature.absolutePath,
                qcManager = "Kevin",
                qcManagerInitials = "KMP",
                qcManagerSignaturePath = managerSignature.absolutePath,
                scanPaths = scanPdf.absolutePath,
                photoPaths = photos.joinToString("|") { it.absolutePath }
            )
        )
        materialRepo.addMaterial(
            MaterialItem(
                jobNumber = "JOB ALPHA/101",
                description = "Plate",
                comments = "Alpha second material"
            )
        )
        materialRepo.addMaterial(
            MaterialItem(
                jobNumber = "JOB BETA/202",
                description = "Pipe",
                comments = "Beta material"
            )
        )

        val alphaExport = ExportService().exportJob("JOB ALPHA/101")
        val betaExport = ExportService().exportJob("JOB BETA/202")

        val alphaPacketDir = File(alphaExport.internalPath, "material_packets")
        val betaPacketDir = File(betaExport.internalPath, "material_packets")
        val alphaPackets = alphaPacketDir.listFiles()?.sortedBy { it.name }.orEmpty()
        val betaPackets = betaPacketDir.listFiles()?.sortedBy { it.name }.orEmpty()
        val alphaNotice = File(alphaExport.internalPath, "export_info.txt").readText()

        assertEquals("Downloads/MaterialGuardian/job_alpha_101", alphaExport.downloadsFolder)
        assertEquals("Downloads/MaterialGuardian/job_beta_202", betaExport.downloadsFolder)
        assertEquals(2, alphaExport.materialPacketCount)
        assertEquals(1, betaExport.materialPacketCount)
        assertEquals(1, alphaExport.scanSourceCount)
        assertEquals(5, alphaExport.photoCount)
        assertEquals(2, alphaPackets.size)
        assertEquals(1, betaPackets.size)
        assertTrue(alphaPackets.all { it.name.contains("job_alpha_101") })
        assertTrue(betaPackets.all { it.name.contains("job_beta_202") })
        assertTrue(alphaNotice.contains("Material packet PDFs: 2"))
        assertTrue(alphaNotice.contains("Included scan sources: 1"))
        assertTrue(alphaNotice.contains("Included photos: 5"))

        val alphaPacketSummaries = alphaPackets.map { packet ->
            PDDocument.load(packet).use { document ->
                PacketSummary(
                    file = packet,
                    pageCount = document.numberOfPages,
                    text = PDFTextStripper().getText(document),
                    xObjectCount = countXObjects(document)
                )
            }
        }
        val richPacket = alphaPacketSummaries.maxByOrNull { it.pageCount }
        requireNotNull(richPacket)

        assertTrue(richPacket.text.contains("Job#: JOB ALPHA/101"))
        assertTrue(richPacket.text.contains("Plate"))
        assertTrue(richPacket.text.contains("QC Inspector"))
        assertTrue(richPacket.text.contains("QC Manager"))
        assertEquals(5, richPacket.pageCount)
        assertTrue(richPacket.xObjectCount >= 1)
    }

    private fun createSamplePdf(directory: File, name: String, pageCount: Int): File {
        val file = File(directory, name)
        PDDocument().use { document ->
            repeat(pageCount) {
                document.addPage(PDPage())
            }
            FileOutputStream(file).use { output -> document.save(output) }
        }
        return file
    }

    private fun createSampleImage(directory: File, name: String, color: Int): File {
        val file = File(directory, name)
        val bitmap = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(color)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        bitmap.recycle()
        return file
    }

    private fun createSignatureImage(directory: File, name: String): File {
        val file = File(directory, name)
        val bitmap = Bitmap.createBitmap(420, 140, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        canvas.drawLine(24f, 100f, 140f, 48f, paint)
        canvas.drawLine(140f, 48f, 224f, 92f, paint)
        canvas.drawLine(224f, 92f, 392f, 36f, paint)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        return file
    }

    private fun countXObjects(document: PDDocument): Int {
        val resources = document.getPage(0).resources ?: return 0
        val iterator = resources.xObjectNames.iterator()
        var count = 0
        while (iterator.hasNext()) {
            iterator.next()
            count += 1
        }
        return count
    }

    private data class PacketSummary(
        val file: File,
        val pageCount: Int,
        val text: String,
        val xObjectCount: Int
    )
}
