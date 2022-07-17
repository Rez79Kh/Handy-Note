package com.application.noteapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.application.noteapp.R
import com.application.noteapp.activities.MainActivity
import com.application.noteapp.databinding.ToolsBottomSheetBinding
import com.application.noteapp.databinding.FragmentAddOrUpdateNoteBinding
import com.application.noteapp.model.Note
import com.application.noteapp.util.hideKeyboard
import com.application.noteapp.viewmodel.NoteViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddOrUpdateNoteFragment : Fragment(R.layout.fragment_add_or_update_note) {
    lateinit var binding: FragmentAddOrUpdateNoteBinding
    lateinit var navigator: NavController
    val viewModel: NoteViewModel by activityViewModels()
    val currentDate = SimpleDateFormat.getInstance().format(Date())
    var note: Note? = null
    var color: Int = -1
    lateinit var result: String
    val job = CoroutineScope(Dispatchers.Main)
    val args: AddOrUpdateNoteFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val anim = MaterialContainerTransform().apply {
            drawingViewId = R.id.mainFragment
            duration = 300
            scrimColor = Color.TRANSPARENT
        }

        sharedElementEnterTransition = anim
        sharedElementReturnTransition = anim

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddOrUpdateNoteBinding.bind(view)

        val activity = activity as MainActivity

        ViewCompat.setTransitionName(
            binding.addOrUpdateNoteFragmentParent,
            "recyclerView_${args.note?.id}"
        )

        navigator = Navigation.findNavController(view)

        binding.backButton.setOnClickListener {
            requireView().hideKeyboard()
            navigator.popBackStack()
        }

        binding.saveButton.setOnClickListener {
            saveNote()
        }

        try {
            binding.noteContentEditText.setOnFocusChangeListener { _, focused ->
                if (focused) {
                    requireView().hideKeyboard()
                    binding.markDownStyleBar.visibility = View.VISIBLE
                    binding.noteContentEditText.setStylesBar(binding.styleBar)
                } else binding.markDownStyleBar.visibility = View.GONE

            }
        } catch (ex: Exception) {
            Log.d("Exception", ex.toString())
        }

        binding.toolsFloatingActButton.setOnClickListener {
            val bottomsheetDialog =
                BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
            val bottomSheetView = layoutInflater.inflate(R.layout.tools_bottom_sheet, null)
            with(bottomsheetDialog) {
                setContentView(bottomSheetView)
                show()
            }
            val bottomSheetBinding = ToolsBottomSheetBinding.bind(bottomSheetView)

            bottomSheetBinding.apply {
                colorPicker.apply {
                    setSelectedColor(color)
                    setOnColorSelectedListener { selectedColor ->
                        color = selectedColor
                        binding.apply {
                            addOrUpdateNoteFragmentParent.setBackgroundColor(color)
                            FragmentAddOrUpdateToolbar.setBackgroundColor(color)
                            markDownStyleBar.setBackgroundColor(color)
                            activity.window.statusBarColor = color
                        }
                        bottomSheetBinding.bottomSheetCard.setCardBackgroundColor(color)
                    }
                }
                bottomSheetCard.setBackgroundColor(color)
            }
            bottomSheetView.post {
                bottomsheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        initNote()

    }

    private fun initNote(){
        val note = args.note
        val title = binding.noteTitleEditText
        val content = binding.noteContentEditText
        val date = binding.noteEditedOnDate

        if(note!=null){
            title.setText(note.title)
            content.renderMD(note.content)
            date.text = getString(R.string.edited_on,note.date)
            color = note.color
            binding.apply {
                job.launch {
                    delay(10)
                    addOrUpdateNoteFragmentParent.setBackgroundColor(color)
                }
                FragmentAddOrUpdateToolbar.setBackgroundColor(color)
                markDownStyleBar.setBackgroundColor(color)
            }
            activity?.window?.statusBarColor = note.color
        }
        else{
            binding.noteEditedOnDate.text = getString(R.string.edited_on, currentDate)
        }
    }

    fun saveNote() {
        if (!binding.noteTitleEditText.text.toString()
                .isEmpty() && !binding.noteContentEditText.text.toString().isEmpty()
        ) {
            note = args.note
            when (note) {
                null -> {
                    viewModel.insertNote(
                        Note(
                            0,
                            binding.noteTitleEditText.text.toString(),
                            binding.noteContentEditText.text.toString(),
                            currentDate, color
                        )
                    )
                    result = "Note Saved"
                    setFragmentResult("key", bundleOf("bundleKey" to result))

                    navigator.navigate(AddOrUpdateNoteFragmentDirections.actionAddOrUpdateNoteFragmentToNoteHomeFragment())
                }
                else -> {
                    updateNote()
                    navigator.popBackStack()
                }
            }
        }
    }

    private fun updateNote() {
        if (note != null) {
            viewModel.updateNote(
                Note(
                    note!!.id,
                    binding.noteTitleEditText.text.toString(),
                    binding.noteContentEditText.getMD(),
                    currentDate,
                    color
                )
            )
        }
    }
}