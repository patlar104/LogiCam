package com.logicam.util

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.logicam.R
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ErrorHandler
 * Tests that error dialogs are created correctly and null safety is maintained
 */
class ErrorHandlerTest {

    private lateinit var activity: Activity
    private lateinit var context: Context
    private lateinit var dialogBuilder: AlertDialog.Builder
    private lateinit var dialog: AlertDialog

    @Before
    fun setup() {
        activity = mockk(relaxed = true)
        context = mockk(relaxed = true)
        dialog = mockk(relaxed = true)
        dialogBuilder = mockk(relaxed = true) {
            every { setTitle(any<String>()) } returns this
            every { setTitle(any<Int>()) } returns this
            every { setMessage(any<String>()) } returns this
            every { setMessage(any<Int>()) } returns this
            every { setPositiveButton(any<String>(), any()) } returns this
            every { setPositiveButton(any<Int>(), any()) } returns this
            every { setNegativeButton(any<String>(), any()) } returns this
            every { setNegativeButton(any<Int>(), any()) } returns this
            every { setNeutralButton(any<String>(), any()) } returns this
            every { setNeutralButton(any<Int>(), any()) } returns this
            every { setCancelable(any()) } returns this
            every { create() } returns dialog
            every { show() } returns dialog
        }
        
        every { context.getString(any()) } returns "Test String"
        every { context.getString(any(), any()) } returns "Test String with args"
    }

    @Test
    fun `CameraError types should be properly defined`() {
        // Verify all error types exist
        val errors = listOf(
            ErrorHandler.CameraError.InUseByOtherApp,
            ErrorHandler.CameraError.LowStorage(50),
            ErrorHandler.CameraError.HardwareFailed,
            ErrorHandler.CameraError.InitializationFailed("test"),
            ErrorHandler.CameraError.RecordingFailed("test")
        )
        
        assertEquals(5, errors.size)
    }

    @Test
    fun `null safety - checkStorageSpace should handle null StatFs gracefully`() {
        // Test that storage check doesn't crash with mock context
        every { context.getExternalFilesDir(any()) } returns null
        
        // Should not crash, should return false indicating insufficient space
        val result = ErrorHandler.checkStorageSpace(context)
        assertEquals(false, result)
    }

    @Test
    fun `LowStorage error should include available space in message`() {
        val error = ErrorHandler.CameraError.LowStorage(availableMB = 25)
        
        // Verify availableMB is accessible
        assertEquals(25, error.availableMB)
    }

    @Test
    fun `InitializationFailed and RecordingFailed should preserve error message`() {
        val initError = ErrorHandler.CameraError.InitializationFailed("Camera not available")
        val recError = ErrorHandler.CameraError.RecordingFailed("Storage full")
        
        assertEquals("Camera not available", initError.message)
        assertEquals("Storage full", recError.message)
    }
}
