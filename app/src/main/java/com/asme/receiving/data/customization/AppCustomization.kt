package com.asme.receiving.data.customization

data class AppCustomization(
    val enableB16Fields: Boolean = true,
    val enableSurfaceFinish: Boolean = false,
    val surfaceFinishUnit: String = SurfaceFinishUnit.MICROINCH,
    val companyLogoPath: String = "",
    val defaultQcInspectorName: String = "",
    val defaultQcManagerName: String = "",
    val savedQcInspectorSignaturePath: String = ""
)

object SurfaceFinishUnit {
    const val MICROINCH = "microinch"
    const val MICRONS = "microns"

    val all = listOf(MICROINCH, MICRONS)

    fun label(value: String): String {
        return when (value) {
            MICRONS -> "Microns"
            else -> "Microinch"
        }
    }
}

object SurfaceFinishCode {
    val all = listOf("", "SF1", "SF2", "SF3", "SF4")

    fun label(value: String): String {
        return if (value.isBlank()) "Clear" else value
    }
}
