package com.logicam.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.videoFrameMillis
import com.logicam.R
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * RecyclerView adapter for displaying video thumbnails
 */
class VideoThumbnailAdapter(
    private val onVideoClick: (VideoItem) -> Unit,
    private val onVideoLongClick: (VideoItem) -> Unit
) : ListAdapter<VideoItem, VideoThumbnailAdapter.VideoViewHolder>(VideoDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_thumbnail, parent, false)
        return VideoViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = getItem(position)
        holder.bind(video, onVideoClick, onVideoLongClick)
    }
    
    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailView: ImageView = itemView.findViewById(R.id.videoThumbnail)
        private val nameView: TextView = itemView.findViewById(R.id.videoName)
        private val dateView: TextView = itemView.findViewById(R.id.videoDate)
        private val sizeView: TextView = itemView.findViewById(R.id.videoSize)
        
        fun bind(
            video: VideoItem,
            onVideoClick: (VideoItem) -> Unit,
            onVideoLongClick: (VideoItem) -> Unit
        ) {
            // Load video thumbnail using Coil
            thumbnailView.load(video.file) {
                crossfade(true)
                videoFrameMillis(1000) // Get frame at 1 second
                placeholder(R.drawable.ic_video_placeholder)
                error(R.drawable.ic_video_placeholder)
            }
            
            nameView.text = video.displayName
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            dateView.text = dateFormat.format(video.dateModified)
            
            sizeView.text = video.sizeMB
            
            itemView.setOnClickListener { onVideoClick(video) }
            itemView.setOnLongClickListener {
                onVideoLongClick(video)
                true
            }
        }
    }
    
    private class VideoDiffCallback : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem.file.absolutePath == newItem.file.absolutePath
        }
        
        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem == newItem
        }
    }
}
