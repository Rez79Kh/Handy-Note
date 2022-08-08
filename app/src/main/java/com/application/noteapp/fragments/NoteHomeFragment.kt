package com.application.noteapp.fragments

import android.animation.Animator
import android.content.res.Configuration
import android.graphics.Color
import androidx.biometric.BiometricPrompt
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.application.noteapp.R
import com.application.noteapp.activities.MainActivity
import com.application.noteapp.adapters.NotesAdapter
import com.application.noteapp.databinding.FragmentNoteHomeBinding
import com.application.noteapp.model.Note
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
import androidx.biometric.BiometricPrompt.PromptInfo
import java.util.concurrent.Executor

class NoteHomeFragment : Fragment(R.layout.fragment_note_home), NotesAdapter.EventListener {
    private val countNotesText: MutableLiveData<String> = MutableLiveData()
    val viewModel: NoteViewModel by activityViewModels()
    lateinit var binding: FragmentNoteHomeBinding
    lateinit var notesAdapter: NotesAdapter
    private lateinit var adapterListener: NotesAdapter.EventListener

    lateinit var executor: Executor
    lateinit var biometricPrompt: BiometricPrompt
    lateinit var promptInfo: PromptInfo

    var allNotesToUnlock:Boolean = false
    var notesToUnlock:ArrayList<Note> = ArrayList()
    var notesToUnlockPositions:ArrayList<Int> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialElevationScale(false).apply { duration = 300 }
        enterTransition = MaterialElevationScale(true).apply { duration = 300 }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNoteHomeBinding.bind(view)
        val navigator = Navigation.findNavController(view)
        val activity = activity as MainActivity
        requireView().hideKeyboard()

        initAuthentication()

        adapterListener = this

        CoroutineScope(Dispatchers.Main).launch {
            delay(10)
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            activity.window.statusBarColor = Color.parseColor("#f7f7ff")
        }

        binding.addNoteFloatingActButton.setOnClickListener {
            binding.appBar.visibility = View.INVISIBLE
            navigator.navigate(NoteHomeFragmentDirections.actionNoteHomeFragmentToAddOrUpdateNoteFragment())
        }

        initRecyclerView()

        deleteOnSwipe()

        binding.searchBarEditText.addTextChangedListener(object : TextWatcher {
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

        binding.searchBarEditText.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                view.clearFocus()
                requireView().hideKeyboard()
            }

            return@setOnEditorActionListener true

        }

        searchBarHandler()

    }

    private fun initAuthentication() {
        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this@NoteHomeFragment,executor,object : BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.e("Authentication Failed","Authentication Failed")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.e("Authentication result",result.toString())
                Log.e("Authentication Success","Authentication Success")

                Log.e("notesToUnlock",notesToUnlock.toString())
                Log.e("noteToUnlockPositions",notesToUnlockPositions.toString())
                if (allNotesToUnlock) {
                    viewModel.updateAllNoteLockState(false)
                } else {
                    for (note in notesToUnlock) {
                        note.is_locked = false
                        viewModel.updateNoteLockState(note.id, false)
                    }
                    for(pos in notesToUnlockPositions){
                        notesAdapter.notifyItemChanged(pos)
                    }
                }
                notesToUnlock.clear()
                notesToUnlockPositions.clear()
                allNotesToUnlock = false
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.e("Authentication ErrorCode",errorCode.toString())
                Log.e("Authentication ErrorString",errString.toString())
            }
        })

        promptInfo = PromptInfo.Builder()
            .setTitle("Access Authentication")
            .setDescription("Scan your fingerprint.")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL or BIOMETRIC_WEAK)
            .build()
    }

    private fun searchBarHandler() {
        binding.searchButton.setOnClickListener {
            binding.homePageTitle.visibility = View.GONE
            binding.closeSearchBarView.visibility = View.INVISIBLE
            binding.openSearchBarView.visibility = View.VISIBLE
            binding.searchBarEditText.setText("")
            val circularReveal = ViewAnimationUtils.createCircularReveal(
                binding.openSearchBarView,
                (binding.searchButton.right + binding.searchButton.left) / 2,
                (binding.searchButton.top + binding.searchButton.bottom) / 2,
                0f,
                binding.searchView.width.toFloat()
            )
            circularReveal.duration = 300
            circularReveal.start()

            binding.searchBarEditText.requestFocus()
        }

        binding.closeSearchButton.setOnClickListener {
            binding.homePageTitle.visibility = View.VISIBLE


            val circularReveal = ViewAnimationUtils.createCircularReveal(
                binding.openSearchBarView,
                (binding.searchButton.right + binding.searchButton.left) / 2,
                (binding.searchButton.top + binding.searchButton.bottom) / 2,
                binding.searchView.width.toFloat(),
                0f
            )
            circularReveal.duration = 300
            circularReveal.start()
            circularReveal.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) = Unit

                override fun onAnimationEnd(p0: Animator?) {
                    binding.openSearchBarView.visibility = View.INVISIBLE
                    binding.closeSearchBarView.visibility = View.VISIBLE
                    binding.searchBarEditText.setText("")
                    circularReveal.removeAllListeners()
                }

                override fun onAnimationCancel(p0: Animator?) = Unit

                override fun onAnimationRepeat(p0: Animator?) = Unit


            })
        }
    }

    private fun deleteOnSwipe() {
        val deleteOnSwipeCallback = object : DeleteWithSwipe() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val note = notesAdapter.currentList[position]
                var is_undoPressed = false
                viewModel.deleteNote(note)
                binding.searchBarEditText.apply {
                    hideKeyboard()
                    clearFocus()
                }
                if (binding.searchBarEditText.text.toString().isEmpty()) {
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
            notesAdapter = NotesAdapter(countNotesText, viewLifecycleOwner, adapterListener)
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

    override fun menuOnClick(notes: ArrayList<Note>,notesPositions: ArrayList<Int>, all_selected: Boolean, request: Int) {
        when (request) {
            1 -> {
                // delete request
                if (all_selected)
                    viewModel.deleteAllNotes()
                else {
                    for (note in notes)
                        viewModel.deleteNote(note)
                }
            }
            2 -> {
                // lock request
                if (all_selected) {
                    viewModel.updateAllNoteLockState(true)
                } else {
                    for (note in notes)
                        viewModel.updateNoteLockState(note.id, true)
                }
            }
            3 -> {
                // unlock request
                allNotesToUnlock= all_selected
                notesToUnlock.clear()
                notesToUnlockPositions.clear()
                notesToUnlock.addAll(notes)
                notesToUnlockPositions.addAll(notesPositions)

                biometricPrompt.authenticate(promptInfo)
            }
        }
    }
}