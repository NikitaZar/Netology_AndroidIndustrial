package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.databinding.CardTextItemSeparatorBinding
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.TextItemSeparator
import ru.netology.nmedia.view.load
import ru.netology.nmedia.view.loadCircleCrop

const val TYPE_POST = 0
const val TYPE_TEXT_SEPARATOR = 1

interface OnInteractionListener {
    fun onLike(post: Post)
    fun onEdit(post: Post)
    fun onRemove(post: Post)
    fun onShare(post: Post)
    fun onResend(post: Post)
    fun onFullscreenAttachment(attachmentUrl: String)
    fun onAuth()
}

class FeedAdapter(
    private val onInteractionListener: OnInteractionListener,
    private val appAuth: AppAuth
) : PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(FeedDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Post -> TYPE_POST
            is TextItemSeparator -> TYPE_TEXT_SEPARATOR
            else -> throw IllegalArgumentException("unknown item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_POST -> PostViewHolder(
                CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onInteractionListener,
                appAuth
            )
            TYPE_TEXT_SEPARATOR -> TextItemViewHolder(
                CardTextItemSeparatorBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                onInteractionListener,
            )
            else -> throw IllegalArgumentException("unknown item type")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position)?.let { feedItem ->
            when (feedItem) {
                is Post -> (holder as PostViewHolder).bind(feedItem)
                is TextItemSeparator -> (holder as TextItemViewHolder).bind(feedItem)
            }
        }
    }
}

class TextItemViewHolder(
    private val binding: CardTextItemSeparatorBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(textItemSeparator: TextItemSeparator) {
        binding.text.text = textItemSeparator.text
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener,
    private val appAuth: AppAuth
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            published.text = post.published
            content.text = post.content
            like.isChecked = post.likedByMe
            like.text = "${post.likes}"
            attachment.isVisible = false

            val avatarUrl = "${BuildConfig.BASE_URL}avatars/${post.authorAvatar}"
            avatar.loadCircleCrop(avatarUrl)

            post.attachment?.let { postAttachment ->
                val attachmentUrl = "${BuildConfig.BASE_URL}media/${postAttachment.url}"
                attachment.load(attachmentUrl)
                attachment.isVisible = true

                attachment.setOnClickListener {
                    onInteractionListener.onFullscreenAttachment(attachmentUrl)
                }
            }

            menu.isVisible = post.ownedByMe
            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
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
                            R.id.resend -> {
                                onInteractionListener.onResend(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            like.setOnClickListener {
                if (appAuth.authStateFlow.value.id == 0L) {
                    onInteractionListener.onAuth()
                    return@setOnClickListener
                }
                onInteractionListener.onLike(post)
            }

            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }
        }
    }
}

class FeedDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        if (oldItem::class.java != newItem::class.java) {
            return false
        }

        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        return oldItem == newItem
    }
}
