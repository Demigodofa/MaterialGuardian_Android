package com.asme.receiving.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asme.receiving.ui.components.MaterialGuardianHeader

@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialGuardianColors.FormBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MaterialGuardianHeader(onBack = onNavigateBack)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "PRIVACY POLICY",
            style = MaterialTheme.typography.titleSmall,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialGuardianColors.SectionTitle,
            modifier = Modifier.fillMaxWidth(),
        )

        PolicySection(
            title = "Overview",
            body = "Material Guardian is an offline-first receiving inspection app. " +
                "The app stores jobs, receiving reports, material photos, scanned PDFs, signatures, and exported packet files on your device.",
        )
        PolicySection(
            title = "Camera and document scanning",
            body = "If you choose to capture material photos or scan MTR/CoC documents, the app uses the device camera and document scanner only for that workflow.",
        )
        PolicySection(
            title = "Data handling",
            body = "Material Guardian does not require an account. " +
                "The current release does not upload your jobs, reports, photos, scans, or signatures to a cloud service operated by the developer.",
        )
        PolicySection(
            title = "Sharing and exports",
            body = "When you export a job, the app writes packet files to app storage and a copy under Downloads/MaterialGuardian on your device. " +
                "If you use Android share actions, the selected files are shared only to the destination you choose.",
        )
        PolicySection(
            title = "Retention and control",
            body = "You control local data on the device. " +
                "You can keep drafts, delete drafts, delete jobs, or remove exported files from device storage. " +
                "Android backup and device-to-device transfer are disabled for app data in this release.",
        )
        PolicySection(
            title = "Contact",
            body = "For support or privacy questions, use the contact details published with the store listing or your company deployment channel.",
        )
    }
}

@Composable
private fun PolicySection(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialGuardianColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialGuardianColors.TextSecondary,
        )
    }
}
