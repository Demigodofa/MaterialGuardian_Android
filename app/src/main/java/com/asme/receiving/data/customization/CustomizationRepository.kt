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
            companyLogoPath = preferences.getString(KEY_COMPANY_LOGO_PATH, "") ?: ""
        )
    }

    fun save(customization: AppCustomization) {
        preferences.edit()
            .putBoolean(KEY_ENABLE_B16, customization.enableB16Fields)
            .putBoolean(KEY_ENABLE_SURFACE_FINISH, customization.enableSurfaceFinish)
            .putString(KEY_SURFACE_FINISH_UNIT, customization.surfaceFinishUnit)
            .putString(KEY_COMPANY_LOGO_PATH, customization.companyLogoPath)
            .apply()
    }

    fun importCompanyLogo(sourceUri: Uri): Result<String> {
        return runCatching {
            val mimeType = context.contentResolver.getType(sourceUri)
            if (mimeType != null && mimeType != "image/png" && mimeType != "image/jpeg") {
                error("Please select a PNG or JPEG logo.")
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            } ?: error("Unable to read the selected image.")

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                error("Unsupported image file.")
            }

            val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_LOGO_DIMENSION)
            val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(
                    input,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize }
                )
            } ?: error("Unable to decode the selected image.")

            val normalized = bitmap.scaleDownTo(MAX_LOGO_DIMENSION)
            val logoDirectory = File(context.filesDir, "customization").also { it.mkdirs() }
            val outputFile = File(logoDirectory, "company_logo.jpg")
            FileOutputStream(outputFile).use { output ->
                normalized.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            bitmap.recycle()
            if (normalized !== bitmap) {
                normalized.recycle()
            }
            outputFile.absolutePath
        }
    }

    fun clearCompanyLogo() {
        load().companyLogoPath.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { File(path).delete() }
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "material_guardian_customization"
        private const val KEY_ENABLE_B16 = "enable_b16_fields"
        private const val KEY_ENABLE_SURFACE_FINISH = "enable_surface_finish"
        private const val KEY_SURFACE_FINISH_UNIT = "surface_finish_unit"
        private const val KEY_COMPANY_LOGO_PATH = "company_logo_path"
        private const val MAX_LOGO_DIMENSION = 1200
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
