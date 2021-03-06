package ua.andrii.andrushchenko.gimmepictures.ui.collection.details

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ua.andrii.andrushchenko.gimmepictures.R
import ua.andrii.andrushchenko.gimmepictures.databinding.FragmentCollectionDetailsBinding
import ua.andrii.andrushchenko.gimmepictures.domain.entities.Photo
import ua.andrii.andrushchenko.gimmepictures.ui.base.BasePagedAdapter
import ua.andrii.andrushchenko.gimmepictures.ui.base.BaseRecyclerViewFragment
import ua.andrii.andrushchenko.gimmepictures.ui.base.RecyclerViewLoadStateAdapter
import ua.andrii.andrushchenko.gimmepictures.ui.photo.PhotosAdapter
import ua.andrii.andrushchenko.gimmepictures.ui.widgets.AspectRatioImageView
import ua.andrii.andrushchenko.gimmepictures.util.setupStaggeredGridLayoutManager
import ua.andrii.andrushchenko.gimmepictures.util.toAmountReadableString

@AndroidEntryPoint
class CollectionDetailsFragment : BaseRecyclerViewFragment<Photo, FragmentCollectionDetailsBinding>(
    FragmentCollectionDetailsBinding::inflate) {

    private val args: CollectionDetailsFragmentArgs by navArgs()
    private val viewModel: CollectionDetailsViewModel by viewModels()

    override val pagedAdapter: BasePagedAdapter<Photo> =
        PhotosAdapter(object : PhotosAdapter.OnItemClickListener {
            override fun onPhotoClick(photo: Photo, photoImageView: AspectRatioImageView) {
                val direction =
                    CollectionDetailsFragmentDirections.actionGlobalPhotoDetailsFragment(photoId = photo.id)
                findNavController().navigate(direction)
            }
        })

    override val rv: RecyclerView
        get() = binding.collectionPhotosListingLayout.recyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            toolbar.apply {
                val navController = findNavController()
                val appBarConfiguration = AppBarConfiguration(
                    setOf(
                        R.id.nav_photos,
                        R.id.nav_collections,
                        R.id.nav_account
                    )
                )
                setupWithNavController(navController, appBarConfiguration)
                setOnClickListener { scrollRecyclerViewToTop() }
            }

            if (savedInstanceState == null) {
                viewModel.setCollection(args.collection)
            }

            viewModel.collection.observe(viewLifecycleOwner) { collection ->
                toolbar.title = collection.title

                collection.description?.let { description ->
                    descriptionTextView.apply {
                        visibility = View.VISIBLE
                        text = description
                    }
                }

                userNameTextView.apply {
                    setOnClickListener {
                        collection.user?.username?.let {
                            val direction =
                                CollectionDetailsFragmentDirections
                                    .actionCollectionDetailsFragmentToUserDetailsFragment(
                                        user = null,
                                        username = it
                                    )
                            findNavController().navigate(direction)
                        }
                    }

                    @SuppressLint("SetTextI18n")
                    text = "${
                        collection.totalPhotos.toAmountReadableString()
                    } ${
                        getString(R.string.photos).apply { first().lowercaseChar() }
                    } ${
                        getString(R.string.curated_by)
                    } ${
                        collection.user?.username
                    }"
                }

                if (viewModel.isUserAuthorized && viewModel.isOwnCollection) {
                    fabEditCollection.apply {
                        visibility = View.VISIBLE
                        setOnClickListener {
                            val direction = CollectionDetailsFragmentDirections
                                .actionCollectionDetailsFragmentToEditCollectionDialogFragment(
                                    collection)
                            findNavController().navigate(direction)
                        }
                    }
                }
            }

            viewModel.isDeleted.observe(viewLifecycleOwner) { isDeleted ->
                if (isDeleted) findNavController().navigateUp()
            }

            collectionPhotosListingLayout.swipeRefreshLayout.setOnRefreshListener {
                pagedAdapter.refresh()
            }

            pagedAdapter.addLoadStateListener { loadState ->
                collectionPhotosListingLayout.swipeRefreshLayout.isRefreshing =
                    loadState.refresh is LoadState.Loading
                rv.isVisible =
                    loadState.source.refresh is LoadState.NotLoading
                collectionPhotosListingLayout.textViewError.isVisible =
                    loadState.source.refresh is LoadState.Error

                // empty view
                if (loadState.source.refresh is LoadState.NotLoading &&
                    loadState.append.endOfPaginationReached &&
                    pagedAdapter.itemCount < 1
                ) {
                    rv.isVisible = false
                }
            }

            rv.apply {
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
        }

        viewModel.collectionPhotos.observe(viewLifecycleOwner) {
            pagedAdapter.submitData(viewLifecycleOwner.lifecycle, it)
        }
    }
}