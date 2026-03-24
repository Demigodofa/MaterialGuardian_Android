package com.asme.receiving.ui

import android.annotation.SuppressLint
import android.content.Context
import com.asme.receiving.data.MaterialItem
import org.json.JSONObject

class MaterialFormDraftStore(
    context: Context
) {
    private val preferences = context.getSharedPreferences("material_form_drafts", Context.MODE_PRIVATE)

    fun draftKey(jobNumber: String, materialId: String?): String {
        val target = materialId?.ifBlank { null } ?: "__new__"
        return "material_form::$jobNumber::$target"
    }

    fun load(key: String): MaterialItem? {
        val raw = preferences.getString(key, null) ?: return null
        return runCatching { materialItemFromJson(JSONObject(raw)) }.getOrNull()
    }

    fun save(key: String, item: MaterialItem) {
        preferences.edit()
            .putString(key, materialItemToJson(item).toString())
            .apply()
    }

    fun clear(key: String) {
        preferences.edit().remove(key).apply()
    }

    @SuppressLint("ApplySharedPref")
    fun saveImmediately(key: String, item: MaterialItem) {
        preferences.edit()
            .putString(key, materialItemToJson(item).toString())
            .commit()
    }

    @SuppressLint("ApplySharedPref")
    fun clearImmediately(key: String) {
        preferences.edit().remove(key).commit()
    }
}

private fun materialItemToJson(item: MaterialItem): JSONObject {
    return JSONObject().apply {
        put("id", item.id)
        put("jobNumber", item.jobNumber)
        put("description", item.description)
        put("vendor", item.vendor)
        put("quantity", item.quantity)
        put("poNumber", item.poNumber)
        put("productType", item.productType)
        put("specificationPrefix", item.specificationPrefix)
        put("gradeType", item.gradeType)
        put("fittingStandard", item.fittingStandard)
        put("fittingSuffix", item.fittingSuffix)
        put("dimensionUnit", item.dimensionUnit)
        put("thickness1", item.thickness1)
        put("thickness2", item.thickness2)
        put("thickness3", item.thickness3)
        put("thickness4", item.thickness4)
        put("width", item.width)
        put("length", item.length)
        put("diameter", item.diameter)
        put("diameterType", item.diameterType)
        put("visualInspectionAcceptable", item.visualInspectionAcceptable)
        put("b16DimensionsAcceptable", item.b16DimensionsAcceptable)
        put("specificationNumbers", item.specificationNumbers)
        put("markings", item.markings)
        put("markingAcceptable", item.markingAcceptable)
        put("markingAcceptableNa", item.markingAcceptableNa)
        put("mtrAcceptable", item.mtrAcceptable)
        put("mtrAcceptableNa", item.mtrAcceptableNa)
        put("acceptanceStatus", item.acceptanceStatus)
        put("comments", item.comments)
        put("qcInitials", item.qcInitials)
        put("qcDate", item.qcDate)
        put("qcSignaturePath", item.qcSignaturePath)
        put("materialApproval", item.materialApproval)
        put("qcManager", item.qcManager)
        put("qcManagerInitials", item.qcManagerInitials)
        put("qcManagerDate", item.qcManagerDate)
        put("qcManagerSignaturePath", item.qcManagerSignaturePath)
        put("offloadStatus", item.offloadStatus)
        put("pdfStatus", item.pdfStatus)
        put("pdfStoragePath", item.pdfStoragePath)
        put("photoPaths", item.photoPaths)
        put("scanPaths", item.scanPaths)
        put("photoCount", item.photoCount)
        put("receivedAt", item.receivedAt)
    }
}

private fun materialItemFromJson(json: JSONObject): MaterialItem {
    return MaterialItem(
        id = json.optString("id"),
        jobNumber = json.optString("jobNumber"),
        description = json.optString("description"),
        vendor = json.optString("vendor"),
        quantity = json.optString("quantity"),
        poNumber = json.optString("poNumber"),
        productType = json.optString("productType"),
        specificationPrefix = json.optString("specificationPrefix"),
        gradeType = json.optString("gradeType"),
        fittingStandard = json.optString("fittingStandard"),
        fittingSuffix = json.optString("fittingSuffix"),
        dimensionUnit = json.optString("dimensionUnit", "imperial"),
        thickness1 = json.optString("thickness1"),
        thickness2 = json.optString("thickness2"),
        thickness3 = json.optString("thickness3"),
        thickness4 = json.optString("thickness4"),
        width = json.optString("width"),
        length = json.optString("length"),
        diameter = json.optString("diameter"),
        diameterType = json.optString("diameterType"),
        visualInspectionAcceptable = json.optBoolean("visualInspectionAcceptable", true),
        b16DimensionsAcceptable = json.optString("b16DimensionsAcceptable"),
        specificationNumbers = json.optString("specificationNumbers"),
        markings = json.optString("markings"),
        markingAcceptable = json.optBoolean("markingAcceptable", true),
        markingAcceptableNa = json.optBoolean("markingAcceptableNa", false),
        mtrAcceptable = json.optBoolean("mtrAcceptable", true),
        mtrAcceptableNa = json.optBoolean("mtrAcceptableNa", false),
        acceptanceStatus = json.optString("acceptanceStatus", "accept"),
        comments = json.optString("comments"),
        qcInitials = json.optString("qcInitials"),
        qcDate = json.optLong("qcDate", System.currentTimeMillis()),
        qcSignaturePath = json.optString("qcSignaturePath"),
        materialApproval = json.optString("materialApproval", "approved"),
        qcManager = json.optString("qcManager"),
        qcManagerInitials = json.optString("qcManagerInitials"),
        qcManagerDate = json.optLong("qcManagerDate", System.currentTimeMillis()),
        qcManagerSignaturePath = json.optString("qcManagerSignaturePath"),
        offloadStatus = json.optString("offloadStatus", "pending"),
        pdfStatus = json.optString("pdfStatus", "pending"),
        pdfStoragePath = json.optString("pdfStoragePath"),
        photoPaths = json.optString("photoPaths"),
        scanPaths = json.optString("scanPaths"),
        photoCount = json.optInt("photoCount", 0),
        receivedAt = json.optLong("receivedAt", System.currentTimeMillis())
    )
}
