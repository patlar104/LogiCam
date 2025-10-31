package com.logicam.ui

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LifecycleOwner
import com.logicam.AppContainer
import com.logicam.capture.CameraXCaptureManager
import com.logicam.capture.PhotoCaptureManager
import com.logicam.capture.RecordingManager
import com.logicam.upload.UploadManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Unit tests for MainViewModel
 * Tests critical recording state transitions and null safety
 * 
 * Note: These tests verify the ViewModel's state machine and null safety.
 * Integration tests with real CameraX are in androidTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var application: Application
    private lateinit var container: AppContainer
    private lateinit var cameraManager: CameraXCaptureManager
    private lateinit var recordingManager: RecordingManager
    private lateinit var photoManager: PhotoCaptureManager
    private lateinit var uploadManager: UploadManager
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        application = mockk(relaxed = true)
        cameraManager = mockk(relaxed = true)
        recordingManager = mockk(relaxed = true)
        photoManager = mockk(relaxed = true)
        uploadManager = mockk(relaxed = true)
        
        container = mockk {
            every { provideCameraManager(any()) } returns cameraManager
            every { provideRecordingManager(any()) } returns recordingManager
            every { providePhotoCaptureManager(any()) } returns photoManager
            every { provideUploadManager(any()) } returns uploadManager
        }
        
        viewModel = MainViewModel(application, container)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Idle`() = runTest {
        val state = viewModel.uiState.first()
        assertTrue(state is MainViewModel.CameraUiState.Idle)
    }

    @Test
    fun `initializeCamera with success should transition to Ready`() = runTest {
        val lifecycle = mockk<LifecycleOwner>()
        coEvery { cameraManager.initialize() } returns Result.success(Unit)
        
        viewModel.initializeCamera(lifecycle)
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state is MainViewModel.CameraUiState.Ready)
    }

    @Test
    fun `initializeCamera with failure should transition to Error`() = runTest {
        val lifecycle = mockk<LifecycleOwner>()
        val exception = RuntimeException("Camera unavailable")
        coEvery { cameraManager.initialize() } returns Result.failure(exception)
        
        viewModel.initializeCamera(lifecycle)
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state is MainViewModel.CameraUiState.Error)
        assertEquals("Camera unavailable", (state as MainViewModel.CameraUiState.Error).message)
    }

    @Test
    fun `null safety - ViewModel should handle null camera manager from container`() = runTest {
        // Create a custom mock that can return null despite AppContainer's non-null signature
        val testContainer = object : AppContainer(application) {
            override fun provideCameraManager(lifecycle: LifecycleOwner): CameraXCaptureManager {
                // In real scenarios, this could throw or return a broken instance
                throw NullPointerException("Camera manager unavailable")
            }
        }
        
        val vmWithNulls = MainViewModel(application, testContainer)
        val lifecycle = mockk<LifecycleOwner>()
        
        // Should not crash, should transition to Error state
        vmWithNulls.initializeCamera(lifecycle)
        advanceUntilIdle()
        
        val state = vmWithNulls.uiState.value
        // State should handle exception gracefully with error message
        assertTrue(state is MainViewModel.CameraUiState.Error)
    }

    @Test
    fun `getCameraManager returns the initialized manager`() = runTest {
        val lifecycle = mockk<LifecycleOwner>()
        coEvery { cameraManager.initialize() } returns Result.success(Unit)
        
        viewModel.initializeCamera(lifecycle)
        advanceUntilIdle()
        
        val retrievedManager = viewModel.getCameraManager()
        assertNotNull(retrievedManager)
    }

    @Test
    fun `onCleared should cleanup resources without crash`() {
        // Trigger onCleared via reflection
        val onClearedMethod = MainViewModel::class.java.getDeclaredMethod("onCleared")
        onClearedMethod.isAccessible = true
        onClearedMethod.invoke(viewModel)
        
        // Verify we get here without crash - cleanup worked
        assertTrue(true)
    }

    @Test
    fun `test ViewModel state machine does not expose nulls to UI`() = runTest {
        // This test verifies that the ViewModel never exposes null states
        val lifecycle = mockk<LifecycleOwner>()
        coEvery { cameraManager.initialize() } returns Result.failure(Exception("test"))
        
        viewModel.initializeCamera(lifecycle)
        advanceUntilIdle()
        
        // Even on failure, state should be valid (not null)
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertTrue(state is MainViewModel.CameraUiState.Error || 
                   state is MainViewModel.CameraUiState.Idle)
    }
}
