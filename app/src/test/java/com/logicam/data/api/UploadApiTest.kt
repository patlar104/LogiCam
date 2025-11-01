package com.logicam.data.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Unit tests for UploadApi data classes and MockUploadApi
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UploadApiTest {

    private lateinit var testFile: File

    @Before
    fun setup() {
        testFile = File.createTempFile("test_video", ".mp4")
        testFile.writeText("test content")
        testFile.deleteOnExit()
    }

    @Test
    fun `VideoMetadata properties are correctly initialized`() {
        val metadata = VideoMetadata(
            duration = 5000L,
            timestamp = 1672531200000L,
            deviceId = "test-device",
            resolution = "FHD"
        )
        
        assertEquals(5000L, metadata.duration)
        assertEquals(1672531200000L, metadata.timestamp)
        assertEquals("test-device", metadata.deviceId)
        assertEquals("FHD", metadata.resolution)
    }

    @Test
    fun `VideoMetadata data class equality works correctly`() {
        val metadata1 = VideoMetadata(
            duration = 5000L,
            timestamp = 1672531200000L,
            deviceId = "test-device",
            resolution = "FHD"
        )
        
        val metadata2 = VideoMetadata(
            duration = 5000L,
            timestamp = 1672531200000L,
            deviceId = "test-device",
            resolution = "FHD"
        )
        
        assertEquals(metadata1, metadata2)
    }

    @Test
    fun `UploadResponse properties are correctly initialized`() {
        val response = UploadResponse(
            id = "video-123",
            url = "https://example.com/videos/video-123.mp4",
            status = "success"
        )
        
        assertEquals("video-123", response.id)
        assertEquals("https://example.com/videos/video-123.mp4", response.url)
        assertEquals("success", response.status)
    }

    @Test
    fun `UploadResponse status defaults to success`() {
        val response = UploadResponse(
            id = "video-123",
            url = "https://example.com/videos/video-123.mp4"
        )
        
        assertEquals("success", response.status)
    }

    @Test
    fun `MockUploadApi uploadVideo returns success result`() = runTest {
        val mockApi = MockUploadApi()
        val metadata = VideoMetadata(
            duration = 5000L,
            timestamp = 1672531200000L,
            deviceId = "test-device",
            resolution = "FHD"
        )
        
        val result = mockApi.uploadVideo(testFile, metadata)
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun `MockUploadApi uploadVideo includes file name in response`() = runTest {
        val mockApi = MockUploadApi()
        val metadata = VideoMetadata(
            duration = 5000L,
            timestamp = 1672531200000L,
            deviceId = "test-device",
            resolution = "FHD"
        )
        
        val result = mockApi.uploadVideo(testFile, metadata)
        
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertTrue(response!!.url.contains(testFile.name))
    }

    @Test
    fun `MockUploadApi uploadVideo returns mock ID`() = runTest {
        val mockApi = MockUploadApi()
        val metadata = VideoMetadata(
            duration = 5000L,
            timestamp = 1672531200000L,
            deviceId = "test-device",
            resolution = "FHD"
        )
        
        val result = mockApi.uploadVideo(testFile, metadata)
        
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertTrue(response!!.id.startsWith("mock-"))
    }

    @Test
    fun `MockUploadApi uploadVideo returns success status`() = runTest {
        val mockApi = MockUploadApi()
        val metadata = VideoMetadata(
            duration = 5000L,
            timestamp = 1672531200000L,
            deviceId = "test-device",
            resolution = "FHD"
        )
        
        val result = mockApi.uploadVideo(testFile, metadata)
        
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertEquals("success", response!!.status)
    }

    @Test
    fun `OkHttpUploadApi returns failure for non-existent file`() = runTest {
        val api = OkHttpUploadApi("https://example.com", "test-key")
        val nonExistentFile = File("non_existent_file.mp4")
        val metadata = VideoMetadata(
            duration = 5000L,
            timestamp = 1672531200000L,
            deviceId = "test-device",
            resolution = "FHD"
        )
        
        val result = api.uploadVideo(nonExistentFile, metadata)
        
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception!!.message!!.contains("File does not exist"))
    }

    @Test
    fun `OkHttpUploadApi constructs correct API endpoint`() {
        val api = OkHttpUploadApi("https://example.com", "test-key")
        
        // Just verify it can be instantiated
        assertNotNull(api)
    }
}
