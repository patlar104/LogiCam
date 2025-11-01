package com.logicam.ui.gallery

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.io.File
import java.util.Date

/**
 * Unit tests for VideoItem data class
 * Tests property calculations and display formatting
 */
class VideoItemTest {

    private lateinit var testFile: File
    private val testDate = Date(1672531200000L) // Jan 1, 2023 00:00:00 UTC
    
    @Before
    fun setup() {
        // Create a temporary file for testing
        testFile = File.createTempFile("test_video", ".mp4")
        testFile.deleteOnExit()
    }

    @Test
    fun `VideoItem properties are correctly initialized`() {
        val videoItem = VideoItem(
            file = testFile,
            name = "test_video.mp4",
            dateModified = testDate,
            durationMs = 5000,
            sizeBytes = 1024 * 1024 // 1 MB
        )
        
        assertEquals(testFile, videoItem.file)
        assertEquals("test_video.mp4", videoItem.name)
        assertEquals(testDate, videoItem.dateModified)
        assertEquals(5000L, videoItem.durationMs)
        assertEquals(1024 * 1024L, videoItem.sizeBytes)
    }

    @Test
    fun `displayName removes mp4 extension`() {
        val videoItem = VideoItem(
            file = testFile,
            name = "my_video.mp4",
            dateModified = testDate
        )
        
        assertEquals("my_video", videoItem.displayName)
    }

    @Test
    fun `displayName handles name without extension`() {
        val videoItem = VideoItem(
            file = testFile,
            name = "video_without_extension",
            dateModified = testDate
        )
        
        assertEquals("video_without_extension", videoItem.displayName)
    }

    @Test
    fun `sizeMB formats bytes correctly to megabytes`() {
        val videoItem = VideoItem(
            file = testFile,
            name = "test.mp4",
            dateModified = testDate,
            sizeBytes = 5242880 // 5 MB
        )
        
        assertEquals("5.0 MB", videoItem.sizeMB)
    }

    @Test
    fun `sizeMB formats fractional megabytes with one decimal`() {
        val videoItem = VideoItem(
            file = testFile,
            name = "test.mp4",
            dateModified = testDate,
            sizeBytes = 1572864 // 1.5 MB
        )
        
        assertEquals("1.5 MB", videoItem.sizeMB)
    }

    @Test
    fun `sizeMB handles small files under 1MB`() {
        val videoItem = VideoItem(
            file = testFile,
            name = "test.mp4",
            dateModified = testDate,
            sizeBytes = 512000 // ~0.5 MB
        )
        
        assertTrue(videoItem.sizeMB.contains("0.") || videoItem.sizeMB.contains("1."))
    }

    @Test
    fun `sizeBytes defaults to file length when not specified`() {
        // Write some data to the file
        testFile.writeText("test data content")
        
        val videoItem = VideoItem(
            file = testFile,
            name = "test.mp4",
            dateModified = testDate
        )
        
        assertEquals(testFile.length(), videoItem.sizeBytes)
    }

    @Test
    fun `durationMs defaults to zero when not specified`() {
        val videoItem = VideoItem(
            file = testFile,
            name = "test.mp4",
            dateModified = testDate
        )
        
        assertEquals(0L, videoItem.durationMs)
    }

    @Test
    fun `data class equality works correctly`() {
        val videoItem1 = VideoItem(
            file = testFile,
            name = "test.mp4",
            dateModified = testDate,
            durationMs = 1000,
            sizeBytes = 2048
        )
        
        val videoItem2 = VideoItem(
            file = testFile,
            name = "test.mp4",
            dateModified = testDate,
            durationMs = 1000,
            sizeBytes = 2048
        )
        
        assertEquals(videoItem1, videoItem2)
    }

    @Test
    fun `data class copy works correctly`() {
        val original = VideoItem(
            file = testFile,
            name = "original.mp4",
            dateModified = testDate
        )
        
        val copy = original.copy(name = "copy.mp4")
        
        assertEquals("copy.mp4", copy.name)
        assertEquals(original.file, copy.file)
        assertEquals(original.dateModified, copy.dateModified)
    }
}
