package com.asme.receiving.ui

import androidx.test.core.app.ApplicationProvider
import com.asme.receiving.data.MaterialItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MaterialFormDraftStoreTest {

    private lateinit var store: MaterialFormDraftStore

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("material_form_drafts", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        store = MaterialFormDraftStore(context)
    }

    @Test
    fun saveAndLoadRoundTripsDraft() {
        val key = store.draftKey("JOB-9", null)
        val item = MaterialItem(
            jobNumber = "JOB-9",
            description = "Plate",
            qcInitials = "KP",
            qcManager = "Kevin",
            photoPaths = "/tmp/photo1.jpg|/tmp/photo2.jpg",
            scanPaths = "/tmp/scan1.pdf\t/tmp/scan1.jpg"
        )

        store.save(key, item)
        val restored = store.load(key)

        requireNotNull(restored)
        assertEquals("JOB-9", restored.jobNumber)
        assertEquals("Plate", restored.description)
        assertEquals("KP", restored.qcInitials)
        assertEquals("Kevin", restored.qcManager)
        assertEquals("/tmp/photo1.jpg|/tmp/photo2.jpg", restored.photoPaths)
        assertEquals("/tmp/scan1.pdf\t/tmp/scan1.jpg", restored.scanPaths)
    }

    @Test
    fun clearRemovesSavedDraft() {
        val key = store.draftKey("JOB-10", "material-1")
        store.save(key, MaterialItem(jobNumber = "JOB-10", description = "Pipe"))

        store.clear(key)

        assertNull(store.load(key))
    }

    @Test
    fun saveImmediatelyPersistsLatestDraftSynchronously() {
        val key = store.draftKey("JOB-11", null)
        val item = MaterialItem(jobNumber = "JOB-11", description = "Valve")

        store.saveImmediately(key, item)

        val restored = store.load(key)
        requireNotNull(restored)
        assertEquals("Valve", restored.description)
    }
}
