package com.asme.receiving.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asme.receiving.data.customization.AppCustomization
import com.asme.receiving.data.customization.CustomizationRepository
import com.asme.receiving.data.customization.SurfaceFinishUnit
import com.asme.receiving.ui.components.MaterialGuardianHeader
import java.io.File

@Composable
fun CustomizationScreen(
    onNavigateBack: () -> Unit,
    repository: CustomizationRepository = CustomizationRepository()
) {
    val initialState = remember { repository.load() }
    val navigationBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var enableB16Fields by remember { mutableStateOf(initialState.enableB16Fields) }
    var enableSurfaceFinish by remember { mutableStateOf(initialState.enableSurfaceFinish) }
    var surfaceFinishUnit by remember { mutableStateOf(initialState.surfaceFinishUnit) }
    var companyLogoPath by remember { mutableStateOf(initialState.companyLogoPath) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val currentLogoBitmap = remember(companyLogoPath) {
        companyLogoPath.takeIf { it.isNotBlank() && File(it).exists() }?.let(BitmapFactory::decodeFile)
    }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        saveError = null
        saveMessage = null
        repository.importCompanyLogo(uri)
            .onSuccess {
                companyLogoPath = it
                saveMessage = "Logo imported."
            }
            .onFailure { saveError = it.message ?: "Unable to import logo." }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialGuardianColors.FormBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        MaterialGuardianHeader(onBack = onNavigateBack)

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "CUSTOMIZATION",
            style = MaterialTheme.typography.titleSmall,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialGuardianColors.SectionTitle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        PreferenceSection(
            title = "Receiving Options",
            body = "Choose which optional inspection sections should appear when your team creates material reports."
        )

        Spacer(modifier = Modifier.height(12.dp))

        PreferenceCheckboxRow(
            label = "Receive ASME B16 parts",
            checked = enableB16Fields,
            onToggle = {
                enableB16Fields = !enableB16Fields
                saveMessage = null
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        PreferenceCheckboxRow(
            label = "Surface finish required",
            checked = enableSurfaceFinish,
            onToggle = {
                enableSurfaceFinish = !enableSurfaceFinish
                saveMessage = null
            }
        )

        if (enableSurfaceFinish) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Surface finish unit",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialGuardianColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PreferenceChoiceChip(
                    label = "Microinch",
                    selected = surfaceFinishUnit == SurfaceFinishUnit.MICROINCH,
                    onClick = {
                        surfaceFinishUnit = SurfaceFinishUnit.MICROINCH
                        saveMessage = null
                    }
                )
                PreferenceChoiceChip(
                    label = "Microns",
                    selected = surfaceFinishUnit == SurfaceFinishUnit.MICRONS,
                    onClick = {
                        surfaceFinishUnit = SurfaceFinishUnit.MICRONS
                        saveMessage = null
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        PreferenceSection(
            title = "Report Logo",
            body = "Upload a logo for exported receiving reports. The app will resize and normalize the image automatically."
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialGuardianColors.CardBackground,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentLogoBitmap != null) {
                    Image(
                        bitmap = currentLogoBitmap.asImageBitmap(),
                        contentDescription = "Selected company logo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Text(
                        text = "No logo selected yet.",
                        color = MaterialGuardianColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = { logoPicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialGuardianColors.PrimaryButton,
                        contentColor = MaterialGuardianColors.PrimaryButtonText
                    )
                ) {
                    Text(if (companyLogoPath.isBlank()) "Upload Logo" else "Replace Logo")
                }

                if (companyLogoPath.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            repository.clearCompanyLogo()
                            companyLogoPath = ""
                            saveMessage = "Logo removed."
                        }
                    ) {
                        Text("Remove Logo")
                    }
                }
            }
        }

        saveError?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialGuardianColors.DeleteButton,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        saveMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialGuardianColors.Success,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                repository.save(
                    AppCustomization(
                        enableB16Fields = enableB16Fields,
                        enableSurfaceFinish = enableSurfaceFinish,
                        surfaceFinishUnit = surfaceFinishUnit,
                        companyLogoPath = companyLogoPath
                    )
                )
                saveError = null
                saveMessage = "Customization saved."
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.78f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialGuardianColors.ExportButton,
                contentColor = MaterialGuardianColors.ExportButtonText
            )
        ) {
            Text("Save Customization")
        }

        Spacer(modifier = Modifier.height(24.dp + navigationBottomPadding))
    }
}

@Composable
private fun PreferenceSection(title: String, body: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialGuardianColors.Title,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialGuardianColors.TextSecondary
        )
    }
}

@Composable
private fun PreferenceCheckboxRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialGuardianColors.TextPrimary
        )
        PreferenceChoiceChip(
            label = if (checked) "Enabled" else "Disabled",
            selected = checked,
            onClick = onToggle
        )
    }
}

@Composable
private fun PreferenceChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialGuardianColors.PrimaryButton else MaterialGuardianColors.CardBackground,
            contentColor = if (selected) MaterialGuardianColors.PrimaryButtonText else MaterialGuardianColors.TextPrimary
        )
    ) {
        Text(label)
    }
}
