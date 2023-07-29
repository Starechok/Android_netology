package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardAdBinding
import ru.netology.nmedia.databinding.PostCardBinding
import ru.netology.nmedia.dto.AdItem
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.view.load
import ru.netology.nmedia.view.loadCircleCrop
import java.lang.IllegalArgumentException

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onRepost(post: Post) {}
    fun onVideo(post: Post) {}
    fun onOpenPost(post: Post) {}
    fun onViewImage(post: Post) {}
    fun onAdClicked(adItem: AdItem) {}
}

class FeedAdapter(
    private val onInteractionListener: OnInteractionListener,
) : PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(FeedItemDiffCallback()) {
    private val typeAd = 0
    private val typePost = 1

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is AdItem -> typeAd
            is Post -> typePost
            null -> throw IllegalArgumentException("unknown item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            typeAd -> AdViewHolder(
                CardAdBinding.inflate(layoutInflater, parent, false),
                onInteractionListener
            )
            typePost -> PostViewHolder(
                PostCardBinding.inflate(layoutInflater, parent, false),
                onInteractionListener
            )
            else -> throw IllegalArgumentException("unknown view type: $viewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is AdItem -> (holder as? AdViewHolder)?.bind(item)
            is Post -> (holder as? PostViewHolder)?.bind(item)
            null -> error("item must not be null")
        }
    }
}

class AdViewHolder(
    private val binding: CardAdBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: AdItem) {
        binding.apply {
            image.load("${BuildConfig.BASE_URL}/media/${item.image}")
            image.setOnClickListener { onInteractionListener.onAdClicked(item) }
        }
    }
}

class PostViewHolder(
    private val binding: PostCardBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            avatar.loadCircleCrop("${BuildConfig.BASE_URL}/avatars/${post.authorAvatar}")
            content.text = post.content
            like.isChecked = post.likedByMe
            like.text = "${post.likes}"
            if (!post.videoUrl.isNullOrEmpty()) {
                video.visibility = View.VISIBLE
            } else video.visibility = View.GONE

            if (post.attachment != null && post.attachment.type == AttachmentType.IMAGE) {
                attachmentView.visibility = View.VISIBLE
                attachmentView.load("${BuildConfig.BASE_URL}/media/${post.attachment.url}")
            } else attachmentView.visibility = View.GONE

            menu.visibility = if (post.ownedByMe) View.VISIBLE else View.INVISIBLE

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    // TODO: if we don't have other options, just remove dots
                    menu.setGroupVisible(R.id.owned, post.ownedByMe)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }
                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }
                            else -> false
                        }
                    }
                }.show()
            }
            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }
            repost.setOnClickListener {
                onInteractionListener.onRepost(post)
            }
            video.setOnClickListener {
                onInteractionListener.onVideo(post)
            }
            content.setOnClickListener {
                onInteractionListener.onOpenPost(post)
            }
            attachmentView.setOnClickListener {
                onInteractionListener.onViewImage(post)
            }
        }
    }
}


class FeedItemDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        if (oldItem::class != newItem::class) {
            return false
        }
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        return oldItem == newItem
    }
}

