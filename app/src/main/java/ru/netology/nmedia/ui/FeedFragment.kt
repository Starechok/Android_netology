package ru.netology.nmedia.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.badge.BadgeDrawable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nmedia.R
import ru.netology.nmedia.R.id.action_fragmentFeed_to_editPostFragment
import ru.netology.nmedia.ui.NewPostFragment.Companion.textArg
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.FeedAdapter
import ru.netology.nmedia.adapter.PagingLoadStateAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.PostViewModel
import java.io.File

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )
    private val viewModelAuth: AuthViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val adapter = FeedAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                findNavController().navigate(
                    action_fragmentFeed_to_editPostFragment,
                    Bundle().apply
                    {
                        textArg = post.content
                        viewModel.edit(post)
                    })
            }

            override fun onLike(post: Post) {
                if (!viewModelAuth.authenticated) {
                    findNavController().navigate(R.id.action_fragmentFeed_to_fragmentSignIn)
                    return
                } else viewModel.likeById(post.id)

            }

            override fun onRemove(post: Post) {
                if (!viewModelAuth.authenticated) {
                    findNavController().navigate(R.id.action_fragmentFeed_to_fragmentSignIn)
                    return
                }
                viewModel.removeById(post.id)
            }

            override fun onRepost(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }
                val repostIntent = Intent.createChooser(intent, getString(R.string.chooser_repost))
                startActivity(repostIntent)
                viewModel.repostById(post.id)
            }

            override fun onOpenPost(post: Post) {
                findNavController().navigate(R.id.action_fragmentFeed_to_cardPostFragment,
                    Bundle().apply {
                        putParcelable("post", post)
                    })
            }

            override fun onViewImage(post: Post) {
                findNavController().navigate(R.id.action_fragmentFeed_to_fragmentImage,
                    Bundle().apply
                    {
                        textArg = post.attachment?.url
                    })
            }

        })
        binding.list.addItemDecoration(
            DividerItemDecoration(binding.list.context, DividerItemDecoration.VERTICAL)
        )

        binding.list.adapter = adapter.withLoadStateHeaderAndFooter(
            header = PagingLoadStateAdapter(object : PagingLoadStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    adapter.retry()
                }
            }),
            footer = PagingLoadStateAdapter(object : PagingLoadStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    adapter.retry()
                }
            }),
        )

        lifecycleScope.launchWhenCreated {
            viewModel.dataPaging.collectLatest(adapter::submitData)
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest { state ->
                binding.swipeRefreshLayout.isRefreshing =
                    state.refresh is LoadState.Loading 
            }
        }

        val badge = requireContext().let { BadgeDrawable.create(it) }
            .apply { isVisible = true }

        viewModel.newerCount.observe(viewLifecycleOwner) { state ->
            if (state > 0) {
                binding.newer.isVisible = true
                badge.number = state
            } else {
                binding.newer.isInvisible = true
            }
        }


        binding.newer.setOnClickListener {
            binding.list.smoothScrollToPosition(0)
            binding.newer.isInvisible = true
            viewModel.readAll()

        }

        binding.fab.setOnClickListener {
            if (!viewModelAuth.authenticated) {
                findNavController().navigate(R.id.action_fragmentFeed_to_fragmentSignIn)
                return@setOnClickListener
            }
            findNavController().navigate(R.id.action_fragmentFeed_to_newPostFragment,
                Bundle().apply
                {
                    val file = File(context?.filesDir, "savecontent.json")
                    if (file.exists()) textArg = file.readText()
                })
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            adapter.refresh()
        }


        return binding.root
    }
}
