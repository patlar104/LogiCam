package com.logicam.util

import android.content.Context
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Unit tests for SecureLogger
 * Tests logging functionality and file operations
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SecureLoggerTest {

    private lateinit var context: Context
    private lateinit var logFile: File

    @Before
    fun setup() {
        // Mock Android Log class
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        
        // Create a temporary directory for testing
        val tempDir = File.createTempFile("test", "dir").apply {
            delete()
            mkdir()
            deleteOnExit()
        }
        
        logFile = File(tempDir, "logicam_log.txt")
        logFile.deleteOnExit()
        
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempDir
    }

    @Test
    fun `d logs debug message`() {
        SecureLogger.d("TestTag", "Debug message")
        
        verify { Log.d("LogiCam", "[TestTag] Debug message") }
    }

    @Test
    fun `i logs info message`() {
        SecureLogger.i("TestTag", "Info message")
        
        verify { Log.i("LogiCam", "[TestTag] Info message") }
    }

    @Test
    fun `w logs warning message without throwable`() {
        SecureLogger.w("TestTag", "Warning message")
        
        verify { Log.w("LogiCam", "[TestTag] Warning message", null) }
    }

    @Test
    fun `w logs warning message with throwable`() {
        val exception = RuntimeException("Test exception")
        
        SecureLogger.w("TestTag", "Warning message", exception)
        
        verify { Log.w("LogiCam", "[TestTag] Warning message", exception) }
    }

    @Test
    fun `e logs error message without throwable`() {
        SecureLogger.e("TestTag", "Error message")
        
        verify { Log.e("LogiCam", "[TestTag] Error message", null) }
    }

    @Test
    fun `e logs error message with throwable`() {
        val exception = RuntimeException("Test exception")
        
        SecureLogger.e("TestTag", "Error message", exception)
        
        verify { Log.e("LogiCam", "[TestTag] Error message", exception) }
    }

    @Test
    fun `logToFile creates log file and writes entry`() = runTest {
        SecureLogger.logToFile(context, "TestTag", "Test message")
        
        assertTrue(logFile.exists())
        val content = logFile.readText()
        assertTrue(content.contains("[TestTag]"))
        assertTrue(content.contains("Test message"))
    }

    @Test
    fun `logToFile appends multiple entries`() = runTest {
        SecureLogger.logToFile(context, "Tag1", "Message 1")
        SecureLogger.logToFile(context, "Tag2", "Message 2")
        
        val content = logFile.readText()
        assertTrue(content.contains("Message 1"))
        assertTrue(content.contains("Message 2"))
        
        val lines = content.lines().filter { it.isNotEmpty() }
        assertEquals(2, lines.size)
    }

    @Test
    fun `logToFile includes timestamp in entry`() = runTest {
        SecureLogger.logToFile(context, "TestTag", "Test message")
        
        val content = logFile.readText()
        // Check for timestamp pattern like [2023-01-01 12:00:00.123]
        assertTrue(content.matches(Regex(".*\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}].*", RegexOption.DOT_MATCHES_ALL)))
    }

    @Test
    fun `getRecentLogs returns empty string when file does not exist`() = runTest {
        val result = SecureLogger.getRecentLogs(context, 10)
        
        assertEquals("", result)
    }

    @Test
    fun `getRecentLogs returns all logs when less than requested lines`() = runTest {
        SecureLogger.logToFile(context, "Tag1", "Message 1")
        SecureLogger.logToFile(context, "Tag2", "Message 2")
        
        val result = SecureLogger.getRecentLogs(context, 100)
        
        assertTrue(result.contains("Message 1"))
        assertTrue(result.contains("Message 2"))
    }

    @Test
    fun `getRecentLogs returns only last N lines when more logs exist`() = runTest {
        // Write 5 log entries
        for (i in 1..5) {
            SecureLogger.logToFile(context, "Tag$i", "Message $i")
        }
        
        val result = SecureLogger.getRecentLogs(context, 2)
        
        // Should contain only last 2 messages
        assertFalse(result.contains("Message 1"))
        assertFalse(result.contains("Message 2"))
        assertFalse(result.contains("Message 3"))
        assertTrue(result.contains("Message 4"))
        assertTrue(result.contains("Message 5"))
    }

    @Test
    fun `getRecentLogs uses default of 100 lines`() = runTest {
        SecureLogger.logToFile(context, "TestTag", "Test message")
        
        // This should not throw and should return content
        val result = SecureLogger.getRecentLogs(context)
        
        assertTrue(result.contains("Test message"))
    }
}
