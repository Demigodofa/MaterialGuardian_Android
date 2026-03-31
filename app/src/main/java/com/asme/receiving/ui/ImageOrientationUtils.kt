package com.asme.receiving.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface

internal fun decodeOrientedBitmap(path: String): Bitmap? {
    val bitmap = BitmapFactory.decodeFile(path) ?: return null
    val orientation = runCatching {
        ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    val matrix = when (orientation) {
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
    } ?: return bitmap

    return runCatching {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }.getOrElse {
        bitmap
    }
}
