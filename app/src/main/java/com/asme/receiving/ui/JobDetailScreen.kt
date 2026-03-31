package com.asme.receiving.ui

import android.app.DownloadManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asme.receiving.data.MaterialItem
import com.asme.receiving.data.export.ExportResult
import com.asme.receiving.data.export.ExportService
import com.asme.receiving.ui.components.MaterialGuardianHeader
import java.io.File
import java.io.FileInputStream
import java.util.ArrayList
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.launch

@Composable
fun JobDetailScreen(
    jobNumber: String,
    onNavigateBack: () -> Unit,
    onAddMaterial: (String) -> Unit,
    onOpenDraft: (String) -> Unit,
    onEditMaterial: (String, String) -> Unit,
    onJobRenamed: (String) -> Unit,
    viewModel: JobDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val uiStateFlow = remember(jobNumber) { viewModel.observe(jobNumber) }
    val uiState by uiStateFlow.collectAsState()
    val job = uiState.job
    val draftStore = remember(context) { MaterialFormDraftStore(context) }

    var showEditDescription by remember { mutableStateOf(false) }
    var showEditJobNumber by remember { mutableStateOf(false) }
    var showExportConfirm by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var exportSuccess by remember { mutableStateOf<ExportResult?>(null) }
    var descriptionDraft by remember { mutableStateOf("") }
    var jobNumberDraft by remember { mutableStateOf("") }
    var materialToDelete by remember { mutableStateOf<MaterialItem?>(null) }
    var newReportDraft by remember(jobNumber) { mutableStateOf<MaterialItem?>(null) }

    fun refreshDraftSummary() {
        newReportDraft = draftStore.load(draftStore.draftKey(jobNumber, null))
    }

    LaunchedEffect(jobNumber) {
        refreshDraftSummary()
    }

    DisposableEffect(lifecycleOwner, jobNumber) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshDraftSummary()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(showEditDescription, job?.description) {
        if (showEditDescription && job != null) {
            descriptionDraft = job.description
        }
    }

    LaunchedEffect(showEditJobNumber, job?.jobNumber) {
        if (showEditJobNumber && job != null) {
            jobNumberDraft = job.jobNumber
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialGuardianColors.FormBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        MaterialGuardianHeader(onBack = onNavigateBack)

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "JOB DETAILS",
            style = MaterialTheme.typography.titleSmall,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialGuardianColors.SectionTitle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (job != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Job# ${job.jobNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialGuardianColors.Link,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { showEditJobNumber = true }
                )
                val statusText = if (job.exportedAt == null) "Not exported" else "Exported"
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (job.exportedAt == null) MaterialGuardianColors.Warning else MaterialGuardianColors.Success
                )
            }

            if (job.description.isNotBlank()) {
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialGuardianColors.TextPrimary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable { showEditDescription = true }
                )
            } else {
                Text(
                    text = "Add job description",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialGuardianColors.Link,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable { showEditDescription = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        Button(
            onClick = { onAddMaterial(jobNumber) },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.8f)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialGuardianColors.PrimaryButton,
                contentColor = MaterialGuardianColors.PrimaryButtonText
            )
        ) {
            Text("Add Receiving Report")
        }

        newReportDraft?.let { draft ->
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onOpenDraft(jobNumber) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialGuardianColors.EditButton,
                        contentColor = MaterialGuardianColors.EditButtonText
                    )
                ) {
                    Text("Resume Draft")
                }
                TextButton(
                    onClick = {
                        draftStore.clearImmediately(draftStore.draftKey(jobNumber, null))
                        refreshDraftSummary()
                    }
                ) {
                    Text("Delete Draft", color = MaterialGuardianColors.DeleteButton)
                }
            }

            val draftSummary = draft.description.ifBlank { "Unsaved receiving report draft" }
            Text(
                text = draftSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialGuardianColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Materials Received",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialGuardianColors.Title
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.materials) { material ->
                MaterialSummaryRow(
                    material = material,
                    onClick = { onEditMaterial(jobNumber, material.id) },
                    onDelete = { materialToDelete = material }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { showExportConfirm = true },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.75f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialGuardianColors.ExportButton,
                contentColor = MaterialGuardianColors.ExportButtonText
            )
        ) {
            Text("Export Job")
        }

        if (job?.exportPath?.isNotBlank() == true) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Latest export folder",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialGuardianColors.TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    val opened = openExportFolder(context, job.exportPath)
                    if (!opened) {
                        exportError = "Could not open the export folder on this device."
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.78f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialGuardianColors.EditButton,
                    contentColor = MaterialGuardianColors.EditButtonText
                )
            ) {
                Text("Open Export Folder")
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = job.exportPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialGuardianColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val shared = shareLatestExportPdfs(context, job.exportPath)
                        if (!shared) {
                            exportError = "Could not share the latest exported packet PDFs on this device."
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialGuardianColors.PrimaryButton,
                        contentColor = MaterialGuardianColors.PrimaryButtonText
                    )
                ) {
                    Text("Share PDFs")
                }

                Button(
                    onClick = {
                        val shared = shareLatestExportZip(context, job.exportPath)
                        if (!shared) {
                            exportError = "Could not share the latest exported ZIP on this device."
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialGuardianColors.EditButton,
                        contentColor = MaterialGuardianColors.EditButtonText
                    )
                ) {
                    Text("Share ZIP")
                }
            }
        }
    }

    if (showEditDescription && job != null) {
        AlertDialog(
            onDismissRequest = { showEditDescription = false },
            title = { Text("Edit job description") },
            text = {
                OutlinedTextField(
                    value = descriptionDraft,
                    onValueChange = { descriptionDraft = it.take(120) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { viewModel.updateDescription(job.jobNumber, descriptionDraft) }
                    showEditDescription = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDescription = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditJobNumber && job != null) {
        AlertDialog(
            onDismissRequest = { showEditJobNumber = false },
            title = { Text("Edit job number") },
            text = {
                OutlinedTextField(
                    value = jobNumberDraft,
                    onValueChange = { jobNumberDraft = it.take(30) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val success = viewModel.renameJob(job.jobNumber, jobNumberDraft)
                        if (success) {
                            onJobRenamed(jobNumberDraft)
                        }
                    }
                    showEditJobNumber = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditJobNumber = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExportConfirm && job != null) {
        val warning = if (job.exportedAt == null) {
            "Export job files to local storage and Downloads?"
        } else {
            "This job was already exported. Export again?"
        }
        AlertDialog(
            onDismissRequest = { showExportConfirm = false },
            title = { Text("Export job") },
            text = { Text(warning) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val exportResult = ExportService().exportJob(job.jobNumber)
                            viewModel.markExported(job.jobNumber, exportResult.downloadsFolder)
                            exportSuccess = exportResult
                        } catch (e: Exception) {
                            exportError = e.message ?: "Export failed."
                        }
                    }
                    showExportConfirm = false
                }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (exportError != null) {
        AlertDialog(
            onDismissRequest = { exportError = null },
            title = { Text("Export failed") },
            text = { Text(exportError ?: "Export failed.") },
            confirmButton = {
                TextButton(onClick = { exportError = null }) {
                    Text("OK")
                }
            }
        )
    }

    exportSuccess?.let { result ->
        AlertDialog(
            onDismissRequest = { exportSuccess = null },
            title = { Text("Export complete") },
            text = { Text(buildExportSuccessMessage(result)) },
            confirmButton = {
                TextButton(onClick = {
                    val opened = openExportFolder(context, result.downloadsFolder)
                    if (!opened) {
                        exportError = "Could not open the export folder on this device."
                    }
                }) {
                    Text("Open Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportSuccess = null }) {
                    Text("Done")
                }
            }
        )
    }

    materialToDelete?.let { material ->
        AlertDialog(
            onDismissRequest = { materialToDelete = null },
            title = { Text("Delete receiving report?") },
            text = {
                Text(
                    "Delete ${material.description.ifBlank { "this material" }} from this job?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.deleteMaterial(material.id)
                        materialToDelete = null
                    }
                }) {
                    Text("Delete", color = MaterialGuardianColors.DeleteButton)
                }
            },
            dismissButton = {
                TextButton(onClick = { materialToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun buildExportSuccessMessage(result: ExportResult): String {
    val parts = buildList {
        add("${result.materialPacketCount} material packet PDF" + if (result.materialPacketCount == 1) "" else "s")
        if (result.scanSourceCount > 0) {
            add("${result.scanSourceCount} scan source" + if (result.scanSourceCount == 1) "" else "s")
        }
        if (result.photoCount > 0) {
            add("${result.photoCount} photo" + if (result.photoCount == 1) "" else "s")
        }
    }
    return "Exported ${parts.joinToString(", ")}.\n\nPhone-accessible folder:\n${result.downloadsFolder}"
}

private fun openExportFolder(context: Context, exportPath: String): Boolean {
    val normalizedPath = exportPath.trim()
        .replace('\\', '/')
        .trim('/')
    if (normalizedPath.isBlank()) {
        return false
    }

    val relativeToDownloads = normalizedPath
        .removePrefix("Downloads/")
        .removePrefix("Download/")
        .trim('/')

    val folderDocumentId = buildDownloadsDocumentId(relativeToDownloads)
    val folderUri = DocumentsContract.buildDocumentUri(
        "com.android.externalstorage.documents",
        folderDocumentId
    )

    val exactFolderIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri)
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (launchIfSupported(context, exactFolderIntent)) {
        return true
    }

    val pickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri)
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (launchIfSupported(context, pickerIntent)) {
        return true
    }

    val downloadsIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return launchIfSupported(context, downloadsIntent)
}

private fun buildDownloadsDocumentId(relativeToDownloads: String): String {
    val cleanedRelativePath = relativeToDownloads.trim('/')
    return if (cleanedRelativePath.isBlank()) {
        "primary:Download"
    } else {
        "primary:Download/$cleanedRelativePath"
    }
}

private fun launchIfSupported(context: Context, intent: Intent): Boolean {
    return runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

private fun shareLatestExportPdfs(context: Context, exportPath: String): Boolean {
    val packetFiles = latestExportPacketFiles(context, exportPath)
    val authority = "${context.packageName}.fileprovider"
    if (packetFiles.isEmpty()) return false
    val uris = ArrayList<Uri>(packetFiles.size)
    var clipData: ClipData? = null
    packetFiles.forEach { packetFile ->
        val uri = FileProvider.getUriForFile(context, authority, packetFile)
        uris += uri
        clipData = clipData?.apply { addItem(ClipData.Item(uri)) }
            ?: ClipData.newUri(context.contentResolver, packetFile.name, uri)
    }

    val shareFilesIntent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/pdf"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
    }.apply {
        clipData?.let { this.clipData = it }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val chooserIntent = Intent.createChooser(shareFilesIntent, "Share latest export files").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return launchIfSupported(context, chooserIntent)
}

private fun shareLatestExportZip(context: Context, exportPath: String): Boolean {
    val authority = "${context.packageName}.fileprovider"
    val zipFile = latestExportShareBundle(context, exportPath) ?: return false
    val uri = FileProvider.getUriForFile(context, authority, zipFile)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, zipFile.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val chooserIntent = Intent.createChooser(intent, "Share latest export bundle").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return launchIfSupported(context, chooserIntent)
}

private fun latestExportPacketFiles(context: Context, exportPath: String): List<File> {
    val exportRoot = latestInternalExportRoot(context, exportPath) ?: return emptyList()
    return exportRoot.resolve("material_packets")
        .listFiles()
        ?.filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }
        ?.sortedBy { it.name }
        .orEmpty()
}

private fun latestExportShareBundle(context: Context, exportPath: String): File? {
    val exportRoot = latestInternalExportRoot(context, exportPath) ?: return null
    val packetFiles = latestExportPacketFiles(context, exportPath)
    if (packetFiles.isEmpty()) {
        return null
    }

    val bundleDir = File(exportRoot, "share_bundle").also { it.mkdirs() }
    val bundleName = "${exportRoot.name}_latest_export.zip"
    val zipFile = File(bundleDir, bundleName)
    if (zipFile.exists()) {
        zipFile.delete()
    }

    ZipOutputStream(zipFile.outputStream().buffered()).use { zipStream ->
        packetFiles.forEach { packetFile ->
            zipStream.putNextEntry(ZipEntry(packetFile.name))
            FileInputStream(packetFile).use { input ->
                input.copyTo(zipStream)
            }
            zipStream.closeEntry()
        }
    }

    return zipFile
}

private fun latestInternalExportRoot(context: Context, exportPath: String): File? {
    val exportSegment = exportPath.trim()
        .replace('\\', '/')
        .trim('/')
        .substringAfterLast('/', "")
    if (exportSegment.isBlank()) {
        return null
    }

    val exportRoot = File(context.filesDir, "exports/$exportSegment")
    return exportRoot.takeIf { it.exists() }
}

@Composable
private fun MaterialSummaryRow(
    material: MaterialItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialGuardianColors.CardBackground, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = material.description.ifBlank { "Material" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialGuardianColors.TextPrimary
            )
            Text(
                text = "Qty ${material.quantity.ifBlank { "-" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialGuardianColors.TextSecondary
            )
        }
        TextButton(onClick = onDelete) {
            Text("Delete", color = MaterialGuardianColors.DeleteButton)
        }
    }
}
