package com.logicam.util

import android.content.Context
import android.content.SharedPreferences
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for AppConfig
 * Tests configuration management and persistence
 * 
 * Note: Tests for getVideoQuality and setVideoQuality are limited due to CameraX Quality 
 * enum requiring Android framework classes that cannot be easily mocked in unit tests.
 * These methods are integration tested in androidTest.
 */
class AppConfigTest {

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPrefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        
        every { context.getSharedPreferences("logicam_prefs", Context.MODE_PRIVATE) } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.apply() } just Runs
    }

    // Note: getVideoQuality and setVideoQuality tests are skipped because
    // androidx.camera.video.Quality enum requires Android framework classes
    // that cannot be mocked in unit tests. These methods are tested in androidTest.

    @Test
    fun `isUploadOnlyWifi returns true by default`() {
        every { sharedPrefs.getBoolean("upload_only_wifi", true) } returns true
        
        val result = AppConfig.isUploadOnlyWifi(context)
        
        assertTrue(result)
    }

    @Test
    fun `isUploadOnlyWifi returns false when disabled`() {
        every { sharedPrefs.getBoolean("upload_only_wifi", true) } returns false
        
        val result = AppConfig.isUploadOnlyWifi(context)
        
        assertFalse(result)
    }

    @Test
    fun `setUploadOnlyWifi saves preference correctly`() {
        AppConfig.setUploadOnlyWifi(context, false)
        
        verify { editor.putBoolean("upload_only_wifi", false) }
        verify { editor.apply() }
    }

    @Test
    fun `isAutoUploadEnabled returns true by default`() {
        every { sharedPrefs.getBoolean("auto_upload", true) } returns true
        
        val result = AppConfig.isAutoUploadEnabled(context)
        
        assertTrue(result)
    }

    @Test
    fun `isAutoUploadEnabled returns false when disabled`() {
        every { sharedPrefs.getBoolean("auto_upload", true) } returns false
        
        val result = AppConfig.isAutoUploadEnabled(context)
        
        assertFalse(result)
    }

    @Test
    fun `setAutoUploadEnabled saves preference correctly`() {
        AppConfig.setAutoUploadEnabled(context, true)
        
        verify { editor.putBoolean("auto_upload", true) }
        verify { editor.apply() }
    }

    @Test
    fun `getMaxReconnectAttempts returns 5 by default`() {
        every { sharedPrefs.getInt("max_reconnect_attempts", 5) } returns 5
        
        val result = AppConfig.getMaxReconnectAttempts(context)
        
        assertEquals(5, result)
    }

    @Test
    fun `getMaxReconnectAttempts returns custom value`() {
        every { sharedPrefs.getInt("max_reconnect_attempts", 5) } returns 10
        
        val result = AppConfig.getMaxReconnectAttempts(context)
        
        assertEquals(10, result)
    }

    @Test
    fun `setMaxReconnectAttempts saves valid value`() {
        AppConfig.setMaxReconnectAttempts(context, 7)
        
        verify { editor.putInt("max_reconnect_attempts", 7) }
        verify { editor.apply() }
    }

    @Test
    fun `setMaxReconnectAttempts enforces minimum of 1`() {
        AppConfig.setMaxReconnectAttempts(context, 0)
        
        verify { editor.putInt("max_reconnect_attempts", 1) }
        verify { editor.apply() }
    }

    @Test
    fun `setMaxReconnectAttempts enforces minimum for negative values`() {
        AppConfig.setMaxReconnectAttempts(context, -5)
        
        verify { editor.putInt("max_reconnect_attempts", 1) }
        verify { editor.apply() }
    }
}
