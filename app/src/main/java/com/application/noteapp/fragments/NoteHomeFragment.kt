package com.application.noteapp.fragments

import android.animation.Animator
import android.content.res.ColorStateList
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

    var allNotesToUnlock: Boolean = false
    var notesToUnlock: ArrayList<Note> = ArrayList()
    var notesToUnlockPositions: ArrayList<Int> = ArrayList()


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
            activity.window.statusBarColor = resources.getColor(R.color.app_toolbar)
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

        chipFilterHandler()

    }

    private fun chipFilterHandler() {
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            Log.e("current filters", checkedIds.toString())
            if (checkedIds.contains(R.id.locked_filter) && checkedIds.contains(R.id.unlocked_filter) && checkedIds.contains(
                    R.id.alarm_filter
                ) && checkedIds.contains(R.id.favorite_filter)
            ) {
                // alarm , lock , unlock and favorite filter
                Log.e(
                    "alarm , lock , unlock and favorite filter",
                    "alarm , lock , unlock and favorite filter"
                )
                binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.chip_select
                    )
                )
                binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.chip_select
                    )
                )
                binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.chip_select
                    )
                )
                binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.chip_select
                    )
                )
                viewModel.getNotesWithAlarmAndFavoriteFilter()
                    .observe(viewLifecycleOwner) { notesList ->
                        notesAdapter.submitList(notesList)
                    }

            } else {
                if (checkedIds.contains(R.id.locked_filter) && checkedIds.contains(R.id.unlocked_filter) && checkedIds.contains(
                        R.id.alarm_filter
                    )
                ) {
                    // alarm , lock , unlock filter
                    Log.e("alarm , lock , unlock filter", "alarm , lock , unlock filter")
                    binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.chip_select
                        )
                    )
                    binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.chip_select
                        )
                    )
                    binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.chip_select
                        )
                    )
                    binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.app_grey
                        )
                    )
                    viewModel.getNotesWithAlarmFilter()
                        .observe(viewLifecycleOwner) { notesList ->
                            notesAdapter.submitList(notesList)
                        }
                } else if (checkedIds.contains(R.id.locked_filter) && checkedIds.contains(R.id.unlocked_filter) && checkedIds.contains(
                        R.id.favorite_filter
                    )
                ) {
                    // unlock , lock , favorite filter
                    Log.e("unlock , lock , favorite filter", "unlock , lock , favorite filter")
                    binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.app_grey
                        )
                    )
                    binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.chip_select
                        )
                    )
                    binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.chip_select
                        )
                    )
                    binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.chip_select
                        )
                    )
                    viewModel.getNotesWithFavoriteFilter()
                        .observe(viewLifecycleOwner) { notesList ->
                            notesAdapter.submitList(notesList)
                        }

                } else if (checkedIds.contains(R.id.locked_filter) && checkedIds.contains(R.id.alarm_filter) && checkedIds.contains(
                        R.id.favorite_filter
                    )
                ) {
                    // alarm , lock , favorite filter
                    Log.e("alarm , lock , favorite filter", "alarm , lock , favorite filter")
                    binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.chip_select
                        )
                    )
                    binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.chip_select
                        )
                    )
                    binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.app_grey
                        )
                    )
                    binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.chip_select
                        )
                    )
                    viewModel.getNotesWithAlarmAndLockAndFavoriteFilter()
                        .observe(viewLifecycleOwner) { notesList ->
                            notesAdapter.submitList(notesList)
                        }
                } else if (checkedIds.contains(R.id.unlocked_filter) && checkedIds.contains(R.id.alarm_filter) && checkedIds.contains(
                        R.id.favorite_filter
                    )
                ) {
                    // alarm , unlock , favorite filter
                    Log.e("alarm , unlock , favorite filter", "alarm , unlock , favorite filter")
                    binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.chip_select
                        )
                    )
                    binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.app_grey
                        )
                    )
                    binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.chip_select
                        )
                    )
                    binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.chip_select
                        )
                    )
                    viewModel.getNotesWithAlarmAndUnlockAndFavoriteFilter()
                        .observe(viewLifecycleOwner) { notesList ->
                            notesAdapter.submitList(notesList)
                        }
                } else {
                    if (checkedIds.contains(R.id.locked_filter) && checkedIds.contains(R.id.unlocked_filter)) {
                        // lock , unlock filter
                        Log.e("lock , unlock filter", "lock , unlock filter")
                        binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.app_grey
                            )
                        )
                        binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.chip_select
                            )
                        )
                        binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.chip_select
                            )
                        )
                        binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.app_grey
                            )
                        )
                        viewModel.getAllNotes()
                            .observe(viewLifecycleOwner) { notesList ->
                                notesAdapter.submitList(notesList)
                            }
                    } else if (checkedIds.contains(R.id.locked_filter) && checkedIds.contains(R.id.alarm_filter)) {
                        // lock , unlock filter
                        Log.e("lock , alarm filter", "lock , alarm filter")
                        binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.chip_select
                            )
                        )
                        binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.chip_select
                            )
                        )
                        binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.app_grey
                            )
                        )
                        binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.app_grey
                            )
                        )
                        viewModel.getNotesWithAlarmAndLockFilter()
                            .observe(viewLifecycleOwner) { notesList ->
                                notesAdapter.submitList(notesList)
                            }
                    } else if (checkedIds.contains(R.id.locked_filter) && checkedIds.contains(R.id.favorite_filter)) {
                        // lock , favorite filter
                        Log.e("lock , favorite filter", "lock , favorite filter")
                        binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.app_grey
                            )
                        )
                        binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.chip_select
                            )
                        )
                        binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.app_grey
                            )
                        )
                        binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.chip_select
                            )
                        )
                        viewModel.getNotesWithLockAndFavoriteFilter()
                            .observe(viewLifecycleOwner) { notesList ->
                                notesAdapter.submitList(notesList)
                            }
                    } else if (checkedIds.contains(R.id.unlocked_filter) && checkedIds.contains(R.id.alarm_filter)) {
                        // unlock , alarm filter
                        Log.e("unlock , alarm filter", "unlock , alarm filter")
                        binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.chip_select
                            )
                        )
                        binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.app_grey
                            )
                        )
                        binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.chip_select
                            )
                        )
                        binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.app_grey
                            )
                        )
                        viewModel.getNotesWithAlarmAndUnLockFilter()
                            .observe(viewLifecycleOwner) { notesList ->
                                notesAdapter.submitList(notesList)
                            }
                    } else if (checkedIds.contains(R.id.unlocked_filter) && checkedIds.contains(R.id.favorite_filter)) {
                        // unlock , favorite filter
                        Log.e("unlock , favorite filter", "unlock , favorite filter")
                        binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.app_grey
                            )
                        )
                        binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.app_grey
                            )
                        )
                        binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.chip_select
                            )
                        )
                        binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.chip_select
                            )
                        )
                        viewModel.getNotesWithUnlockAndFavoriteFilter()
                            .observe(viewLifecycleOwner) { notesList ->
                                notesAdapter.submitList(notesList)
                            }
                    } else if (checkedIds.contains(R.id.alarm_filter) && checkedIds.contains(R.id.favorite_filter)) {
                        // alarm , favorite filter
                        Log.e("alarm , favorite filter", "alarm , favorite filter")
                        binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.chip_select
                            )
                        )
                        binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.app_grey
                            )
                        )
                        binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.app_grey
                            )
                        )
                        binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.chip_select
                            )
                        )
                        viewModel.getNotesWithAlarmAndFavoriteFilter()
                            .observe(viewLifecycleOwner) { notesList ->
                                notesAdapter.submitList(notesList)
                            }
                    } else {
                        if (checkedIds.contains(R.id.locked_filter)) {
                            // lock filter
                            Log.e("lock filter", "lock filter")
                            binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.chip_select
                                )
                            )
                            binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            viewModel.getNotesWithLockFilter()
                                .observe(viewLifecycleOwner) { notesList ->
                                    notesAdapter.submitList(notesList)
                                }
                        } else if (checkedIds.contains(R.id.unlocked_filter)) {
                            // unlock filter
                            Log.e("unlock filter", "unlock filter")
                            binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.chip_select
                                )
                            )
                            binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            viewModel.getNotesWithUnlockFilter()
                                .observe(viewLifecycleOwner) { notesList ->
                                    notesAdapter.submitList(notesList)
                                }
                        } else if (checkedIds.contains(R.id.favorite_filter)) {
                            // favorite filter
                            Log.e("favorite filter", "favorite filter")
                            binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.chip_select
                                )
                            )
                            viewModel.getNotesWithFavoriteFilter()
                                .observe(viewLifecycleOwner) { notesList ->
                                    notesAdapter.submitList(notesList)
                                }
                        } else if (checkedIds.contains(R.id.alarm_filter)) {
                            // alarm filter
                            Log.e("alarm filter", "alarm filter")
                            binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.chip_select
                                )
                            )
                            binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            viewModel.getNotesWithAlarmFilter()
                                .observe(viewLifecycleOwner) { notesList ->
                                    notesAdapter.submitList(notesList)
                                }
                        } else {
                            // no filter
                            Log.e("no filter", "no filter")
                            binding.alarmFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            binding.lockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            binding.unlockedFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            binding.favoriteFilter.chipBackgroundColor = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.app_grey
                                )
                            )
                            viewModel.getAllNotes()
                                .observe(viewLifecycleOwner) { notesList ->
                                    notesAdapter.submitList(notesList)
                                }
                        }
                    }
                }
            }

        }
    }

    private fun initAuthentication() {
        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(
            this@NoteHomeFragment,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.e("Authentication Failed", "Authentication Failed")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.e("Authentication result", result.toString())
                    Log.e("Authentication Success", "Authentication Success")

                    Log.e("notesToUnlock", notesToUnlock.toString())
                    Log.e("noteToUnlockPositions", notesToUnlockPositions.toString())
                    if (allNotesToUnlock) {
                        viewModel.updateAllNoteLockState(false)
                    } else {
                        for (note in notesToUnlock) {
                            note.is_locked = false
                            viewModel.updateNoteLockState(note.id, false)
                        }
                        for (pos in notesToUnlockPositions) {
                            notesAdapter.notifyItemChanged(pos)
                        }
                    }
                    notesToUnlock.clear()
                    notesToUnlockPositions.clear()
                    allNotesToUnlock = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e("Authentication ErrorCode", errorCode.toString())
                    Log.e("Authentication ErrorString", errString.toString())
                }
            })

        promptInfo = PromptInfo.Builder()
            .setTitle(getString(R.string.access_authentication))
            .setDescription(getString(R.string.scan_fingerprint))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL or BIOMETRIC_WEAK)
            .build()
    }

    private fun searchBarHandler() {
        binding.searchButton.setOnClickListener {
            binding.homePageTitle.visibility = View.GONE
            binding.closeSearchBarView.visibility = View.INVISIBLE
            binding.openSearchBarView.visibility = View.VISIBLE
            binding.searchBarEditText.visibility = View.VISIBLE
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
            binding.searchBarEditText.visibility = View.INVISIBLE
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
                    binding.searchBarEditText.clearFocus()
                    circularReveal.removeAllListeners()
                    requireView().hideKeyboard()
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
                var isUndoPressed = false
                viewModel.deleteNote(note)
                binding.searchBarEditText.apply {
                    hideKeyboard()
                    clearFocus()
                }
                if (binding.searchBarEditText.text.toString().isEmpty()) {
                    observeData()
                }
                val snackbar =
                    Snackbar.make(requireView(), R.string.note_deleted, Snackbar.LENGTH_LONG)
                        .addCallback(object :
                            BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                            }

                            override fun onShown(transientBottomBar: Snackbar?) {
                                transientBottomBar?.setAction(R.string.undo) {
                                    viewModel.insertNote(note)
                                    isUndoPressed = true
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
            // for update alarm icon on notes in homepage
            for(i in notesAdapter.currentList.indices){
                for(note in notesList){
                    if(notesAdapter.currentList[i].id == note.id) {
                        if (note.alarm_set != notesAdapter.currentList[i].alarm_set) {
                            notesAdapter.notifyItemChanged(i)
                        }
                        break
                    }
                }
            }
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
            notesAdapter = NotesAdapter(countNotesText, viewLifecycleOwner, adapterListener,context)
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

    override fun menuOnClick(
        notes: ArrayList<Note>,
        notesPositions: ArrayList<Int>,
        all_selected: Boolean,
        request: Int
    ) {
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
                    for (note in notes) {
                        note.is_locked = true
                        viewModel.updateNoteLockState(note.id, true)
                    }
                    for (pos in notesPositions) {
                        notesAdapter.notifyItemChanged(pos)
                    }
                }
            }
            3 -> {
                // unlock request
                allNotesToUnlock = all_selected
                notesToUnlock.clear()
                notesToUnlockPositions.clear()
                notesToUnlock.addAll(notes)
                notesToUnlockPositions.addAll(notesPositions)

                biometricPrompt.authenticate(promptInfo)
            }
        }
    }
}