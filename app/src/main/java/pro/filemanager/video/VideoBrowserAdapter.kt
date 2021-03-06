package pro.filemanager.video

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.signature.MediaStoreSignature
import kotlinx.coroutines.launch
import pro.filemanager.databinding.LayoutVideoItemBinding

class VideoBrowserAdapter(val context: Context, val videoItems: MutableList<VideoItem>, val layoutInflater: LayoutInflater, val hostFragment: VideoBrowserFragment) : RecyclerView.Adapter<VideoBrowserAdapter.VideoItemViewHolder>() {

    class VideoItemViewHolder(val context: Context, val binding: LayoutVideoItemBinding, val hostFragment: VideoBrowserFragment, val adapter: VideoBrowserAdapter) : RecyclerView.ViewHolder(binding.root) {

        lateinit var item: VideoItem

        init {
            binding.layoutVideoItemRootLayout.apply {
                setOnClickListener {
//                    @Suppress("UNCHECKED_CAST")
//                    hostFragment.viewModel.selectionTool?.handleClickInViewHolder(SelectionTool.CLICK_SHORT, adapterPosition, adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>, hostFragment.requireActivity() as HomeActivity) {
//                        FileCore.openFileOut(this@VideoItemViewHolder.context, item.data)
//                    }

                }

                setOnLongClickListener {
//                    @Suppress("UNCHECKED_CAST")
//                    hostFragment.viewModel.selectionTool?.handleClickInViewHolder(SelectionTool.CLICK_LONG, adapterPosition, adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>, hostFragment.requireActivity() as HomeActivity)
//
                    true
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoItemViewHolder {
        return VideoItemViewHolder(context, LayoutVideoItemBinding.inflate(layoutInflater, parent, false), hostFragment, this)
    }

    override fun onBindViewHolder(holder: VideoItemViewHolder, position: Int) {
        holder.item = videoItems[position]

        hostFragment.MainScope.launch {

            VideoCore.glideRequestBuilder
                    .load(holder.item.data)
                    .override(holder.binding.layoutVideoItemThumbnail.width, holder.binding.layoutVideoItemThumbnail.height)
                    .signature(MediaStoreSignature(VideoCore.MIME_TYPE, videoItems[position].dateModified.toLong(), 0))
                    .into(holder.binding.layoutVideoItemThumbnail)

            hostFragment.viewModel.selectionTool?.differentiateItem(position, holder.binding.layoutVideoItemThumbnail, holder.binding.layoutVideoItemIconCheck, holder.binding.layoutVideoItemIconUnchecked)

        }

    }

    override fun getItemCount(): Int {
        return videoItems.size
    }
}