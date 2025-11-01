package com.logicam

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.logicam.data.api.UploadApi
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for AppContainer
 * Tests dependency injection and provider methods
 */
class AppContainerTest {

    private lateinit var context: Context
    private lateinit var container: AppContainer

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        container = AppContainer(context)
    }

    @Test
    fun `provideCameraManager returns non-null instance`() {
        val lifecycle = mockk<LifecycleOwner>(relaxed = true)
        
        val manager = container.provideCameraManager(lifecycle)
        
        assertNotNull(manager)
    }

    @Test
    fun `provideRecordingManager returns non-null instance`() {
        val lifecycle = mockk<LifecycleOwner>(relaxed = true)
        val cameraManager = container.provideCameraManager(lifecycle)
        
        val recordingManager = container.provideRecordingManager(cameraManager)
        
        assertNotNull(recordingManager)
    }

    @Test
    fun `providePhotoCaptureManager returns non-null instance`() {
        val lifecycle = mockk<LifecycleOwner>(relaxed = true)
        val cameraManager = container.provideCameraManager(lifecycle)
        
        val photoManager = container.providePhotoCaptureManager(cameraManager)
        
        assertNotNull(photoManager)
    }

    @Test
    fun `provideUploadManager returns non-null instance`() {
        val scope = TestScope()
        
        val uploadManager = container.provideUploadManager(scope)
        
        assertNotNull(uploadManager)
    }

    @Test
    fun `provideUploadApi returns MockUploadApi by default`() {
        val uploadApi = container.provideUploadApi()
        
        assertNotNull(uploadApi)
        // Default implementation should be MockUploadApi
        assertTrue(uploadApi.javaClass.simpleName.contains("Mock"))
    }

    @Test
    fun `AppContainer can be extended for testing`() {
        // Test that AppContainer is open and can be mocked/extended
        val customContainer = object : AppContainer(context) {
            override fun provideUploadApi(): UploadApi = mockk(relaxed = true)
        }
        
        val api = customContainer.provideUploadApi()
        assertNotNull(api)
    }

    @Test
    fun `multiple calls to providers create new instances`() {
        val lifecycle = mockk<LifecycleOwner>(relaxed = true)
        
        val manager1 = container.provideCameraManager(lifecycle)
        val manager2 = container.provideCameraManager(lifecycle)
        
        // Should create new instances each time (not singletons)
        assertNotSame(manager1, manager2)
    }
}
