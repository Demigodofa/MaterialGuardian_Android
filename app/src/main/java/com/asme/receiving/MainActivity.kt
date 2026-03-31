package com.asme.receiving

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.asme.receiving.ui.JobDetailScreen
import com.asme.receiving.ui.JobsScreen
import com.asme.receiving.ui.MaterialFormScreen
import com.asme.receiving.ui.PrivacyPolicyScreen
import com.asme.receiving.ui.SplashScreen
import com.asme.receiving.ui.CustomizationScreen
import com.asme.receiving.ui.theme.MaterialGuardianTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MaterialGuardian)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialGuardianTheme(darkTheme = false, dynamicColor = false) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onTimeout = { showSplash = false })
    } else {
        NavHost(
            navController = navController,
            startDestination = "jobs"
        ) {
            composable("jobs") {
                JobsScreen(
                    onJobClick = { jobNumber ->
                        navController.navigate("job_detail/$jobNumber")
                    },
                    onCustomizationClick = {
                        navController.navigate("customization")
                    },
                    onPrivacyPolicyClick = {
                        navController.navigate("privacy_policy")
                    },
                )
            }

            composable("job_detail/{jobNumber}") { backStackEntry ->
                val jobNumber = backStackEntry.arguments?.getString("jobNumber") ?: ""
                JobDetailScreen(
                    jobNumber = jobNumber,
                    onNavigateBack = { navController.popBackStack() },
                    onAddMaterial = { targetJob ->
                        navController.navigate("material_form/$targetJob")
                    },
                    onOpenDraft = { targetJob ->
                        navController.navigate("material_form/$targetJob?restoreDraft=true")
                    },
                    onEditMaterial = { targetJob, materialId ->
                        navController.navigate("material_form/$targetJob?materialId=$materialId")
                    },
                    onJobRenamed = { newJob ->
                        navController.navigate("job_detail/$newJob") {
                            popUpTo("job_detail/$jobNumber") { inclusive = true }
                        }
                    }
                )
            }

            composable(
                "material_form/{jobNumber}?materialId={materialId}&restoreDraft={restoreDraft}",
                arguments = listOf(
                    navArgument("materialId") {
                        defaultValue = ""
                        nullable = true
                    },
                    navArgument("restoreDraft") {
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val jobNumber = backStackEntry.arguments?.getString("jobNumber") ?: ""
                val materialId = backStackEntry.arguments?.getString("materialId") ?: ""
                val restoreDraft = backStackEntry.arguments?.getBoolean("restoreDraft") ?: false
                MaterialFormScreen(
                    jobNumber = jobNumber,
                    materialId = materialId.ifBlank { null },
                    restoreDraft = restoreDraft,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("privacy_policy") {
                PrivacyPolicyScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable("customization") {
                CustomizationScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
