package com.asme.receiving.data.customization

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CustomizationRepositoryTest {

    private lateinit var repository: CustomizationRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("material_guardian_customization", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        repository = CustomizationRepository(context)
    }

    @Test
    fun saveAndLoadRoundTripsCustomizationSettings() {
        repository.save(
            AppCustomization(
                enableB16Fields = false,
                enableSurfaceFinish = true,
                surfaceFinishUnit = SurfaceFinishUnit.MICRONS,
                companyLogoPath = "/tmp/company_logo.jpg"
            )
        )

        val restored = repository.load()

        assertTrue(!restored.enableB16Fields)
        assertTrue(restored.enableSurfaceFinish)
        assertEquals(SurfaceFinishUnit.MICRONS, restored.surfaceFinishUnit)
        assertEquals("/tmp/company_logo.jpg", restored.companyLogoPath)
    }
}
