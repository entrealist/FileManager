package pro.filemanager.videos

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.signature.MediaStoreSignature
import kotlinx.coroutines.launch
import pro.filemanager.databinding.LayoutVideoItemBinding

class VideoBrowserAdapter(val context: Context, val videoItems: MutableList<VideoItem>, val layoutInflater: LayoutInflater, val hostFragment: VideoBrowserFragment) : RecyclerView.Adapter<VideoBrowserAdapter.VideoItemViewHolder>() {

    class VideoItemViewHolder(val context: Context, val binding: LayoutVideoItemBinding) : RecyclerView.ViewHolder(binding.root) {

        lateinit var item: VideoItem

        init {
            binding.layoutVideoItemRootLayout.setOnClickListener {
                VideoManager.openInOtherVideoPlayer(context, item.data)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoItemViewHolder {
        return VideoItemViewHolder(context, LayoutVideoItemBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: VideoItemViewHolder, position: Int) {

        hostFragment.MainScope.launch {

            holder.item = videoItems[position]

            VideoManager.glideVideoRequestBuilder
                    .load(holder.item.data)
                    .override(holder.binding.layoutVideoItemThumbnail.width, holder.binding.layoutVideoItemThumbnail.height)
                    .signature(MediaStoreSignature(VideoManager.MIME_TYPE, holder.item.dateModified.toLong(), 0))
                    .into(holder.binding.layoutVideoItemThumbnail)

        }

    }

    override fun getItemCount(): Int {
        return videoItems.size
    }
}