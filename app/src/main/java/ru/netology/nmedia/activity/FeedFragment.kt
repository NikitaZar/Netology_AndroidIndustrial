package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.*
import androidx.fragment.app.*
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val adapter = PostsAdapter(requireContext(), object : OnInteractionListener {
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
        })

        binding.list.adapter = adapter
        viewModel.data.observe(viewLifecycleOwner) { data ->
            val isNewPost = adapter.itemCount < data.posts.size
            adapter.submitList(data.posts) {
                if (isNewPost) {
                    binding.list.smoothScrollToPosition(0)
                }
            }

            binding.emptyText.isVisible = data.empty

            viewModel.dataState.observe(viewLifecycleOwner) { dataState ->
                binding.progress.isVisible = dataState.loading

                if (!dataState.loading) {
                    binding.swipeRefresh.isRefreshing = false
                }

                if (dataState.error) {
                    Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.retry_loading) {
                            viewModel.retryActon(dataState.actionType, dataState.actionId)
                        }.show()
                }
            }
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadPosts()
        }

        return binding.root
    }
}
