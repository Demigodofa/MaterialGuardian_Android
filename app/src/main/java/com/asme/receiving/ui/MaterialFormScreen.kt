package com.asme.receiving.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asme.receiving.R
import com.asme.receiving.data.MaterialItem
import com.asme.receiving.data.customization.CustomizationRepository
import com.asme.receiving.data.customization.SurfaceFinishCode
import com.asme.receiving.data.customization.SurfaceFinishUnit
import com.asme.receiving.ui.components.MaterialGuardianHeader
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialFormScreen(
    jobNumber: String,
    materialId: String? = null,
    restoreDraft: Boolean = false,
    onNavigateBack: () -> Unit,
    viewModel: MaterialViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val draftStore = remember(context) { MaterialFormDraftStore(context) }
    val customization = remember(context) { CustomizationRepository(context).load() }
    val draftKey = remember(jobNumber, materialId) { draftStore.draftKey(jobNumber, materialId) }
    var suppressDraftPersistence by remember(draftKey) { mutableStateOf(false) }
    var forceDraftDeleteOnExit by remember(draftKey) { mutableStateOf(false) }

    var materialDescription by remember { mutableStateOf("") }
    var poNumber by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var productType by remember { mutableStateOf("") }
    var specificationPrefix by remember { mutableStateOf("") }
    var gradeType by remember { mutableStateOf("") }
    var fittingStandard by remember { mutableStateOf("N/A") }
    var fittingSuffix by remember { mutableStateOf("") }
    var dimensionUnit by remember { mutableStateOf("imperial") }
    var thickness1 by remember { mutableStateOf("") }
    var thickness2 by remember { mutableStateOf("") }
    var thickness3 by remember { mutableStateOf("") }
    var thickness4 by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var diameter by remember { mutableStateOf("") }
    var diameterType by remember { mutableStateOf("") }
    var visualInspectionAcceptable by remember { mutableStateOf(true) }
    var b16DimensionsAcceptable by remember { mutableStateOf("") }
    var surfaceFinishCode by remember { mutableStateOf("") }
    var surfaceFinishReading by remember { mutableStateOf("") }
    var surfaceFinishUnit by remember { mutableStateOf("") }
    var markings by remember { mutableStateOf("") }
    var markingAcceptable by remember { mutableStateOf(true) }
    var markingAcceptableNa by remember { mutableStateOf(false) }
    var mtrAcceptable by remember { mutableStateOf(true) }
    var mtrAcceptableNa by remember { mutableStateOf(false) }
    var acceptanceStatus by remember { mutableStateOf("accept") }
    var comments by remember { mutableStateOf("") }
    var qcInitials by remember { mutableStateOf("") }
    var qcDate by remember { mutableStateOf(LocalDate.now()) }
    var qcSignaturePath by remember { mutableStateOf("") }
    var materialApproval by remember { mutableStateOf("approved") }
    var qcManager by remember { mutableStateOf("") }
    var qcManagerInitials by remember { mutableStateOf("") }
    var qcManagerDate by remember { mutableStateOf(LocalDate.now()) }
    var qcManagerSignaturePath by remember { mutableStateOf("") }
    var receivedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var offloadStatus by remember { mutableStateOf("pending") }
    var pdfStatus by remember { mutableStateOf("pending") }
    var pdfStoragePath by remember { mutableStateOf("") }
    val photoPaths = remember { mutableStateListOf<String>() }
    val scanCaptures = remember { mutableStateListOf<ScanCapture>() }
    var activeCapture by remember { mutableStateOf<CaptureType?>(null) }
    var replaceIndex by remember { mutableStateOf<Int?>(null) }
    var replaceType by remember { mutableStateOf<CaptureType?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }
    var pendingPhotoReplaceIndex by remember { mutableStateOf<Int?>(null) }
    var pendingPhotoLaunchAfterPermission by remember { mutableStateOf(false) }
    var photoSessionActive by remember { mutableStateOf(false) }
    var showPhotoReview by remember { mutableStateOf(false) }
    var pendingScanCapture by remember { mutableStateOf<ScanCapture?>(null) }
    var pendingScanReplaceIndex by remember { mutableStateOf<Int?>(null) }
    var scanSessionActive by remember { mutableStateOf(false) }
    var showScanReview by remember { mutableStateOf(false) }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var showMediaActionDialog by remember { mutableStateOf(false) }
    var selectedMediaIndex by remember { mutableStateOf<Int?>(null) }
    var selectedMediaType by remember { mutableStateOf<CaptureType?>(null) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showInspectorSignatureChoiceDialog by remember { mutableStateOf(false) }
    var signatureTarget by remember { mutableStateOf(SignatureTarget.QcInspector) }
    var showMaxPhotosDialog by remember { mutableStateOf(false) }
    var showScanLimitDialog by remember { mutableStateOf(false) }
    var showScanFallbackDialog by remember { mutableStateOf(false) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingPhotoPath != null) {
            showPhotoReview = true
        } else {
            pendingPhotoPath?.let { File(it).delete() }
            pendingPhotoUri = null
            pendingPhotoPath = null
            pendingPhotoReplaceIndex = null
            photoSessionActive = false
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingPhotoLaunchAfterPermission) {
            pendingPhotoLaunchAfterPermission = false
            pendingPhotoUri?.let { takePictureLauncher.launch(it) }
        } else {
            pendingPhotoLaunchAfterPermission = false
            pendingPhotoPath?.let { File(it).delete() }
            pendingPhotoUri = null
            pendingPhotoPath = null
            pendingPhotoReplaceIndex = null
            photoSessionActive = false
            saveError = "Camera permission is required to add photos."
        }
    }
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val data = result.data
        if (data == null) {
            scanSessionActive = false
            pendingScanReplaceIndex = null
            return@rememberLauncherForActivityResult
        }
        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
        val pdf = scanResult?.pdf
        if (pdf != null && scanCaptures.size < 8) {
            val targetIndex = pendingScanReplaceIndex?.plus(1) ?: (scanCaptures.size + 1).coerceAtMost(8)
            val target = buildScanPdfFile(
                context = context,
                jobNumber = jobNumber,
                materialDescription = materialDescription,
                index = targetIndex
            )
            context.contentResolver.openInputStream(pdf.uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            val previewPath = scanResult.pages?.firstOrNull()?.imageUri?.let { uri ->
                val previewFile = buildScanPreviewFile(
                    context = context,
                    jobNumber = jobNumber,
                    materialDescription = materialDescription,
                    index = targetIndex
                )
                context.contentResolver.openInputStream(uri)?.use { input ->
                    previewFile.outputStream().use { output -> input.copyTo(output) }
                }
                previewFile.absolutePath
            } ?: ""
            pendingScanCapture =
                ScanCapture(
                    sourcePath = target.absolutePath,
                    previewPath = previewPath
                )
            showScanReview = true
        } else {
            scanSessionActive = false
            pendingScanReplaceIndex = null
        }
    }

    val isDirty = materialDescription.isNotBlank() || poNumber.isNotBlank() || vendor.isNotBlank() ||
        quantity.isNotBlank() || productType.isNotBlank() || specificationPrefix.isNotBlank() ||
        gradeType.isNotBlank() || fittingStandard != "N/A" || fittingSuffix.isNotBlank() ||
        thickness1.isNotBlank() || thickness2.isNotBlank() || thickness3.isNotBlank() ||
        thickness4.isNotBlank() || width.isNotBlank() || length.isNotBlank() ||
        diameter.isNotBlank() || markings.isNotBlank() || comments.isNotBlank() ||
        qcInitials != customization.defaultQcInspectorName ||
        qcManager != customization.defaultQcManagerName ||
        qcManagerInitials.isNotBlank() ||
        !visualInspectionAcceptable || !markingAcceptable || !mtrAcceptable ||
        acceptanceStatus != "accept" || materialApproval != "approved" ||
        dimensionUnit != "imperial" || diameterType.isNotBlank() ||
        b16DimensionsAcceptable.isNotBlank() || surfaceFinishCode.isNotBlank() ||
        surfaceFinishReading.isNotBlank() || photoPaths.isNotEmpty() || scanCaptures.isNotEmpty() ||
        markingAcceptableNa || mtrAcceptableNa || qcSignaturePath.isNotBlank() ||
        qcManagerSignaturePath.isNotBlank()

    val showB16Fields = customization.enableB16Fields ||
        fittingStandard == "B16" ||
        fittingSuffix.isNotBlank() ||
        b16DimensionsAcceptable.isNotBlank()
    val showSurfaceFinishFields = customization.enableSurfaceFinish ||
        surfaceFinishCode.isNotBlank() ||
        surfaceFinishReading.isNotBlank() ||
        surfaceFinishUnit.isNotBlank()
    val resolvedSurfaceFinishUnit = surfaceFinishUnit.ifBlank { customization.surfaceFinishUnit }

    val encodedPhotoPaths = photoPaths.joinToString("|")
    val encodedScanPaths = scanCaptures.joinToString("|") { encodeScanCapture(it) }

    BackHandler(enabled = isDirty) {
        showDiscardDialog = true
    }

    LaunchedEffect(materialId) {
        if (!materialId.isNullOrBlank()) {
            viewModel.load(materialId)
        }
    }

    fun restoreMaterialState(material: MaterialItem) {
        applyMaterialToState(
            material = material,
            onDescription = { materialDescription = it },
            onPo = { poNumber = it },
            onVendor = { vendor = it },
            onQty = { quantity = it },
            onProduct = { productType = it },
            onSpecPrefix = { specificationPrefix = it },
            onGrade = { gradeType = it },
            onFittingStandard = { fittingStandard = it },
            onFittingSuffix = { fittingSuffix = it },
            onDimensionUnit = { dimensionUnit = it },
            onThickness1 = { thickness1 = it },
            onThickness2 = { thickness2 = it },
            onThickness3 = { thickness3 = it },
            onThickness4 = { thickness4 = it },
            onWidth = { width = it },
            onLength = { length = it },
            onDiameter = { diameter = it },
            onDiameterType = { diameterType = it },
            onVisual = { visualInspectionAcceptable = it },
            onB16 = { b16DimensionsAcceptable = it },
            onSurfaceFinishCode = { surfaceFinishCode = it },
            onSurfaceFinishReading = { surfaceFinishReading = it },
            onSurfaceFinishUnit = { surfaceFinishUnit = it },
            onMarkings = { markings = it },
            onMarkingAcceptable = { markingAcceptable = it },
            onMarkingAcceptableNa = { markingAcceptableNa = it },
            onMtrAcceptable = { mtrAcceptable = it },
            onMtrAcceptableNa = { mtrAcceptableNa = it },
            onAcceptance = { acceptanceStatus = it },
            onComments = { comments = it },
            onQcInitials = { qcInitials = it },
            onQcDate = { qcDate = it },
            onQcSignaturePath = { qcSignaturePath = it },
            onMaterialApproval = { materialApproval = it },
            onQcManager = { qcManager = it },
            onQcManagerInitials = { qcManagerInitials = it },
            onQcManagerDate = { qcManagerDate = it },
            onQcManagerSignaturePath = { qcManagerSignaturePath = it },
            onReceivedAt = { receivedAt = it },
            onOffloadStatus = { offloadStatus = it },
            onPdfStatus = { pdfStatus = it },
            onPdfStoragePath = { pdfStoragePath = it }
        )
        photoPaths.clear()
        photoPaths.addAll(decodePaths(material.photoPaths))
        scanCaptures.clear()
        scanCaptures.addAll(decodeScanCaptures(material.scanPaths))
    }

    fun resetNewMaterialState() {
        materialDescription = ""
        poNumber = ""
        vendor = ""
        quantity = ""
        productType = ""
        specificationPrefix = ""
        gradeType = ""
        fittingStandard = "N/A"
        fittingSuffix = ""
        dimensionUnit = "imperial"
        thickness1 = ""
        thickness2 = ""
        thickness3 = ""
        thickness4 = ""
        width = ""
        length = ""
        diameter = ""
        diameterType = ""
        visualInspectionAcceptable = true
        b16DimensionsAcceptable = ""
        surfaceFinishCode = ""
        surfaceFinishReading = ""
        surfaceFinishUnit = ""
        markings = ""
        markingAcceptable = true
        markingAcceptableNa = false
        mtrAcceptable = true
        mtrAcceptableNa = false
        acceptanceStatus = "accept"
        comments = ""
        qcInitials = customization.defaultQcInspectorName
        qcDate = LocalDate.now()
        qcSignaturePath = ""
        materialApproval = "approved"
        qcManager = customization.defaultQcManagerName
        qcManagerInitials = ""
        qcManagerDate = LocalDate.now()
        qcManagerSignaturePath = ""
        receivedAt = System.currentTimeMillis()
        offloadStatus = "pending"
        pdfStatus = "pending"
        pdfStoragePath = ""
        photoPaths.clear()
        scanCaptures.clear()
        saveError = null
        showSaveSuccess = false
    }

    var restoredDraftOrRecord by remember(draftKey) { mutableStateOf(false) }
    var hasStartedDraftSession by remember(draftKey, materialId, restoreDraft) {
        mutableStateOf(restoreDraft || materialId != null)
    }

    LaunchedEffect(draftKey, materialId, uiState.loading, uiState.material?.id) {
        if (restoredDraftOrRecord) return@LaunchedEffect
        if (materialId.isNullOrBlank()) {
            if (restoreDraft) {
                val draft = draftStore.load(draftKey)
                if (draft != null) {
                    restoreMaterialState(draft)
                } else {
                    resetNewMaterialState()
                }
            } else {
                resetNewMaterialState()
            }
            restoredDraftOrRecord = true
            return@LaunchedEffect
        }
        val draft = draftStore.load(draftKey)
        if (uiState.loading) return@LaunchedEffect
        val source = draft ?: uiState.material
        if (source != null) {
            restoreMaterialState(source)
        }
        restoredDraftOrRecord = true
    }

    LaunchedEffect(fittingStandard, fittingSuffix) {
        if (fittingStandard != "B16") {
            fittingSuffix = ""
        }
        if (fittingStandard == "B16" && fittingSuffix.isNotBlank()) {
            if (b16DimensionsAcceptable.isBlank()) {
                b16DimensionsAcceptable = "Yes"
            }
        } else {
            b16DimensionsAcceptable = ""
        }
    }

    LaunchedEffect(showSurfaceFinishFields, customization.surfaceFinishUnit, restoredDraftOrRecord) {
        if (restoredDraftOrRecord && showSurfaceFinishFields && surfaceFinishUnit.isBlank()) {
            surfaceFinishUnit = customization.surfaceFinishUnit
        }
    }

    LaunchedEffect(isDirty) {
        if (isDirty) {
            hasStartedDraftSession = true
        }
    }

    fun buildDraftSnapshot(): MaterialItem {
        return MaterialItem(
            id = materialId ?: "",
            jobNumber = jobNumber,
            description = materialDescription,
            vendor = vendor,
            quantity = quantity,
            poNumber = poNumber,
            productType = productType,
            specificationPrefix = specificationPrefix,
            gradeType = gradeType,
            fittingStandard = fittingStandard,
            fittingSuffix = fittingSuffix,
            dimensionUnit = dimensionUnit,
            thickness1 = thickness1,
            thickness2 = thickness2,
            thickness3 = thickness3,
            thickness4 = thickness4,
            width = width,
            length = length,
            diameter = diameter,
            diameterType = diameterType,
            visualInspectionAcceptable = visualInspectionAcceptable,
            b16DimensionsAcceptable = b16DimensionsAcceptable,
            surfaceFinishCode = surfaceFinishCode,
            surfaceFinishReading = surfaceFinishReading,
            surfaceFinishUnit = resolvedSurfaceFinishUnit,
            specificationNumbers = specificationPrefix,
            markings = markings,
            markingAcceptable = markingAcceptable,
            markingAcceptableNa = markingAcceptableNa,
            mtrAcceptable = mtrAcceptable,
            mtrAcceptableNa = mtrAcceptableNa,
            acceptanceStatus = acceptanceStatus,
            comments = comments,
            qcInitials = qcInitials,
            qcDate = toEpochMillis(qcDate),
            qcSignaturePath = qcSignaturePath,
            materialApproval = materialApproval,
            qcManager = qcManager,
            qcManagerInitials = qcManagerInitials,
            qcManagerDate = toEpochMillis(qcManagerDate),
            qcManagerSignaturePath = qcManagerSignaturePath,
            offloadStatus = offloadStatus,
            pdfStatus = pdfStatus,
            pdfStoragePath = pdfStoragePath,
            photoPaths = photoPaths.joinToString("|"),
            scanPaths = scanCaptures.map { encodeScanCapture(it) }.joinToString("|"),
            photoCount = photoPaths.size,
            receivedAt = receivedAt
        )
    }

    val shouldPersistDraft = !suppressDraftPersistence && hasStartedDraftSession && (materialId != null || restoreDraft || isDirty)
    val latestDraftSnapshot by rememberUpdatedState(newValue = buildDraftSnapshot())
    val latestShouldPersistDraft by rememberUpdatedState(newValue = shouldPersistDraft)
    val latestHasStartedDraftSession by rememberUpdatedState(newValue = hasStartedDraftSession)
    val latestForceDraftDeleteOnExit by rememberUpdatedState(newValue = forceDraftDeleteOnExit)

    DisposableEffect(lifecycleOwner, draftKey, restoredDraftOrRecord) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && restoredDraftOrRecord) {
                // A user-selected draft delete must win over the generic autosave observer.
                if (latestForceDraftDeleteOnExit) {
                    draftStore.clearImmediately(draftKey)
                } else if (latestShouldPersistDraft) {
                    draftStore.saveImmediately(draftKey, latestDraftSnapshot)
                } else if (latestHasStartedDraftSession) {
                    draftStore.clearImmediately(draftKey)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun launchPhotoCapture(replaceAt: Int? = null) {
        if (replaceAt == null && photoPaths.size >= 4) {
            showMaxPhotosDialog = true
            photoSessionActive = false
            return
        }
        val targetIndex = (replaceAt?.plus(1) ?: (photoPaths.size + 1)).coerceAtMost(4)
        val file = buildMediaFile(
            context = context,
            jobNumber = jobNumber,
            materialDescription = materialDescription,
            type = CaptureType.PHOTO,
            index = targetIndex
        )
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        pendingPhotoUri = uri
        pendingPhotoPath = file.absolutePath
        pendingPhotoReplaceIndex = replaceAt
        draftStore.saveImmediately(draftKey, buildDraftSnapshot())
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            takePictureLauncher.launch(uri)
        } else {
            pendingPhotoLaunchAfterPermission = true
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    fun launchScanSession(replaceAt: Int? = null) {
        if (replaceAt == null && scanCaptures.size >= 8) {
            showScanLimitDialog = true
            scanSessionActive = false
            return
        }
        pendingScanReplaceIndex = replaceAt
        draftStore.saveImmediately(draftKey, buildDraftSnapshot())
        launchScanCapture(
            context = context,
            jobNumber = jobNumber,
            materialDescription = materialDescription,
            scanLauncher = scanLauncher,
            onFallback = {
                showScanFallbackDialog = true
                replaceIndex = replaceAt
                replaceType = CaptureType.SCAN
                activeCapture = CaptureType.SCAN
            }
        )
    }

    LaunchedEffect(
        draftKey,
        restoredDraftOrRecord,
        materialDescription,
        poNumber,
        vendor,
        quantity,
        productType,
        specificationPrefix,
        gradeType,
        fittingStandard,
        fittingSuffix,
        dimensionUnit,
        thickness1,
        thickness2,
        thickness3,
        thickness4,
        width,
        length,
        diameter,
        diameterType,
        visualInspectionAcceptable,
        b16DimensionsAcceptable,
        surfaceFinishCode,
        surfaceFinishReading,
        surfaceFinishUnit,
        markings,
        markingAcceptable,
        markingAcceptableNa,
        mtrAcceptable,
        mtrAcceptableNa,
        acceptanceStatus,
        comments,
        qcInitials,
        qcDate,
        qcSignaturePath,
        materialApproval,
        qcManager,
        qcManagerInitials,
        qcManagerDate,
        qcManagerSignaturePath,
        receivedAt,
        offloadStatus,
        pdfStatus,
        pdfStoragePath,
        encodedPhotoPaths,
        encodedScanPaths
    ) {
        if (!restoredDraftOrRecord) return@LaunchedEffect
        if (shouldPersistDraft) {
            draftStore.save(draftKey, buildDraftSnapshot())
        } else if (hasStartedDraftSession) {
            draftStore.clear(draftKey)
        }
    }

    if (showMaxPhotosDialog) {
        AlertDialog(
            onDismissRequest = { showMaxPhotosDialog = false },
            title = { Text("Max photos taken") },
            text = { Text("You have reached the 4-photo limit for this material.") },
            confirmButton = {
                TextButton(onClick = { showMaxPhotosDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showPhotoReview && pendingPhotoPath != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Review photo") },
            text = {
                PhotoFilePreview(
                    path = pendingPhotoPath.orEmpty(),
                    emptyLabel = "Photo preview unavailable."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val path = pendingPhotoPath ?: return@TextButton
                    val replaceAt = pendingPhotoReplaceIndex
                    if (replaceAt != null && replaceAt < photoPaths.size) {
                        val oldPath = photoPaths[replaceAt]
                        if (oldPath != path) {
                            File(oldPath).delete()
                        }
                        photoPaths[replaceAt] = path
                    } else if (photoPaths.size < 4) {
                        photoPaths.add(path)
                    }
                    pendingPhotoUri = null
                    pendingPhotoPath = null
                    pendingPhotoReplaceIndex = null
                    showPhotoReview = false
                    if (photoPaths.size >= 4) {
                        showMaxPhotosDialog = true
                        photoSessionActive = false
                    } else if (photoSessionActive) {
                        launchPhotoCapture()
                    }
                }) {
                    Text("Keep")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = {
                        pendingPhotoPath?.let { File(it).delete() }
                        pendingPhotoUri = null
                        pendingPhotoPath = null
                        showPhotoReview = false
                        launchPhotoCapture(replaceAt = pendingPhotoReplaceIndex)
                    }) {
                        Text("Retake")
                    }
                    TextButton(onClick = {
                        pendingPhotoPath?.let { File(it).delete() }
                        pendingPhotoUri = null
                        pendingPhotoPath = null
                        pendingPhotoReplaceIndex = null
                        showPhotoReview = false
                        photoSessionActive = false
                    }) {
                        Text("Exit")
                    }
                }
            }
        )
    }

    if (showScanReview && pendingScanCapture != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Review scan") },
            text = {
                ScanCapturePreview(capture = pendingScanCapture!!)
            },
            confirmButton = {
                TextButton(onClick = {
                    val capture = pendingScanCapture ?: return@TextButton
                    val replaceAt = pendingScanReplaceIndex
                    if (replaceAt != null && replaceAt < scanCaptures.size) {
                        val existing = scanCaptures[replaceAt]
                        if (existing.sourcePath != capture.sourcePath) {
                            File(existing.sourcePath).delete()
                        }
                        if (existing.previewPath.isNotBlank() && existing.previewPath != capture.previewPath) {
                            File(existing.previewPath).delete()
                        }
                        scanCaptures[replaceAt] = capture
                    } else if (scanCaptures.size < 8) {
                        scanCaptures.add(capture)
                    }
                    pendingScanCapture = null
                    pendingScanReplaceIndex = null
                    showScanReview = false
                    if (scanCaptures.size >= 8) {
                        showScanLimitDialog = true
                        scanSessionActive = false
                    } else if (scanSessionActive) {
                        launchScanSession()
                    }
                }) {
                    Text("Keep")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = {
                        pendingScanCapture?.let {
                            File(it.sourcePath).delete()
                            if (it.previewPath.isNotBlank()) {
                                File(it.previewPath).delete()
                            }
                        }
                        pendingScanCapture = null
                        showScanReview = false
                        launchScanSession(replaceAt = pendingScanReplaceIndex)
                    }) {
                        Text("Retake")
                    }
                    TextButton(onClick = {
                        pendingScanCapture?.let {
                            File(it.sourcePath).delete()
                            if (it.previewPath.isNotBlank()) {
                                File(it.previewPath).delete()
                            }
                        }
                        pendingScanCapture = null
                        pendingScanReplaceIndex = null
                        showScanReview = false
                        scanSessionActive = false
                    }) {
                        Text("Exit")
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialGuardianColors.FormBackground)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        MaterialGuardianHeader(
            onBack = {
                if (isDirty) {
                    showDiscardDialog = true
                } else {
                    onNavigateBack()
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "RECEIVING INSPECTION\nREPORT",
            style = MaterialTheme.typography.titleSmall,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            lineHeight = 30.sp,
            color = MaterialGuardianColors.SectionTitle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LabeledField("Material Description") {
            OutlinedTextField(
                value = materialDescription,
                onValueChange = { materialDescription = it.take(40) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                singleLine = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LabeledField("PO #", modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = poNumber,
                    onValueChange = { poNumber = it.take(20) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    singleLine = true
                )
            }
            LabeledField("Vendor", modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it.take(20) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    singleLine = true
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LabeledField("Qty", modifier = Modifier.weight(0.6f)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.take(6) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    singleLine = true
                )
            }
            LabeledField("Product", modifier = Modifier.weight(1f)) {
                DropdownField(
                    value = productType,
                    options = listOf("Tube", "Pipe", "Plate", "Fitting", "Bar", "Other"),
                    placeholder = "Select"
                ) { productType = it }
            }
            LabeledField("A/SA", modifier = Modifier.weight(0.9f)) {
                DropdownField(
                    value = specificationPrefix,
                    options = listOf("", "A", "SA"),
                    optionLabel = { option -> if (option.isBlank()) "Blank" else option },
                    placeholder = ""
                ) { specificationPrefix = it }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LabeledField("Spec/Grade", modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = gradeType,
                    onValueChange = { gradeType = it.take(12) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    singleLine = true
                )
            }
        }

        if (showB16Fields) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LabeledField("Fitting", modifier = Modifier.weight(0.9f)) {
                    DropdownField(
                        value = fittingStandard,
                        options = listOf("N/A", "B16"),
                        placeholder = "N/A"
                    ) { fittingStandard = it }
                }
                LabeledField("B16 Type", modifier = Modifier.weight(0.8f)) {
                    DropdownField(
                        value = fittingSuffix,
                        options = listOf("5", "9", "11", "34"),
                        placeholder = "",
                        enabled = fittingStandard == "B16"
                    ) { fittingSuffix = it }
                }
            }
        }

        LabeledField("Dimensions") {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                XToggle(
                    label = "Imperial",
                    selected = dimensionUnit == "imperial"
                ) { dimensionUnit = "imperial" }
                XToggle(
                    label = "Metric",
                    selected = dimensionUnit == "metric"
                ) { dimensionUnit = "metric" }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LabeledField("TH 1", modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = thickness1,
                    onValueChange = { thickness1 = it.take(10) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    singleLine = true
                )
            }
            LabeledField("TH 2", modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = thickness2,
                    onValueChange = { thickness2 = it.take(10) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    singleLine = true
                )
            }
            LabeledField("TH 3", modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = thickness3,
                    onValueChange = { thickness3 = it.take(10) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    singleLine = true
                )
            }
            LabeledField("TH 4", modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = thickness4,
                    onValueChange = { thickness4 = it.take(10) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    singleLine = true
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LabeledField("Width", modifier = Modifier.weight(0.9f)) {
                OutlinedTextField(
                    value = width,
                    onValueChange = { width = it.take(10) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            LabeledField("Length", modifier = Modifier.weight(0.9f)) {
                OutlinedTextField(
                    value = length,
                    onValueChange = { length = it.take(10) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            LabeledField("Diameter", modifier = Modifier.weight(0.72f)) {
                OutlinedTextField(
                    value = diameter,
                    onValueChange = { diameter = it.take(10) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End)
                )
            }
            LabeledField("ID/OD", modifier = Modifier.weight(1.08f)) {
                DropdownField(
                    value = diameterType,
                    options = listOf("", "O.D.", "I.D."),
                    optionLabel = { option -> if (option.isBlank()) "None" else option },
                    placeholder = ""
                ) { diameterType = it }
            }
        }

        LabeledField("Visual inspection acceptable") {
            YesNoToggle(
                yesSelected = visualInspectionAcceptable,
                onYes = { visualInspectionAcceptable = true },
                onNo = { visualInspectionAcceptable = false }
            )
        }

        if (showB16Fields) {
            LabeledField("B16 Dimensions") {
                DropdownField(
                    value = b16DimensionsAcceptable,
                    options = listOf("", "Yes", "No"),
                    optionLabel = { option -> if (option.isBlank()) "None" else option },
                    placeholder = ""
                ) { b16DimensionsAcceptable = it }
            }
        }

        if (showSurfaceFinishFields) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LabeledField("Surface Finish", modifier = Modifier.weight(1f)) {
                    DropdownField(
                        value = surfaceFinishCode,
                        options = SurfaceFinishCode.all,
                        optionLabel = SurfaceFinishCode::label,
                        placeholder = ""
                    ) { surfaceFinishCode = it }
                }
            }

            LabeledField("Actual Surface Finish Reading") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = surfaceFinishReading,
                        onValueChange = { surfaceFinishReading = sanitizeFourDecimalInput(it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        singleLine = true
                    )
                    Text(
                        text = SurfaceFinishUnit.label(resolvedSurfaceFinishUnit),
                        color = MaterialGuardianColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        LabeledField("Actual Markings") {
            OutlinedTextField(
                value = markings,
                onValueChange = { markings = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                maxLines = 5
            )
        }

        LabeledField("Marking acceptable to Code/Standard") {
            YesNoNaToggle(
                yesSelected = markingAcceptable && !markingAcceptableNa,
                noSelected = !markingAcceptable && !markingAcceptableNa,
                naSelected = markingAcceptableNa,
                onYes = {
                    markingAcceptable = true
                    markingAcceptableNa = false
                },
                onNo = {
                    markingAcceptable = false
                    markingAcceptableNa = false
                },
                onNa = {
                    markingAcceptable = false
                    markingAcceptableNa = true
                }
            )
        }

        LabeledField("MTR/CoC acceptable to specification") {
            YesNoNaToggle(
                yesSelected = mtrAcceptable && !mtrAcceptableNa,
                noSelected = !mtrAcceptable && !mtrAcceptableNa,
                naSelected = mtrAcceptableNa,
                onYes = {
                    mtrAcceptable = true
                    mtrAcceptableNa = false
                },
                onNo = {
                    mtrAcceptable = false
                    mtrAcceptableNa = false
                },
                onNa = {
                    mtrAcceptable = false
                    mtrAcceptableNa = true
                }
            )
        }

        LabeledField("Disposition") {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                XToggle(
                    label = "Accept",
                    selected = acceptanceStatus == "accept"
                ) { acceptanceStatus = "accept" }
                XToggle(
                    label = "Reject",
                    selected = acceptanceStatus == "reject"
                ) { acceptanceStatus = "reject" }
            }
        }

        LabeledField("Comments") {
            OutlinedTextField(
                value = comments,
                onValueChange = { comments = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                maxLines = 2
            )
        }

        LabeledField("Quality Control") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = qcInitials,
                        onValueChange = { qcInitials = it.take(20) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        singleLine = true
                    )
                    DateField(qcDate, modifier = Modifier.weight(0.7f)) { qcDate = it }
                }
                SignatureField(
                    label = "QC inspector signature",
                    signaturePath = qcSignaturePath,
                    onSign = {
                        if (customization.savedQcInspectorSignaturePath.isNotBlank() &&
                            File(customization.savedQcInspectorSignaturePath).exists()
                        ) {
                            showInspectorSignatureChoiceDialog = true
                        } else {
                            signatureTarget = SignatureTarget.QcInspector
                            showSignatureDialog = true
                        }
                    },
                    onClear = {
                        deleteIfPresent(qcSignaturePath)
                        qcSignaturePath = ""
                    }
                )
            }
        }

        LabeledField("Material approval") {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                XToggle(
                    label = "Approved",
                    selected = materialApproval == "approved"
                ) { materialApproval = "approved" }
                XToggle(
                    label = "Rejected",
                    selected = materialApproval == "rejected"
                ) { materialApproval = "rejected" }
            }
        }

        LabeledField("QC Manager") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = qcManager,
                        onValueChange = { qcManager = it.take(20) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        singleLine = true
                    )
                    DateField(qcManagerDate, modifier = Modifier.weight(0.7f)) { qcManagerDate = it }
                }
                SignatureField(
                    label = "QC manager signature",
                    signaturePath = qcManagerSignaturePath,
                    onSign = {
                        signatureTarget = SignatureTarget.QcManager
                        showSignatureDialog = true
                    },
                    onClear = {
                        deleteIfPresent(qcManagerSignaturePath)
                        qcManagerSignaturePath = ""
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LabeledField("Material photos") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        photoSessionActive = true
                        launchPhotoCapture()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Add material photos (${photoPaths.size}/4)",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Use these for arrival condition, markings, and visible damage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialGuardianColors.TextSecondary
                )
              ThumbnailRow(
                  paths = photoPaths,
                  maxCount = 4,
                    onTap = { index ->
                        selectedMediaIndex = index
                        selectedMediaType = CaptureType.PHOTO
                        showMediaActionDialog = true
                    }
                )
            }
        }

        LabeledField("MTR/CoC scans") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scanSessionActive = true
                        launchScanSession()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Scan MTR/CoC PDFs (${scanCaptures.size}/8)",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Preferred: document scanner. Camera fallback still exports cleanly into the combined MTR PDF.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialGuardianColors.TextSecondary
                )
              ThumbnailRow(
                  paths = scanCaptures.map { it.previewPath.ifBlank { it.sourcePath } },
                  maxCount = 8,
                    onTap = { index ->
                        selectedMediaIndex = index
                        selectedMediaType = CaptureType.SCAN
                        showMediaActionDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    saveError = null
                    val result = viewModel.saveMaterial(
                        materialId = materialId,
                        jobNumber = jobNumber,
                        materialDescription = materialDescription,
                        poNumber = poNumber,
                        vendor = vendor,
                        quantity = quantity,
                        productType = productType,
                        specificationPrefix = specificationPrefix,
                        gradeType = gradeType,
                        fittingStandard = fittingStandard,
                        fittingSuffix = fittingSuffix,
                        dimensionUnit = dimensionUnit,
                        thickness1 = thickness1,
                        thickness2 = thickness2,
                        thickness3 = thickness3,
                        thickness4 = thickness4,
                        width = width,
                        length = length,
                        diameter = diameter,
                        diameterType = diameterType,
                        visualInspectionAcceptable = visualInspectionAcceptable,
                        b16DimensionsAcceptable = if (showB16Fields) b16DimensionsAcceptable else "",
                        surfaceFinishCode = if (showSurfaceFinishFields) surfaceFinishCode else "",
                        surfaceFinishReading = if (showSurfaceFinishFields) surfaceFinishReading else "",
                        surfaceFinishUnit = if (showSurfaceFinishFields) resolvedSurfaceFinishUnit else "",
                        markings = markings,
                        markingAcceptable = markingAcceptable,
                        markingAcceptableNa = markingAcceptableNa,
                        mtrAcceptable = mtrAcceptable,
                        mtrAcceptableNa = mtrAcceptableNa,
                        acceptanceStatus = acceptanceStatus,
                        comments = comments,
                        qcInitials = qcInitials,
                        qcDate = toEpochMillis(qcDate),
                        qcSignaturePath = qcSignaturePath,
                        materialApproval = materialApproval,
                        qcManager = qcManager,
                        qcManagerInitials = qcManagerInitials,
                        qcManagerDate = toEpochMillis(qcManagerDate),
                        qcManagerSignaturePath = qcManagerSignaturePath,
                        receivedAt = receivedAt,
                        offloadStatus = offloadStatus,
                      pdfStatus = pdfStatus,
                      pdfStoragePath = pdfStoragePath,
                      photoPaths = photoPaths,
                      scanPaths = scanCaptures.map { encodeScanCapture(it) }
                    )
                    result.onSuccess {
                        suppressDraftPersistence = true
                        draftStore.clearImmediately(draftKey)
                        showSaveSuccess = true
                    }
                    result.onFailure { saveError = it.message ?: "Unable to save material." }
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.75f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = materialDescription.isNotBlank()
        ) {
            Text("Save Material")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showDiscardDialog) {
        ConfirmExitDialog(
            onLeave = {
                showDiscardDialog = false
                forceDraftDeleteOnExit = false
                draftStore.saveImmediately(draftKey, buildDraftSnapshot())
                onNavigateBack()
            },
            onDeleteDraft = {
                showDiscardDialog = false
                forceDraftDeleteOnExit = true
                suppressDraftPersistence = true
                hasStartedDraftSession = false
                draftStore.clearImmediately(draftKey)
                onNavigateBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    fun acknowledgeSaveSuccess() {
        showSaveSuccess = false
        onNavigateBack()
    }

    if (showSaveSuccess) {
        AlertDialog(
            onDismissRequest = { acknowledgeSaveSuccess() },
            title = { Text("Material saved") },
            text = { Text("This material entry was saved to the job.") },
            confirmButton = {
                TextButton(onClick = { acknowledgeSaveSuccess() }) {
                    Text("OK")
                }
            }
        )
    }

    if (saveError != null) {
        AlertDialog(
            onDismissRequest = { saveError = null },
            title = { Text("Save failed") },
            text = { Text(saveError ?: "Unable to save material.") },
            confirmButton = {
                TextButton(onClick = { saveError = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showMediaActionDialog) {
        val targetIndex = selectedMediaIndex
        val targetType = selectedMediaType
        AlertDialog(
            onDismissRequest = { showMediaActionDialog = false },
            title = { Text("Media options") },
            text = { Text("Would you like to retake or delete this file?") },
            confirmButton = {
                TextButton(onClick = {
                    if (targetIndex != null && targetType != null) {
                        if (targetType == CaptureType.PHOTO) {
                            photoSessionActive = false
                            launchPhotoCapture(replaceAt = targetIndex)
                        } else {
                            scanSessionActive = false
                            launchScanSession(replaceAt = targetIndex)
                        }
                    }
                    showMediaActionDialog = false
                }) {
                    Text("Retake")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        if (targetIndex != null && targetType != null) {
                            if (targetType == CaptureType.PHOTO) {
                                val path = photoPaths.getOrNull(targetIndex)
                                if (path != null) {
                                    File(path).delete()
                                    photoPaths.removeAt(targetIndex)
                                }
                            } else {
                                val capture = scanCaptures.getOrNull(targetIndex)
                                if (capture != null) {
                                    File(capture.sourcePath).delete()
                                    if (capture.previewPath.isNotBlank()) {
                                        File(capture.previewPath).delete()
                                    }
                                    scanCaptures.removeAt(targetIndex)
                                }
                            }
                        }
                        showMediaActionDialog = false
                    }) {
                        Text("Delete")
                    }
                    TextButton(onClick = { showMediaActionDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    val captureType = activeCapture
    if (captureType == CaptureType.SCAN) {
        val maxCount = if (captureType == CaptureType.PHOTO) 4 else 8
        val currentCount = if (captureType == CaptureType.PHOTO) photoPaths.size else scanCaptures.size
        val targetIndex = if (replaceType == captureType) replaceIndex else null
        CameraCaptureOverlay(
            title = if (captureType == CaptureType.PHOTO) "Material Photos" else "MTR/CoC Scans",
            maxCount = maxCount,
            currentCount = currentCount,
            captureIndex = targetIndex?.plus(1),
            onClose = {
                activeCapture = null
                replaceIndex = null
                replaceType = null
                scanSessionActive = false
            },
            onCreateFile = { index: Int ->
                buildMediaFile(
                    context = context,
                    jobNumber = jobNumber,
                    materialDescription = materialDescription,
                    type = captureType,
                    index = index
                )
            },
            onCaptureAccepted = { file: File, _: Int ->
                if (targetIndex != null && targetIndex < scanCaptures.size) {
                    val existing = scanCaptures[targetIndex]
                    if (existing.sourcePath != file.absolutePath) {
                        File(existing.sourcePath).delete()
                    }
                    scanCaptures[targetIndex] = ScanCapture(
                        sourcePath = file.absolutePath,
                        previewPath = file.absolutePath
                    )
                } else if (scanCaptures.size < maxCount) {
                    scanCaptures.add(
                        ScanCapture(
                            sourcePath = file.absolutePath,
                            previewPath = file.absolutePath
                        )
                    )
                }
                replaceIndex = null
                replaceType = null
                val reachedLimit = scanCaptures.size >= maxCount
                if (reachedLimit) {
                    showScanLimitDialog = true
                    scanSessionActive = false
                    activeCapture = null
                } else {
                    activeCapture = if (scanSessionActive) CaptureType.SCAN else null
                }
            }
        )
    }

    if (showScanLimitDialog) {
        AlertDialog(
            onDismissRequest = { showScanLimitDialog = false },
            title = { Text("Scan limit reached") },
            text = { Text("You can attach up to 8 scans per material.") },
            confirmButton = {
                TextButton(onClick = { showScanLimitDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showScanFallbackDialog) {
        AlertDialog(
            onDismissRequest = { showScanFallbackDialog = false },
            title = { Text("Scanner unavailable") },
            text = { Text("Using camera capture instead of the document scanner. These pages will still be bundled into the exported MTR PDF.") },
            confirmButton = {
                TextButton(onClick = { showScanFallbackDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showSignatureDialog) {
        SignatureDialog(
            title = if (signatureTarget == SignatureTarget.QcInspector) {
                "QC Inspector Signature"
            } else {
                "QC Manager Signature"
            },
            onSave = { bitmap ->
                val targetFile = buildSignatureFile(
                    context = context,
                    jobNumber = jobNumber,
                    materialDescription = materialDescription,
                    target = signatureTarget
                )
                val savedPath = saveSignatureBitmap(targetFile, bitmap)
                if (signatureTarget == SignatureTarget.QcInspector) {
                    deleteIfPresent(qcSignaturePath, savedPath)
                    qcSignaturePath = savedPath
                } else {
                    deleteIfPresent(qcManagerSignaturePath, savedPath)
                    qcManagerSignaturePath = savedPath
                }
                showSignatureDialog = false
            },
            onDismiss = { showSignatureDialog = false }
        )
    }

    if (showInspectorSignatureChoiceDialog) {
        val inspectorName = customization.defaultQcInspectorName.ifBlank { "this inspector" }
        AlertDialog(
            onDismissRequest = { showInspectorSignatureChoiceDialog = false },
            title = { Text("Apply saved signature?") },
            text = { Text("$inspectorName has a saved digital signature. Would you like to apply it or draw a new one?") },
            confirmButton = {
                TextButton(onClick = {
                    val sourcePath = customization.savedQcInspectorSignaturePath
                    val targetFile = buildSignatureFile(
                        context = context,
                        jobNumber = jobNumber,
                        materialDescription = materialDescription,
                        target = SignatureTarget.QcInspector
                    )
                    val savedPath = copySignatureFileToMaterial(sourcePath, targetFile)
                    if (savedPath != null) {
                        deleteIfPresent(qcSignaturePath, savedPath)
                        qcSignaturePath = savedPath
                    } else {
                        saveError = "Unable to apply the saved QC inspector signature."
                    }
                    showInspectorSignatureChoiceDialog = false
                }) {
                    Text("Apply Saved")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = {
                        showInspectorSignatureChoiceDialog = false
                        signatureTarget = SignatureTarget.QcInspector
                        showSignatureDialog = true
                    }) {
                        Text("Draw New")
                    }
                    TextButton(onClick = { showInspectorSignatureChoiceDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

}

@Composable
private fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    reserveLabelSpace: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF374151)
            )
        } else if (reserveLabelSpace) {
            Spacer(modifier = Modifier.height(20.dp))
        }
        content()
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    value: String,
    options: List<String>,
    placeholder: String,
    enabled: Boolean = true,
    optionLabel: (String) -> String = { it },
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidth by remember { mutableIntStateOf(0) }

    Box {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    anchorWidth = coordinates.size.width
                }
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                .background(Color.White, RoundedCornerShape(10.dp))
                .clickable(enabled = enabled) { expanded = true }
                .height(52.dp)
                .padding(horizontal = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val displayText = if (value.isBlank()) placeholder else value
                Text(
                    text = displayText,
                    color = if (value.isBlank()) Color(0xFF9CA3AF) else Color(0xFF1F2937),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true),
            modifier = Modifier.width(with(LocalDensity.current) { anchorWidth.toDp() })
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
private fun XToggle(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        XCheckbox(selected = selected, onToggle = onToggle)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = Color(0xFF374151))
    }
}

@Composable
private fun XCheckbox(selected: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(1.dp, Color(0xFF6B7280), RoundedCornerShape(4.dp))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Text(text = "X", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun YesNoToggle(
    yesSelected: Boolean,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        XToggle(label = "Yes", selected = yesSelected, onToggle = onYes)
        XToggle(label = "No", selected = !yesSelected, onToggle = onNo)
    }
}

@Composable
private fun YesNoNaToggle(
    yesSelected: Boolean,
    noSelected: Boolean,
    naSelected: Boolean,
    onYes: () -> Unit,
    onNo: () -> Unit,
    onNa: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        XToggle(label = "Yes", selected = yesSelected, onToggle = onYes)
        XToggle(label = "No", selected = noSelected, onToggle = onNo)
        XToggle(label = "N/A", selected = naSelected, onToggle = onNa)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DateField(
    date: LocalDate,
    modifier: Modifier = Modifier,
    onDateChange: (LocalDate) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MM/dd/yyyy") }
    var showDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = toEpochMillis(date)
    )

    Box(modifier = modifier) {
        OutlinedTextField(
            value = date.format(formatter),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { showDialog = true }
        )
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        onDateChange(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        )
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ThumbnailRow(
    paths: List<String>,
    maxCount: Int,
    onTap: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        repeat(maxCount) { index ->
            val path = paths.getOrNull(index)
            if (path != null) {
                val isPdf = path.endsWith(".pdf", ignoreCase = true)
                val bitmap = remember(path) {
                    if (isPdf) null else decodeOrientedBitmap(path)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Media thumbnail",
                        modifier = Modifier
                            .size(64.dp)
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                            .clickable { onTap(index) },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                            .clickable { onTap(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPdf) "PDF" else "File",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4B5563)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                )
            }
        }
    }
}

@Composable
private fun PhotoFilePreview(
    path: String,
    emptyLabel: String
) {
    val bitmap = remember(path) { decodeOrientedBitmap(path) }
    if (bitmap != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentScale = ContentScale.Fit
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialGuardianColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun ScanCapturePreview(capture: ScanCapture) {
    if (capture.previewPath.isNotBlank()) {
        PhotoFilePreview(
            path = capture.previewPath,
            emptyLabel = "Scan preview unavailable."
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PDF scan ready to keep.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialGuardianColors.TextSecondary
            )
        }
    }
}

@Composable
private fun SignatureField(
    label: String,
    signaturePath: String,
    onSign: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialGuardianColors.TextSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onSign,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (signaturePath.isBlank()) "Capture Signature" else "Re-sign")
            }
            if (signaturePath.isNotBlank()) {
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }
        }
        if (signaturePath.isNotBlank()) {
            SignaturePreview(signaturePath = signaturePath)
        }
    }
}

@Composable
private fun SignaturePreview(signaturePath: String) {
    val bitmap = remember(signaturePath) { BitmapFactory.decodeFile(signaturePath) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Signature preview",
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                .background(Color.White, RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SignatureDialog(
    title: String,
    onSave: (android.graphics.Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentStroke by remember { mutableStateOf(listOf<Offset>()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(title) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .height(170.dp)
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .onGloballyPositioned { canvasSize = it.size }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> currentStroke = listOf(offset) },
                            onDrag = { change, _ ->
                                currentStroke = currentStroke + change.position
                            },
                            onDragEnd = {
                                if (currentStroke.isNotEmpty()) {
                                    strokes = strokes + listOf(currentStroke)
                                    currentStroke = emptyList()
                                }
                            }
                        )
                    }
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val allStrokes = if (currentStroke.isNotEmpty()) strokes + listOf(currentStroke) else strokes
                    allStrokes.forEach { stroke ->
                        for (index in 0 until stroke.size - 1) {
                            drawLine(
                                color = Color.Black,
                                start = stroke[index],
                                end = stroke[index + 1],
                                strokeWidth = 4f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalized = if (currentStroke.isNotEmpty()) strokes + listOf(currentStroke) else strokes
                    if (finalized.isEmpty()) return@TextButton
                    onSave(renderSignatureBitmap(finalized, canvasSize))
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = {
                    strokes = emptyList()
                    currentStroke = emptyList()
                }) {
                    Text("Clear")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun ConfirmExitDialog(
    onLeave: () -> Unit,
    onDeleteDraft: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exit receiving report?") },
        text = {
            Text("This report will be autosaved as a draft when you leave. Keep editing, leave now, or delete the draft.")
        },
        confirmButton = {
            Button(
                onClick = onLeave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialGuardianColors.Success,
                    contentColor = MaterialGuardianColors.PrimaryButtonText
                )
            ) {
                Text("Leave")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Keep Editing")
                }
                Button(
                    onClick = onDeleteDraft,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialGuardianColors.DeleteButton,
                        contentColor = MaterialGuardianColors.DeleteButtonText
                    )
                ) {
                    Text("Delete Draft")
                }
            }
        }
    )
}

private fun toEpochMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun buildMediaFile(
    context: android.content.Context,
    jobNumber: String,
    materialDescription: String,
    type: CaptureType,
    index: Int
): File {
    val safeJob = sanitizeFileComponent(jobNumber)
    val safeDesc = sanitizeFileComponent(materialDescription).ifBlank { "material" }
    val baseName = safeDesc.take(24)
    val folder = File(context.filesDir, "job_media/$safeJob/${type.folder}")
    folder.mkdirs()
    val fileName = "${baseName}_${type.label}_${index}.jpg"
    return File(folder, fileName)
}

private fun sanitizeFileComponent(value: String): String {
    return value.lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
}

private enum class CaptureType(val folder: String, val label: String) {
    PHOTO("photos", "photo"),
    SCAN("scans", "scan")
}

private enum class SignatureTarget(val fileLabel: String) {
    QcInspector("qc"),
    QcManager("qc_manager")
}

private fun buildScanPdfFile(
    context: android.content.Context,
    jobNumber: String,
    materialDescription: String,
    index: Int
): File {
    val safeJob = sanitizeFileComponent(jobNumber)
    val safeDesc = sanitizeFileComponent(materialDescription).ifBlank { "material" }
    val baseName = safeDesc.take(24)
    val folder = File(context.filesDir, "job_media/$safeJob/scans")
    folder.mkdirs()
    return File(folder, "${baseName}_scan_${index}.pdf")
}

private fun buildScanPreviewFile(
    context: android.content.Context,
    jobNumber: String,
    materialDescription: String,
    index: Int
): File {
    val safeJob = sanitizeFileComponent(jobNumber)
    val safeDesc = sanitizeFileComponent(materialDescription).ifBlank { "material" }
    val baseName = safeDesc.take(24)
    val folder = File(context.filesDir, "job_media/$safeJob/scan_previews")
    folder.mkdirs()
    return File(folder, "${baseName}_scan_${index}.jpg")
}

private fun buildSignatureFile(
    context: android.content.Context,
    jobNumber: String,
    materialDescription: String,
    target: SignatureTarget
): File {
    val safeJob = sanitizeFileComponent(jobNumber)
    val safeDesc = sanitizeFileComponent(materialDescription).ifBlank { "material" }
    val baseName = safeDesc.take(24)
    val folder = File(context.filesDir, "job_media/$safeJob/signatures")
    folder.mkdirs()
    return File(folder, "${baseName}_${target.fileLabel}_signature.png")
}

private fun launchScanCapture(
    context: android.content.Context,
    jobNumber: String,
    materialDescription: String,
    scanLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>,
    onFallback: () -> Unit
) {
    val options = GmsDocumentScannerOptions.Builder()
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .setPageLimit(8)
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
        )
        .build()
    val scanner = GmsDocumentScanning.getClient(options)
    val activity = findActivity(context)
    if (activity == null) {
        onFallback()
        return
    }
    scanner.getStartScanIntent(activity)
        .addOnSuccessListener { intentSender ->
            scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
        .addOnFailureListener {
            onFallback()
        }
}

private fun findActivity(context: android.content.Context): android.app.Activity? {
    var current = context
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

private data class ScanCapture(
    val sourcePath: String,
    val previewPath: String
)

private fun saveSignatureBitmap(file: File, bitmap: android.graphics.Bitmap): String {
    file.outputStream().use { output ->
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
    }
    return file.absolutePath
}

private fun renderSignatureBitmap(
    strokes: List<List<Offset>>,
    size: IntSize
): android.graphics.Bitmap {
    val scale = 3
    val width = size.width.coerceAtLeast(1) * scale
    val height = size.height.coerceAtLeast(1) * scale
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 12f
        style = android.graphics.Paint.Style.STROKE
        strokeJoin = android.graphics.Paint.Join.ROUND
        strokeCap = android.graphics.Paint.Cap.ROUND
        isAntiAlias = true
    }
    strokes.forEach { stroke ->
        for (index in 0 until stroke.size - 1) {
            val start = stroke[index]
            val end = stroke[index + 1]
            canvas.drawLine(start.x * scale, start.y * scale, end.x * scale, end.y * scale, paint)
        }
    }
    return bitmap
}

private fun deleteIfPresent(path: String, skipIfEquals: String = "") {
    if (path.isBlank() || path == skipIfEquals) return
    File(path).delete()
}

private fun copySignatureFileToMaterial(sourcePath: String, targetFile: File): String? {
    if (sourcePath.isBlank()) return null
    val sourceFile = File(sourcePath)
    if (!sourceFile.exists()) return null
    targetFile.parentFile?.mkdirs()
    sourceFile.copyTo(targetFile, overwrite = true)
    return targetFile.absolutePath
}

private fun encodeScanCapture(capture: ScanCapture): String {
    return listOf(capture.sourcePath, capture.previewPath).joinToString("\t")
}

private fun decodePaths(value: String): List<String> {
    return value.split("|").filter { it.isNotBlank() }
}

private fun decodeScanCaptures(value: String): List<ScanCapture> {
    return decodePaths(value).map { entry ->
        val parts = entry.split("\t", limit = 2)
        ScanCapture(
            sourcePath = parts.firstOrNull().orEmpty(),
            previewPath = parts.getOrNull(1).orEmpty()
        )
    }
}

private fun applyMaterialToState(
    material: MaterialItem,
    onDescription: (String) -> Unit,
    onPo: (String) -> Unit,
    onVendor: (String) -> Unit,
    onQty: (String) -> Unit,
    onProduct: (String) -> Unit,
    onSpecPrefix: (String) -> Unit,
    onGrade: (String) -> Unit,
    onFittingStandard: (String) -> Unit,
    onFittingSuffix: (String) -> Unit,
    onDimensionUnit: (String) -> Unit,
    onThickness1: (String) -> Unit,
    onThickness2: (String) -> Unit,
    onThickness3: (String) -> Unit,
    onThickness4: (String) -> Unit,
    onWidth: (String) -> Unit,
    onLength: (String) -> Unit,
    onDiameter: (String) -> Unit,
    onDiameterType: (String) -> Unit,
    onVisual: (Boolean) -> Unit,
    onB16: (String) -> Unit,
    onSurfaceFinishCode: (String) -> Unit,
    onSurfaceFinishReading: (String) -> Unit,
    onSurfaceFinishUnit: (String) -> Unit,
    onMarkings: (String) -> Unit,
    onMarkingAcceptable: (Boolean) -> Unit,
    onMarkingAcceptableNa: (Boolean) -> Unit,
    onMtrAcceptable: (Boolean) -> Unit,
    onMtrAcceptableNa: (Boolean) -> Unit,
    onAcceptance: (String) -> Unit,
    onComments: (String) -> Unit,
    onQcInitials: (String) -> Unit,
    onQcDate: (LocalDate) -> Unit,
    onQcSignaturePath: (String) -> Unit,
    onMaterialApproval: (String) -> Unit,
    onQcManager: (String) -> Unit,
    onQcManagerInitials: (String) -> Unit,
    onQcManagerDate: (LocalDate) -> Unit,
    onQcManagerSignaturePath: (String) -> Unit,
    onReceivedAt: (Long) -> Unit,
    onOffloadStatus: (String) -> Unit,
    onPdfStatus: (String) -> Unit,
    onPdfStoragePath: (String) -> Unit
) {
    onDescription(material.description)
    onPo(material.poNumber)
    onVendor(material.vendor)
    onQty(material.quantity)
    onProduct(material.productType)
    onSpecPrefix(material.specificationPrefix)
    onGrade(material.gradeType)
    onFittingStandard(material.fittingStandard)
    onFittingSuffix(material.fittingSuffix)
    onDimensionUnit(material.dimensionUnit)
    onThickness1(material.thickness1)
    onThickness2(material.thickness2)
    onThickness3(material.thickness3)
    onThickness4(material.thickness4)
    onWidth(material.width)
    onLength(material.length)
    onDiameter(material.diameter)
    onDiameterType(material.diameterType)
    onVisual(material.visualInspectionAcceptable)
    onB16(material.b16DimensionsAcceptable)
    onSurfaceFinishCode(material.surfaceFinishCode)
    onSurfaceFinishReading(material.surfaceFinishReading)
    onSurfaceFinishUnit(material.surfaceFinishUnit)
    onMarkings(material.markings)
    onMarkingAcceptable(material.markingAcceptable)
    onMarkingAcceptableNa(material.markingAcceptableNa)
    onMtrAcceptable(material.mtrAcceptable)
    onMtrAcceptableNa(material.mtrAcceptableNa)
    onAcceptance(material.acceptanceStatus)
    onComments(material.comments)
    onQcInitials(material.qcInitials)
    onQcDate(Instant.ofEpochMilli(material.qcDate).atZone(ZoneId.systemDefault()).toLocalDate())
    onQcSignaturePath(material.qcSignaturePath)
    onMaterialApproval(material.materialApproval)
    onQcManager(material.qcManager)
    onQcManagerInitials(material.qcManagerInitials)
    onQcManagerDate(Instant.ofEpochMilli(material.qcManagerDate).atZone(ZoneId.systemDefault()).toLocalDate())
    onQcManagerSignaturePath(material.qcManagerSignaturePath)
    onReceivedAt(material.receivedAt)
    onOffloadStatus(material.offloadStatus)
    onPdfStatus(material.pdfStatus)
    onPdfStoragePath(material.pdfStoragePath)
}

private fun sanitizeFourDecimalInput(input: String): String {
    val filtered = buildString(input.length) {
        input.forEachIndexed { index, char ->
            if (char.isDigit()) {
                append(char)
            } else if (char == '.' && index > 0 && !contains('.')) {
                append(char)
            }
        }
    }
    val decimalIndex = filtered.indexOf('.')
    if (decimalIndex < 0) {
        return filtered.take(10)
    }
    val whole = filtered.substring(0, decimalIndex).take(6)
    val fractional = filtered.substring(decimalIndex + 1).take(4)
    return if (fractional.isEmpty()) "$whole." else "$whole.$fractional"
}

