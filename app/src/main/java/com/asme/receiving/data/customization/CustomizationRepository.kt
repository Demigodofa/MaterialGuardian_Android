package com.asme.receiving.data.customization

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.asme.receiving.AppContextHolder
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class CustomizationRepository(
    private val context: Context = AppContextHolder.appContext
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): AppCustomization {
        return AppCustomization(
            enableB16Fields = preferences.getBoolean(KEY_ENABLE_B16, true),
            enableSurfaceFinish = preferences.getBoolean(KEY_ENABLE_SURFACE_FINISH, false),
            surfaceFinishUnit = preferences.getString(KEY_SURFACE_FINISH_UNIT, SurfaceFinishUnit.MICROINCH)
                ?.takeIf { it in SurfaceFinishUnit.all }
                ?: SurfaceFinishUnit.MICROINCH,
            companyLogoPath = preferences.getString(KEY_COMPANY_LOGO_PATH, "") ?: "",
            defaultQcInspectorName = preferences.getString(KEY_DEFAULT_QC_INSPECTOR_NAME, "") ?: "",
            defaultQcManagerName = preferences.getString(KEY_DEFAULT_QC_MANAGER_NAME, "") ?: "",
            savedQcInspectorSignaturePath = preferences.getString(KEY_SAVED_QC_INSPECTOR_SIGNATURE_PATH, "") ?: ""
        )
    }

    fun save(customization: AppCustomization) {
        preferences.edit()
            .putBoolean(KEY_ENABLE_B16, customization.enableB16Fields)
            .putBoolean(KEY_ENABLE_SURFACE_FINISH, customization.enableSurfaceFinish)
            .putString(KEY_SURFACE_FINISH_UNIT, customization.surfaceFinishUnit)
            .putString(KEY_COMPANY_LOGO_PATH, customization.companyLogoPath)
            .putString(KEY_DEFAULT_QC_INSPECTOR_NAME, customization.defaultQcInspectorName)
            .putString(KEY_DEFAULT_QC_MANAGER_NAME, customization.defaultQcManagerName)
            .putString(KEY_SAVED_QC_INSPECTOR_SIGNATURE_PATH, customization.savedQcInspectorSignaturePath)
            .apply()
    }

    fun importCompanyLogo(sourceUri: Uri): Result<String> {
        return runCatching {
            val mimeType = context.contentResolver.getType(sourceUri)
            if (mimeType != null && !mimeType.startsWith("image/")) {
                error("Please select an image file for the report logo.")
            }
            val bounds = decodeBounds(context, sourceUri) ?: error("Unable to read the selected image.")

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                error("Unsupported image file.")
            }

            val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_LOGO_DIMENSION)
            val bitmap = decodeBitmap(context, sourceUri, sampleSize)
                ?: error("Unable to decode the selected image.")

            val existingLogoPath = load().companyLogoPath
            val normalized = bitmap.scaleDownTo(MAX_LOGO_DIMENSION)
            val logoDirectory = File(context.filesDir, "customization").also { it.mkdirs() }
            val preserveAlpha = normalized.hasAlpha()
            val outputFile = File(logoDirectory, if (preserveAlpha) "company_logo.png" else "company_logo.jpg")
            FileOutputStream(outputFile).use { output ->
                val format = if (preserveAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val quality = if (preserveAlpha) 100 else 90
                normalized.compress(format, quality, output)
            }
            bitmap.recycle()
            if (normalized !== bitmap) {
                normalized.recycle()
            }
            if (existingLogoPath.isNotBlank() && existingLogoPath != outputFile.absolutePath) {
                runCatching { File(existingLogoPath).delete() }
            }
            preferences.edit()
                .putString(KEY_COMPANY_LOGO_PATH, outputFile.absolutePath)
                .apply()
            outputFile.absolutePath
        }
    }

    fun saveDefaultQcInspectorSignature(bitmap: Bitmap): Result<String> {
        return runCatching {
            val existingSignaturePath = load().savedQcInspectorSignaturePath
            val customizationDirectory = File(context.filesDir, "customization").also { it.mkdirs() }
            val outputFile = File(customizationDirectory, "default_qc_inspector_signature.png")
            FileOutputStream(outputFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (existingSignaturePath.isNotBlank() && existingSignaturePath != outputFile.absolutePath) {
                runCatching { File(existingSignaturePath).delete() }
            }
            preferences.edit()
                .putString(KEY_SAVED_QC_INSPECTOR_SIGNATURE_PATH, outputFile.absolutePath)
                .apply()
            outputFile.absolutePath
        }
    }

    fun clearCompanyLogo() {
        load().companyLogoPath.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { File(path).delete() }
        }
        preferences.edit()
            .putString(KEY_COMPANY_LOGO_PATH, "")
            .apply()
    }

    fun clearDefaultQcInspectorSignature() {
        load().savedQcInspectorSignaturePath.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { File(path).delete() }
        }
        preferences.edit()
            .putString(KEY_SAVED_QC_INSPECTOR_SIGNATURE_PATH, "")
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "material_guardian_customization"
        private const val KEY_ENABLE_B16 = "enable_b16_fields"
        private const val KEY_ENABLE_SURFACE_FINISH = "enable_surface_finish"
        private const val KEY_SURFACE_FINISH_UNIT = "surface_finish_unit"
        private const val KEY_COMPANY_LOGO_PATH = "company_logo_path"
        private const val KEY_DEFAULT_QC_INSPECTOR_NAME = "default_qc_inspector_name"
        private const val KEY_DEFAULT_QC_MANAGER_NAME = "default_qc_manager_name"
        private const val KEY_SAVED_QC_INSPECTOR_SIGNATURE_PATH = "saved_qc_inspector_signature_path"
        private const val MAX_LOGO_DIMENSION = 1200
    }
}

private fun decodeBounds(context: Context, sourceUri: Uri): BitmapFactory.Options? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val decodedFromStream = context.contentResolver.openInputStream(sourceUri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
        bounds.outWidth > 0 && bounds.outHeight > 0
    } ?: false
    if (decodedFromStream) {
        return bounds
    }
    return context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { descriptor ->
        BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor, null, bounds)
        bounds.takeIf { it.outWidth > 0 && it.outHeight > 0 }
    }
}

private fun decodeBitmap(context: Context, sourceUri: Uri, sampleSize: Int): Bitmap? {
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmapFromStream = context.contentResolver.openInputStream(sourceUri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
    if (bitmapFromStream != null) {
        return bitmapFromStream
    }
    return context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { descriptor ->
        BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor, null, options)
    }
}

private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var inSampleSize = 1
    var sampledWidth = width
    var sampledHeight = height
    while (sampledWidth > maxDimension * 2 || sampledHeight > maxDimension * 2) {
        inSampleSize *= 2
        sampledWidth /= 2
        sampledHeight /= 2
    }
    return inSampleSize
}

private fun Bitmap.scaleDownTo(maxDimension: Int): Bitmap {
    val currentMax = max(width, height)
    if (currentMax <= maxDimension) {
        return this
    }
    val scale = maxDimension / currentMax.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}
