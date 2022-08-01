package com.application.noteapp.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.fonts.SystemFonts
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.application.noteapp.R
import com.application.noteapp.activities.MainActivity
import com.application.noteapp.adapters.FontsAdapter
import com.application.noteapp.adapters.NotesAdapter
import com.application.noteapp.databinding.FontsBottomSheetBinding
import com.application.noteapp.databinding.FragmentAddOrUpdateNoteBinding
import com.application.noteapp.model.Font
import com.application.noteapp.model.Note
import com.application.noteapp.util.getAvailableFonts
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
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AddOrUpdateNoteFragment : Fragment(R.layout.fragment_add_or_update_note) {
    lateinit var binding: FragmentAddOrUpdateNoteBinding
    lateinit var navigator: NavController
    lateinit var fontAdapter: FontsAdapter
    val viewModel: NoteViewModel by activityViewModels()
    val currentDate = SimpleDateFormat.getInstance().format(Date())
    var note: Note? = null
    var color: Int = Color.parseColor("#f7f7ff")
    lateinit var result: String
    val job = CoroutineScope(Dispatchers.Main)
    val args: AddOrUpdateNoteFragmentArgs by navArgs()
    var is_color_picker_showing: Boolean = false

    var selectedFontId:Int = R.font.roboto

    var fonts: ArrayList<Font> = ArrayList()

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

        if(note!=null) selectedFontId = note!!.fontId

        // Read Fonts file
        fonts = getAvailableFonts()

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

        initNote()

        handleActionButtons()

        binding.closeColorPickerButton.setOnClickListener {
            binding.colorPickerLayout.animate().translationX(200f).duration = 350
            is_color_picker_showing = false
        }

        binding.colorPicker.apply {
            setSelectedColor(color)
            setOnColorSelectedListener { selectedColor ->
                color = selectedColor
                binding.apply {
                    addOrUpdateNoteFragmentParent.setBackgroundColor(color)
                    FragmentAddOrUpdateToolbar.setBackgroundColor(color)
                    markDownStyleBar.setBackgroundColor(color)
                    activity.window.statusBarColor = color
                }
            }
        }


    }

    private fun handleActionButtons() {
        binding.toolsFloatingActButtonLayout.apply {
            toolsFab.animate().rotationBy(180f)

            toolsFab.setOnClickListener {
                if (toolsFab.rotation == 180f) showActionButtons()
                else hideActionButtons()
            }

            saveNoteFab.setOnClickListener {
                if (!binding.noteTitleEditText.text.toString()
                        .isEmpty() && !binding.noteContentEditText.text.toString().isEmpty()
                ) {
                    hideActionButtons()
                    saveNote()
                } else {
                    // Your note is empty
                }
            }

            deleteNoteFab.setOnClickListener {
                val note2 = args.note
                if (note2 != null) {
                    viewModel.deleteNote(note2)
                    hideActionButtons()
                    navigator.popBackStack()
                } else {
                    // Your note is empty
                }
            }

            changeFontFab.setOnClickListener {
                hideActionButtons()
                // show change font layout
                val bottomSheetDialog =
                    BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
                val bottomSheetView = layoutInflater.inflate(R.layout.fonts_bottom_sheet, null)
                with(bottomSheetDialog) {
                    setContentView(bottomSheetView)
                    show()
                }
                val bottomSheetBinding = FontsBottomSheetBinding.bind(bottomSheetView)

                bottomSheetBinding.apply {
                    fontList.layoutManager = LinearLayoutManager(context)
                    fontAdapter = FontsAdapter(fonts)
                    fontList.adapter = fontAdapter

                    fontAdapter.onItemClick = { font ->
                        val typeface = ResourcesCompat.getFont(bottomSheetView.context, font.id)
                        binding.noteTitleEditText.typeface = typeface
                        binding.noteContentEditText.typeface = typeface
                        selectedFontId = font.id
                        bottomSheetDialog.dismiss()
                    }
                    bottomSheetCard.setBackgroundColor(color)

                }
                bottomSheetView.post {
                    bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                }
            }

            colorPickerFab.setOnClickListener {
                // Show color picker layout
                hideActionButtons()
                if (!is_color_picker_showing) {
                    binding.colorPickerLayout.animate().translationX(0f).duration = 350
                    is_color_picker_showing = true
                }
            }

            sendCopyOfNoteFab.setOnClickListener {
                // Share Note
                if(binding.noteTitleEditText.text.toString().isNotEmpty() && binding.noteContentEditText.text.toString().isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    var text = binding.noteTitleEditText.text.toString() + "\n" + binding.noteContentEditText.text.toString()
                    intent.putExtra(Intent.EXTRA_TEXT, text)
                    startActivity(Intent.createChooser(intent,"Send A Copy To..."))
                }
                else{
                    // show message that your text is empty
                }
            }
        }
    }

    private fun showActionButtons() {
        binding.toolsFloatingActButtonLayout.apply {
            toolsFab.animate().rotationBy(-180f)
            saveNoteFab.animate().translationY(-180f).translationX(-80f)
            deleteNoteFab.animate().translationY(-180f).translationX(80f)
            changeFontFab.animate().translationY(-330f).translationX(-80f)
            colorPickerFab.animate().translationY(-330f).translationX(80f)
            sendCopyOfNoteFab.animate().translationY(-460f)
        }
    }

    private fun hideActionButtons() {
        binding.toolsFloatingActButtonLayout.apply {
            toolsFab.animate().rotationBy(180f)
            saveNoteFab.animate().translationY(0f).translationX(0f)
            deleteNoteFab.animate().translationY(0f).translationX(0f)
            changeFontFab.animate().translationY(0f).translationX(0f)
            colorPickerFab.animate().translationY(0f).translationX(0f)
            sendCopyOfNoteFab.animate().translationY(0f)
        }
    }

    private fun initNote() {
        val note = args.note
        val title = binding.noteTitleEditText
        val content = binding.noteContentEditText
        val date = binding.noteEditedOnDate

        if (note != null) {
            title.setText(note.title)
            content.renderMD(note.content)

            // set font
            val typeface = ResourcesCompat.getFont(requireView().context, note.fontId)
            title.typeface = typeface
            content.typeface = typeface

            date.text = getString(R.string.edited_on, note.date)
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
        } else {
            binding.noteEditedOnDate.text = getString(R.string.edited_on, currentDate)
        }
    }

    private fun saveNote() {
        Log.e("selectedFontId",selectedFontId.toString())
        note = args.note
        when (note) {
            null -> {
                viewModel.insertNote(
                    Note(
                        0,
                        binding.noteTitleEditText.text.toString(),
                        binding.noteContentEditText.text.toString(),
                        currentDate, color,selectedFontId
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

    private fun updateNote() {
        if (note != null) {
            viewModel.updateNote(
                Note(
                    note!!.id,
                    binding.noteTitleEditText.text.toString(),
                    binding.noteContentEditText.getMD(),
                    currentDate,
                    color,
                    note!!.fontId
                )
            )
        }
    }
}