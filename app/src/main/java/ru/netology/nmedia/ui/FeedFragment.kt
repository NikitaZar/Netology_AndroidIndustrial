package ru.netology.nmedia.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.FeedAdapter
import ru.netology.nmedia.adapter.FeedLoadStateAdapter
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.ui.FullscreenAttachmentFragment.Companion.url
import ru.netology.nmedia.viewmodel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val adapter = FeedAdapter(
            object : OnInteractionListener {
                override fun onEdit(post: Post) {
                    viewModel.edit(post)
                }

                override fun onLike(post: Post) {
                    when (post.likedByMe) {
                        false -> viewModel.likeById(post.id)
                        true -> viewModel.dislikeById(post.id)
                    }
                }

                override fun onRemove(post: Post) {
                    viewModel.removeById(post.id)
                }

                override fun onShare(post: Post) {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, post.content)
                        type = "text/plain"
                    }

                    val shareIntent =
                        Intent.createChooser(intent, getString(R.string.chooser_share_post))
                    startActivity(shareIntent)
                }

                override fun onResend(post: Post) {
                    viewModel.retrySave(post)
                }

                override fun onFullscreenAttachment(attachmentUrl: String) {
                    findNavController().navigate(
                        R.id.action_feedFragment_to_fullscreenAttachmentFragment,
                        Bundle().apply { this.url = attachmentUrl }
                    )

                }

                override fun onAuth() {
                    findNavController().navigate(R.id.action_feedFragment_to_authFragment)
                }
            },

            appAuth
        )

        binding.list.adapter = adapter.withLoadStateHeader(
            header = FeedLoadStateAdapter { viewModel.loadPosts() }
        )

        lifecycleScope.launchWhenCreated {
            viewModel.data.collectLatest(adapter::submitData)
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest { state ->
                if (state.refresh is LoadState.Loading) {
                    binding.list.smoothScrollToPosition(0)
                }
            }
        }

        viewModel.dataState.observe(viewLifecycleOwner) { dataState ->
            binding.progress.isVisible = dataState.loading

            if (dataState.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry_loading) {
                        viewModel.retryActon(dataState.actionType, dataState.actionId)
                    }.show()
            }
        }

        binding.fab.setOnClickListener {
            when (appAuth.authStateFlow.value.id != 0L) {
                false -> findNavController().navigate(R.id.action_feedFragment_to_authFragment)
                true -> findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
            }
        }

        viewModel.newerCount.observe(viewLifecycleOwner) { newerCount ->
            if (newerCount > 20) {
                binding.fabNewer.show()
            }
        }

        binding.fabNewer.setOnClickListener {
            viewModel.loadPosts()
            viewModel.asVisibleAll()
            binding.list.smoothScrollToPosition(0)
            binding.fabNewer.hide()
        }

        return binding.root
    }
}
