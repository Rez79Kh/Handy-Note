package com.application.noteapp.fragments

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.application.noteapp.R
import com.application.noteapp.activities.MainActivity
import com.application.noteapp.adapters.NotesAdapter
import com.application.noteapp.databinding.FragmentNoteHomeBinding
import com.application.noteapp.util.DeleteWithSwipe
import com.application.noteapp.util.hideKeyboard
import com.application.noteapp.viewmodel.NoteViewModel
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NoteHomeFragment : Fragment(R.layout.fragment_note_home) {
    val viewModel: NoteViewModel by activityViewModels()
    lateinit var binding: FragmentNoteHomeBinding
    lateinit var notesAdapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialElevationScale(false).apply { duration = 300 }
        enterTransition = MaterialElevationScale(true).apply { duration = 300 }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNoteHomeBinding.bind(view)
        val navigator = Navigation.findNavController(view)
        var activity = activity as MainActivity
        requireView().hideKeyboard()

        CoroutineScope(Dispatchers.Main).launch {
            delay(10)
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            activity.window.statusBarColor = Color.parseColor("#9E9D9D")
        }

        binding.addNoteButton.setOnClickListener {
            binding.appBar.visibility = View.INVISIBLE
            navigator.navigate(NoteHomeFragmentDirections.actionNoteHomeFragmentToAddOrUpdateNoteFragment())
        }

        binding.addNoteFloatingActButton.setOnClickListener {
            binding.appBar.visibility = View.INVISIBLE
            navigator.navigate(NoteHomeFragmentDirections.actionNoteHomeFragmentToAddOrUpdateNoteFragment())
        }

        initRecyclerView()

        deleteOnSwipe()

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(ch: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (ch.toString().isNotEmpty()) {
                    val text: String = ch.toString()
                    val query: String = "%$text%"
                    if (query.isNotEmpty()) {
                        viewModel.findNote(query).observe(viewLifecycleOwner) {
                            notesAdapter.submitList(it)
                        }
                    } else {
                        observeData()
                    }
                } else {
                    observeData()
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        binding.notesRecyclerView.setOnScrollChangeListener { _, scrollX, scrollY, _, oldScrollY ->
            when {
                scrollY > oldScrollY -> binding.addNoteButtonText.isVisible = false
                scrollX == scrollY -> binding.addNoteButtonText.isVisible = true
                else -> binding.addNoteButtonText.isVisible = true
            }
        }

        binding.searchBar.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                view.clearFocus()
                requireView().hideKeyboard()
            }

            return@setOnEditorActionListener true

        }

    }

    private fun deleteOnSwipe() {
        val deleteOnSwipeCallback = object : DeleteWithSwipe() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val note = notesAdapter.currentList[position]
                var is_undoPressed = false
                viewModel.deleteNote(note)
                binding.searchBar.apply {
                    hideKeyboard()
                    clearFocus()
                }
                if (binding.searchBar.text.toString().isEmpty()) {
                    observeData()
                }
                val snackbar =
                    Snackbar.make(requireView(), "The Note Was Deleted", Snackbar.LENGTH_LONG)
                        .addCallback(object :
                            BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                            }

                            override fun onShown(transientBottomBar: Snackbar?) {
                                transientBottomBar?.setAction("UNDO") {
                                    viewModel.insertNote(note)
                                    is_undoPressed = true
                                    binding.noDataFoundLayout.isVisible = false
                                }

                                super.onShown(transientBottomBar)
                            }
                        }
                        ).apply {
                            animationMode = Snackbar.ANIMATION_MODE_FADE
                            setAnchorView(R.id.addNoteButton)
                        }
                snackbar.setActionTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.yellow
                    )
                )
                snackbar.show()
            }

        }
        val itemTouchHelper = ItemTouchHelper(deleteOnSwipeCallback)
        itemTouchHelper.attachToRecyclerView(binding.notesRecyclerView)
    }

    private fun observeData() {
        viewModel.getAllNotes().observe(viewLifecycleOwner) { notesList ->
            binding.noDataFoundLayout.isVisible = notesList.isEmpty()
            notesAdapter.submitList(notesList)
        }
    }

    private fun initRecyclerView() {
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> showNotes(2)
            Configuration.ORIENTATION_LANDSCAPE -> showNotes(3)
        }
    }

    private fun showNotes(columnCount: Int) {
        binding.notesRecyclerView.apply {
            layoutManager =
                StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(true)
            notesAdapter = NotesAdapter()
            notesAdapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            adapter = notesAdapter
            postponeEnterTransition(300, TimeUnit.MILLISECONDS)
            viewTreeObserver.addOnPreDrawListener {
                startPostponedEnterTransition()
                true
            }

        }
        observeData()
    }
}