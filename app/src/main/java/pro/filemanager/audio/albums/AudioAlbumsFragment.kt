package pro.filemanager.audio.albums

import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import pro.filemanager.ApplicationLoader
import pro.filemanager.HomeActivity
import pro.filemanager.R
import pro.filemanager.core.KEY_TRANSIENT_PARCELABLE_ALBUMS_MAIN_LIST_RV_STATE
import pro.filemanager.core.KEY_TRANSIENT_STRINGS_ALBUMS_SEARCH_TEXT
import pro.filemanager.core.SimpleInjector
import pro.filemanager.core.UIManager
import pro.filemanager.core.tools.SelectionTool
import pro.filemanager.databinding.FragmentAudioAlbumsBinding
import java.lang.IllegalStateException

class AudioAlbumsFragment : Fragment(), Observer<MutableList<AudioAlbumItem>> {

    lateinit var binding: FragmentAudioAlbumsBinding
    lateinit var navController: NavController
    lateinit var activity: HomeActivity
    lateinit var viewModel: AudioAlbumsViewModel

    lateinit var onBackCallback: OnBackPressedCallback

    override fun onChanged(t: MutableList<AudioAlbumItem>?) {
        if(binding.fragmentAudioAlbumsList.adapter != null) {
            try {
                viewModel.MainScope?.cancel()
                viewModel.MainScope = CoroutineScope(Main)
            } catch (thr: Throwable) {

            }

            (binding.fragmentAudioAlbumsList.adapter as AudioAlbumsAdapter).audioAlbumItems = t!!
            binding.fragmentAudioAlbumsList.adapter!!.notifyDataSetChanged()

            binding.fragmentAudioAlbumsList.scrollToPosition(0)

            viewModel.searchInProgress = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAudioAlbumsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        navController = Navigation.findNavController(binding.root)

        activity = requireActivity() as HomeActivity

        setHasOptionsMenu(true)

        activity.setSupportActionBar(binding.fragmentAudioAlbumsToolbarInclude.layoutBaseToolbar)

        onBackCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navController.popBackStack(R.id.audioBrowserFragment, true)
            }
        }

        activity.requestExternalStoragePermission {
            ApplicationLoader.ApplicationIOScope.launch {
                viewModel = ViewModelProviders.of(this@AudioAlbumsFragment, SimpleInjector.provideAudioAlbumsViewModelFactory()).get(AudioAlbumsViewModel::class.java)

                withContext(Main) {
                    try {
                        viewModel.getAlbumsLive().observe(viewLifecycleOwner, this@AudioAlbumsFragment)

                        ApplicationLoader.transientStrings[KEY_TRANSIENT_STRINGS_ALBUMS_SEARCH_TEXT].let {
                            if(!it.isNullOrEmpty()) {
                                viewModel.isSearchViewEnabled = true
                                viewModel.currentSearchText = it

                                viewModel.search(requireContext(), viewModel.currentSearchText)
                            }
                        }

                        initAdapter(viewModel.getAlbumsLive().value!!)

                    } catch (e: IllegalStateException) {
                        e.printStackTrace()

                        // TODO: MediaStore fetching failed with IllegalStateException.
                        //  Most likely, it is something out of our hands.
                        //  Show "Something went wrong" dialog

                    } finally {

                    }


                    if (viewModel.selectionTool == null)
                        viewModel.selectionTool = SelectionTool()

                    viewModel.selectionTool!!.initOnBackCallback(
                            activity,
                            binding.fragmentAudioAlbumsList.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>,
                            binding.fragmentAudioAlbumsToolbarInclude.layoutSelectionBarInclude.layoutSelectionBarRootLayoutSelectionCountCb,
                            binding.fragmentAudioAlbumsToolbarInclude.layoutSelectionBarInclude.layoutSelectionBarRootLayout,
                            binding.fragmentAudioAlbumsBottomToolBarInclude.layoutBottomToolBarRootLayout,
                            binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomTabsBarRootLayout
                    )

                    if (viewModel.selectionTool!!.selectionMode) {
                        if (viewModel.selectionTool!!.selectedPositions.isNotEmpty()) {
                            activity.supportActionBar?.hide()
                            binding.fragmentAudioAlbumsToolbarInclude.layoutSelectionBarInclude.layoutSelectionBarRootLayout.visibility = View.VISIBLE
                            binding.fragmentAudioAlbumsToolbarInclude.layoutSelectionBarInclude.layoutSelectionBarRootLayoutSelectionCountCb.text = viewModel.selectionTool!!.selectedPositions.size.toString()
                        }

                        binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomTabsBarRootLayout.visibility = View.GONE
                        binding.fragmentAudioAlbumsBottomToolBarInclude.layoutBottomToolBarRootLayout.visibility = View.VISIBLE
                    }

                    binding.fragmentAudioAlbumsToolbarInclude.layoutSelectionBarInclude.layoutSelectionBarRootLayoutSelectionCountCb.setOnCheckedChangeListener { _: CompoundButton, b: Boolean ->
                        if (b) {
                            viewModel.selectionTool!!.selectAll(binding.fragmentAudioAlbumsList.adapter!!, binding.fragmentAudioAlbumsToolbarInclude.layoutSelectionBarInclude.layoutSelectionBarRootLayoutSelectionCountCb)
                        } else {
                            viewModel.selectionTool!!.unselectAll(binding.fragmentAudioAlbumsList.adapter!!, binding.fragmentAudioAlbumsToolbarInclude.layoutSelectionBarInclude.layoutSelectionBarRootLayoutSelectionCountCb)
                        }
                    }
                }
            }
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackCallback)

        binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomTabsBarRootLayout.post {
            binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomTabsBarRootLayout.height.let {
                binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomBarGalleryTitle.textSize = (it / 8).toFloat()
                binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomBarGalleryTitle.text = resources.getText(R.string.title_library)

                binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomTabsBarAlbumsTitle.textSize = (it / 8).toFloat()
                binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomTabsBarAlbumsTitle.text = resources.getText(R.string.title_folders)
            }
        }

        binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomTabsBarAlbumsTitle.setTypeface(null, Typeface.BOLD)
        binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomTabsBarAlbumsTitleIndicator.visibility = View.VISIBLE

        binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomTabsBarGalleryTitleContainer.setOnClickListener {
            onBackCallback.isEnabled = false
            ApplicationLoader.transientParcelables[KEY_TRANSIENT_PARCELABLE_ALBUMS_MAIN_LIST_RV_STATE] = binding.fragmentAudioAlbumsList.layoutManager?.onSaveInstanceState()
            activity.onBackPressed()
        }

    }

    private fun initAdapter(audioAlbumItems: MutableList<AudioAlbumItem>) {
        binding.fragmentAudioAlbumsList.layoutManager = GridLayoutManager(context, UIManager.getAlbumGridSpanNumber(requireActivity()))
        ApplicationLoader.transientParcelables[KEY_TRANSIENT_PARCELABLE_ALBUMS_MAIN_LIST_RV_STATE].let {
            if(it != null) {
                binding.fragmentAudioAlbumsList.layoutManager?.onRestoreInstanceState(it)
                ApplicationLoader.transientParcelables.remove(KEY_TRANSIENT_PARCELABLE_ALBUMS_MAIN_LIST_RV_STATE)
            } else
                binding.fragmentAudioAlbumsList.layoutManager?.onRestoreInstanceState(viewModel.mainListRvState)
        }

        binding.fragmentAudioAlbumsList.adapter = AudioAlbumsAdapter(requireActivity(), audioAlbumItems, layoutInflater, this@AudioAlbumsFragment)

        binding.fragmentAudioAlbumsList.itemAnimator = object : DefaultItemAnimator() {
            override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                return true
            }
        }

        binding.fragmentAudioAlbumsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dx > 0 || dy > 0) {
                    if(this@AudioAlbumsFragment::viewModel.isInitialized && viewModel.selectionTool != null && !viewModel.selectionTool!!.selectionMode)
                        binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomTabsBarRootLayout.visibility = View.GONE
                } else {
                    if(this@AudioAlbumsFragment::viewModel.isInitialized && viewModel.selectionTool != null && !viewModel.selectionTool!!.selectionMode)
                        binding.fragmentAudioAlbumsBottomTabsBarInclude.layoutBottomTabsBarRootLayout.visibility = View.VISIBLE
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()

        activity.supportActionBar?.title = requireContext().resources.getString(R.string.title_folders)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_toolbar_menu, menu)

        val searchView = menu.findItem(R.id.mainToolbarMenuItemSearch).actionView as SearchView

        searchView.post {
            searchView.apply {
                imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI

                setOnSearchClickListener {
                    viewModel.isSearchViewEnabled = true
                }

                setOnCloseListener {
                    viewModel.isSearchViewEnabled = false
                    ApplicationLoader.transientStrings.remove(KEY_TRANSIENT_STRINGS_ALBUMS_SEARCH_TEXT)
                    false
                }

                if(this@AudioAlbumsFragment::viewModel.isInitialized && viewModel.isSearchViewEnabled) {
                    setQuery(viewModel.currentSearchText, false)
                    isIconified = false
                    requestFocus()

                    if(viewModel.currentSearchText.isEmpty()) clearFocus()
                } else {
                    isIconified = true
                }

                setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                    override fun onQueryTextSubmit(query: String?): Boolean {

                        return false
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.search(requireContext(), newText)

                        ApplicationLoader.transientStrings[KEY_TRANSIENT_STRINGS_ALBUMS_SEARCH_TEXT] = newText
                        return false
                    }

                })
            }
        }
    }

    override fun onStop() {
        super.onStop()

        if(this::viewModel.isInitialized)
            viewModel.mainListRvState = binding.fragmentAudioAlbumsList.layoutManager?.onSaveInstanceState()

    }

}