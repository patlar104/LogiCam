package com.logicam.ui.gallery

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.logicam.R
import com.logicam.util.SecureLogger
import com.logicam.util.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

/**
 * Activity to display recorded videos in a gallery view
 */
class VideoGalleryActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var adapter: VideoThumbnailAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_gallery)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        
        adapter = VideoThumbnailAdapter(
            onVideoClick = { video -> playVideo(video) },
            onVideoLongClick = { video -> showDeleteDialog(video) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        loadVideos()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onResume() {
        super.onResume()
        // Reload videos when returning to the activity
        loadVideos()
    }
    
    private fun loadVideos() {
        lifecycleScope.launch {
            val videos = withContext(Dispatchers.IO) {
                val videoDir = StorageUtil.getVideoOutputDirectory(this@VideoGalleryActivity)
                val files = videoDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".mp4")
                } ?: emptyArray()
                
                files.map { file ->
                    VideoItem(
                        file = file,
                        name = file.name,
                        dateModified = Date(file.lastModified()),
                        sizeBytes = file.length()
                    )
                }.sortedByDescending { it.dateModified }
            }
            
            if (videos.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyStateLayout.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyStateLayout.visibility = View.GONE
                adapter.submitList(videos)
            }
            
            SecureLogger.i("VideoGallery", "Loaded ${videos.size} videos")
        }
    }
    
    private fun playVideo(video: VideoItem) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(video.file), "video/mp4")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.error_camera_title)
                .setMessage("No video player found")
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
    
    private fun showDeleteDialog(video: VideoItem) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_video)
            .setMessage(getString(R.string.delete_video_confirm))
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteVideo(video)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    private fun deleteVideo(video: VideoItem) {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    video.file.delete()
                    // Also delete metadata file if exists
                    val metadataFile = StorageUtil.getMetadataFile(video.file)
                    if (metadataFile.exists()) {
                        metadataFile.delete()
                    }
                    true
                } catch (e: Exception) {
                    SecureLogger.e("VideoGallery", "Failed to delete video", e)
                    false
                }
            }
            
            if (success) {
                SecureLogger.i("VideoGallery", "Deleted video: ${video.name}")
                loadVideos() // Reload the list
            } else {
                AlertDialog.Builder(this@VideoGalleryActivity)
                    .setTitle(R.string.error_camera_title)
                    .setMessage("Failed to delete video")
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }
}
