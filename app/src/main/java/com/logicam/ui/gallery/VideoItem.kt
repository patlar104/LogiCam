package com.logicam.ui.gallery

import java.io.File
import java.util.Date

/**
 * Represents a video file with metadata
 */
data class VideoItem(
    val file: File,
    val name: String,
    val dateModified: Date,
    val durationMs: Long = 0,
    val sizeBytes: Long = file.length()
) {
    val displayName: String
        get() = name.removeSuffix(".mp4")
    
    val sizeMB: String
        get() = "%.1f MB".format(sizeBytes / (1024f * 1024f))
}
