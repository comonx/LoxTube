package io.github.aedev.flow.player.floating

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.aedev.flow.R
import io.github.aedev.flow.data.model.Video
import java.util.Locale

fun interface OnVideoClickListener {
    fun onVideoClick(video: Video)
}

class FloatingRelatedVideosAdapter(
    private val onVideoClick: OnVideoClickListener,
) : RecyclerView.Adapter<FloatingRelatedVideosAdapter.ViewHolder>() {

    private val videos = mutableListOf<Video>()

    fun getVideos(): List<Video> = videos.toList()

    fun submitList(newVideos: List<Video>) {
        videos.clear()
        videos.addAll(newVideos)
        notifyDataSetChanged()
    }

    fun appendVideos(moreVideos: List<Video>) {
        val existingIds = videos.map { it.id }.toSet()
        val distinct = moreVideos.filter { it.id.isNotBlank() && !existingIds.contains(it.id) }
        if (distinct.isNotEmpty()) {
            val start = videos.size
            videos.addAll(distinct)
            notifyItemRangeInserted(start, distinct.size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_floating_related_video, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = videos[position]
        holder.bind(video)
    }

    override fun getItemCount(): Int = videos.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.related_item_thumbnail)
        val duration: TextView = itemView.findViewById(R.id.related_item_duration)
        val title: TextView = itemView.findViewById(R.id.related_item_title)
        val channel: TextView = itemView.findViewById(R.id.related_item_channel)
        val meta: TextView = itemView.findViewById(R.id.related_item_meta)

        fun bind(video: Video) {
            title.text = video.title
            channel.text = video.channelName

            // Duration
            if (video.duration > 0) {
                duration.visibility = View.VISIBLE
                duration.text = formatDuration(video.duration)
            } else {
                duration.visibility = View.GONE
            }

            // Views & time
            val viewsStr = formatViews(video.viewCount)
            val dateStr = video.uploadDate.trim()
            if (viewsStr.isNotBlank() && dateStr.isNotBlank()) {
                meta.text = "$viewsStr • $dateStr"
                meta.visibility = View.VISIBLE
            } else if (viewsStr.isNotBlank()) {
                meta.text = viewsStr
                meta.visibility = View.VISIBLE
            } else if (dateStr.isNotBlank()) {
                meta.text = dateStr
                meta.visibility = View.VISIBLE
            } else {
                meta.visibility = View.GONE
            }

            // Thumbnail
            FloatingImageLoader.loadThumbnail(thumbnail, video.thumbnailUrl)

            itemView.setOnClickListener {
                onVideoClick.onVideoClick(video)
            }
        }
    }

    private fun formatDuration(durationSecs: Int): String {
        val hours = durationSecs / 3600
        val mins = (durationSecs % 3600) / 60
        val secs = durationSecs % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    private fun formatViews(views: Long): String {
        return when {
            views >= 1_000_000_000 -> String.format(Locale.US, "%.1fB views", views / 1_000_000_000.0)
            views >= 1_000_000 -> String.format(Locale.US, "%.1fM views", views / 1_000_000.0).replace(".0M", "M")
            views >= 1_000 -> String.format(Locale.US, "%.1fK views", views / 1_000.0).replace(".0K", "K")
            views > 0 -> "$views views"
            else -> ""
        }
    }
}
