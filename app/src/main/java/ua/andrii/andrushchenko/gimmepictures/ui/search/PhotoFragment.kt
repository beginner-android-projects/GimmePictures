package ua.andrii.andrushchenko.gimmepictures.ui.search

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ua.andrii.andrushchenko.gimmepictures.R
import ua.andrii.andrushchenko.gimmepictures.databinding.ListingLayoutBinding
import ua.andrii.andrushchenko.gimmepictures.models.Photo
import ua.andrii.andrushchenko.gimmepictures.ui.base.BasePagedAdapter
import ua.andrii.andrushchenko.gimmepictures.ui.base.BaseRecyclerViewFragment
import ua.andrii.andrushchenko.gimmepictures.ui.base.RecyclerViewLoadStateAdapter
import ua.andrii.andrushchenko.gimmepictures.ui.photo.PhotosAdapter
import ua.andrii.andrushchenko.gimmepictures.util.setupStaggeredGridLayoutManager

@AndroidEntryPoint
class PhotoFragment :
    BaseRecyclerViewFragment<Photo, ListingLayoutBinding>(ListingLayoutBinding::inflate) {

    private val viewModel: SearchViewModel by viewModels(
        ownerProducer = { requireParentFragment().childFragmentManager.primaryNavigationFragment!! }
    )

    override val pagedAdapter: BasePagedAdapter<Photo> =
        PhotosAdapter(object : PhotosAdapter.OnItemClickListener {
            override fun onPhotoClick(photo: Photo) {
                val direction =
                    SearchFragmentDirections.actionGlobalPhotoDetailsFragment(photoId = photo.id)
                findNavController().navigate(direction)
            }
        })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            swipeRefreshLayout.setOnRefreshListener {
                pagedAdapter.refresh()
            }

            pagedAdapter.addLoadStateListener { loadState ->
                swipeRefreshLayout.isRefreshing =
                    loadState.refresh is LoadState.Loading
                recyclerView.isVisible =
                    loadState.source.refresh is LoadState.NotLoading
                textViewError.isVisible =
                    loadState.source.refresh is LoadState.Error

                // empty view
                if (loadState.source.refresh is LoadState.NotLoading &&
                    loadState.append.endOfPaginationReached &&
                    pagedAdapter.itemCount < 1
                ) {
                    recyclerView.isVisible = false
                    textViewEmpty.isVisible = true
                } else {
                    textViewEmpty.isVisible = false
                }
            }

            recyclerView.apply {
                setHasFixedSize(true)
                layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL)
                setupStaggeredGridLayoutManager(
                    resources.configuration.orientation,
                    resources.getDimensionPixelSize(R.dimen.indent_8dp)
                )

                adapter = pagedAdapter.withLoadStateHeaderAndFooter(
                    header = RecyclerViewLoadStateAdapter { pagedAdapter.retry() },
                    footer = RecyclerViewLoadStateAdapter { pagedAdapter.retry() }
                )

            }

            viewModel.photoResults.observe(viewLifecycleOwner) {
                pagedAdapter.submitData(viewLifecycleOwner.lifecycle, it)
            }
        }
    }
}