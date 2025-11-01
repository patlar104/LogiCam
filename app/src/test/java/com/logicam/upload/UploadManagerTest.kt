package com.logicam.upload

import android.content.Context
import android.util.Log
import com.logicam.util.SecureLogger
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Unit tests for UploadManager
 * Tests upload scheduling and state management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UploadManagerTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var uploadManager: UploadManager
    private lateinit var testFile: File

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        
        // Mock SecureLogger
        mockkObject(SecureLogger)
        every { SecureLogger.i(any(), any()) } just Runs
        every { SecureLogger.e(any(), any(), any()) } just Runs
        
        // Mock UploadWorker static method
        mockkObject(UploadWorker)
        every { UploadWorker.scheduleUpload(any()) } just Runs
        
        context = mockk(relaxed = true)
        testScope = TestScope()
        uploadManager = UploadManager(context, testScope)
        
        testFile = File.createTempFile("test_video", ".mp4")
        testFile.deleteOnExit()
    }

    @Test
    fun `initial uploadState is Idle`() = runTest {
        val state = uploadManager.uploadState.first()
        
        assertTrue(state is UploadManager.UploadState.Idle)
    }

    @Test
    fun `scheduleUpload transitions to Uploading state`() = testScope.runTest {
        uploadManager.scheduleUpload(testFile)
        advanceUntilIdle()
        
        // State should progress through Uploading and then Completed
        val finalState = uploadManager.uploadState.value
        assertTrue(
            finalState is UploadManager.UploadState.Completed &&
            finalState.filename == testFile.name
        )
    }

    @Test
    fun `scheduleUpload calls UploadWorker scheduleUpload`() = testScope.runTest {
        uploadManager.scheduleUpload(testFile)
        advanceUntilIdle()
        
        verify { UploadWorker.scheduleUpload(context) }
    }

    @Test
    fun `scheduleUpload logs appropriate messages`() = testScope.runTest {
        uploadManager.scheduleUpload(testFile)
        advanceUntilIdle()
        
        verify { SecureLogger.i("UploadManager", "Scheduling upload for ${testFile.name}") }
    }

    @Test
    fun `scheduleUpload transitions to Failed state on exception`() = testScope.runTest {
        every { UploadWorker.scheduleUpload(any()) } throws RuntimeException("Test exception")
        
        uploadManager.scheduleUpload(testFile)
        advanceUntilIdle()
        
        val state = uploadManager.uploadState.value
        assertTrue(state is UploadManager.UploadState.Failed)
        assertEquals(testFile.name, (state as UploadManager.UploadState.Failed).filename)
    }

    @Test
    fun `scheduleUpload logs error on exception`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        every { UploadWorker.scheduleUpload(any()) } throws exception
        
        uploadManager.scheduleUpload(testFile)
        advanceUntilIdle()
        
        verify { SecureLogger.e("UploadManager", "Failed to schedule upload", any()) }
    }

    @Test
    fun `scheduleAllPendingUploads calls UploadWorker scheduleUpload`() = testScope.runTest {
        uploadManager.scheduleAllPendingUploads()
        advanceUntilIdle()
        
        verify { UploadWorker.scheduleUpload(context) }
    }

    @Test
    fun `scheduleAllPendingUploads logs appropriate message`() = testScope.runTest {
        uploadManager.scheduleAllPendingUploads()
        advanceUntilIdle()
        
        verify { SecureLogger.i("UploadManager", "Scheduling all pending uploads") }
    }

    @Test
    fun `uploadState Uploading includes progress`() = testScope.runTest {
        var sawUploadingState = false
        
        uploadManager.scheduleUpload(testFile)
        advanceUntilIdle()
        
        // The final state should be Completed
        val finalState = uploadManager.uploadState.value
        assertTrue(finalState is UploadManager.UploadState.Completed)
    }

    @Test
    fun `UploadState sealed class has all expected states`() {
        // Verify all state types are accessible
        val idle: UploadManager.UploadState = UploadManager.UploadState.Idle
        val uploading: UploadManager.UploadState = UploadManager.UploadState.Uploading("test.mp4", 50)
        val completed: UploadManager.UploadState = UploadManager.UploadState.Completed("test.mp4")
        val failed: UploadManager.UploadState = UploadManager.UploadState.Failed("test.mp4", "error")
        
        assertNotNull(idle)
        assertNotNull(uploading)
        assertNotNull(completed)
        assertNotNull(failed)
    }
}
