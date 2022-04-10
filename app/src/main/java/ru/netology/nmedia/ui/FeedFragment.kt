package ru.netology.nmedia.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.*
import androidx.fragment.app.*
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.ui.FullscreenAttachmentFragment.Companion.url
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel
import javax.inject.Inject
import androidx.paging.LoadState
import kotlinx.coroutines.flow.collectLatest

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

        val adapter = PostsAdapter(
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

        binding.list.adapter = adapter

        lifecycleScope.launchWhenCreated {
            viewModel.data.collectLatest(adapter::submitData)
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest { state ->
                binding.swipeRefresh.isRefreshing =
                    state.refresh is LoadState.Loading ||
                            state.prepend is LoadState.Loading ||
                            state.append is LoadState.Loading

                if (state.refresh is LoadState.Loading) {
                    binding.list.smoothScrollToPosition(0)
                }
            }
        }

        //TODO check
//        adapter.addLoadStateListener { loadState ->
//            binding.emptyText.isVisible =
//                (loadState.source.refresh is LoadState.NotLoading &&
//                        loadState.append.endOfPaginationReached && adapter.itemCount < 1)
//        }

        //TODO: show emptyText
//        viewModel.data.asLiveData().observe(viewLifecycleOwner) { data ->
//            val isNewPost = (adapter.itemCount < data.posts.size) && (adapter.itemCount > 0)
//            adapter.submitList(data.posts) {
//                if (isNewPost) {
//                    binding.list.smoothScrollToPosition(0)
//                }
//            }

//            binding.emptyText.isVisible = data.empty
//    }

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

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadPosts()
        }

        viewModel.newerCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                binding.fabNewer.show()
            }
        }

        binding.fabNewer.setOnClickListener {
            viewModel.asVisibleAll()
            binding.list.smoothScrollToPosition(0)
            binding.fabNewer.hide()
        }

        return binding.root
    }
}