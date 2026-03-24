package com.asme.receiving.data

import androidx.test.core.app.ApplicationProvider
import com.asme.receiving.AppContextHolder
import com.asme.receiving.data.local.AppDatabaseProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class JobRepositoryTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppContextHolder.init(context)
        AppDatabaseProvider.resetForTests()
        File(context.getDatabasePath("material_guardian.db").path).delete()
    }

    @Test
    fun renameJob_movesMaterials() = runBlocking {
        val jobRepo = JobRepository()
        val materialRepo = MaterialRepository()

        jobRepo.upsert(JobItem(jobNumber = "JOB-1", description = "Test"))
        materialRepo.addMaterial(
            MaterialItem(
                jobNumber = "JOB-1",
                description = "Pipe",
                quantity = "3"
            )
        )

        val success = jobRepo.renameJob("JOB-1", "JOB-2")
        val materials = materialRepo.streamMaterialsForJob("JOB-2").first()

        assertEquals(true, success)
        assertEquals(1, materials.size)
        assertEquals("JOB-2", materials.first().jobNumber)
    }

    @Test
    fun renameJob_movesSanitizedMediaDirectory() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val jobRepo = JobRepository()
        val originalDir = File(context.filesDir, "job_media/job_100_a")
        val renamedDir = File(context.filesDir, "job_media/job_200_b")
        originalDir.mkdirs()
        File(originalDir, "sample.txt").writeText("media")

        jobRepo.upsert(JobItem(jobNumber = "JOB 100/A", description = "Test"))

        val success = jobRepo.renameJob("JOB 100/A", "JOB 200/B")

        assertTrue(success)
        assertFalse(originalDir.exists())
        assertTrue(renamedDir.exists())
        assertTrue(File(renamedDir, "sample.txt").exists())
    }

    @Test
    fun deleteJob_removesSanitizedMediaDirectory() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val jobRepo = JobRepository()
        val mediaDir = File(context.filesDir, "job_media/job_300_c")
        mediaDir.mkdirs()
        File(mediaDir, "sample.txt").writeText("media")

        jobRepo.upsert(JobItem(jobNumber = "JOB 300/C", description = "Delete test"))

        jobRepo.deleteJob("JOB 300/C")

        assertFalse(mediaDir.exists())
    }
}
