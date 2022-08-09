package com.application.noteapp.fragments

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.View
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
import com.application.noteapp.R
import com.application.noteapp.activities.MainActivity
import com.application.noteapp.adapters.FontsAdapter
import com.application.noteapp.databinding.FontsBottomSheetBinding
import com.application.noteapp.databinding.FragmentAddOrUpdateNoteBinding
import com.application.noteapp.model.Font
import com.application.noteapp.model.Note
import com.application.noteapp.receivers.NotificationReceiver
import com.application.noteapp.util.getAvailableFonts
import com.application.noteapp.util.hideKeyboard
import com.application.noteapp.viewmodel.NoteViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    lateinit var fontAdapter: FontsAdapter
    val viewModel: NoteViewModel by activityViewModels()
    val currentDate = SimpleDateFormat.getInstance().format(Date())
    var note: Note? = null
    var color: Int = Color.parseColor("#f7f7ff")
    lateinit var result: String
    val job = CoroutineScope(Dispatchers.Main)
    val args: AddOrUpdateNoteFragmentArgs by navArgs()
    var is_color_picker_showing: Boolean = false

    var selectedFontId: Int = R.font.roboto

    var fonts: ArrayList<Font> = ArrayList()

    lateinit var alarmDate: String

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
        note = args.note

        setUpNotificationChannel()

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

        binding.notificationButton.setOnClickListener {
            if (note != null) {
                if (!note!!.alarm_set) showDatePicker()
                else {
                    MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                        .setIcon(R.drawable.warning)
                        .setTitle("Warning")
                        .setMessage("Alarm already set on ${note!!.alarm_date} , Do you want to cancel it?")
                        .setPositiveButton("YES") { dialog, which ->
                            // cancel alarm
                            note!!.alarm_set = false
                            note!!.alarm_date = ""
                            viewModel.updateAlarmState(note!!.id, false, "")
                            cancelAlarm()
                        }
                        .setNegativeButton("NO") { dialog, which ->

                        }
                        .show()
                }
            } else {
                MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                    .setIcon(R.drawable.warning)
                    .setTitle("Attention")
                    .setMessage("You should create a note at first.")
                    .setPositiveButton("OK") { dialog, which ->

                    }
                    .show()
            }

        }

        binding.favoriteButton.setOnClickListener {
            if (note != null) {
                if (!note!!.is_favorite) {
                    binding.favoriteButton.setBackgroundResource(R.drawable.favorite_enable)
                    note!!.is_favorite = true
                    viewModel.updateNoteFavoriteState(note!!.id, true)
                } else {
                    binding.favoriteButton.setBackgroundResource(R.drawable.favorite_disable)
                    note!!.is_favorite = false
                    viewModel.updateNoteFavoriteState(note!!.id, false)
                }
            } else {
                MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                    .setIcon(R.drawable.warning)
                    .setTitle("Attention")
                    .setMessage("You should create a note at first.")
                    .setNeutralButton("OK") { dialog, which ->

                    }
                    .show()
            }
        }


    }

    private fun showDatePicker() {
        val currentDay: Int
        val currentMonth: Int
        val currentYear: Int
        var currentHour: Int
        var currentMinute: Int
        var selectedDay: Int
        var selectedMonth: Int
        var selectedYear: Int
        var selectedHour: Int
        var selectedMinute: Int

        val calendar: Calendar = Calendar.getInstance()
        currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        currentMonth = calendar.get(Calendar.MONTH)
        currentYear = calendar.get(Calendar.YEAR)
        val datePickerDialog =
            DatePickerDialog(requireContext(), { view, year, month, day ->
                selectedDay = day
                selectedYear = year
                selectedMonth = month
                currentHour = calendar.get(Calendar.HOUR)
                currentMinute = calendar.get(Calendar.MINUTE)
                val timePickerDialog = TimePickerDialog(
                    view.context, { _, hour, minute ->
                        selectedHour = hour
                        selectedMinute = minute
                        val selectedCalendar: Calendar = Calendar.getInstance()
                        selectedCalendar.set(
                            selectedYear,
                            selectedMonth,
                            selectedDay,
                            selectedHour,
                            selectedMinute
                        )
                        if (isValid(selectedCalendar))
                            setAlarm(selectedCalendar)
                    }, currentHour, currentMinute,
                    DateFormat.is24HourFormat(view.context)
                )
                val simpleDateFormat = SimpleDateFormat("a")
                val currentTime: String = simpleDateFormat.format(calendar.time)
                val temp = if (currentTime == "PM") 12 else 0
                timePickerDialog.updateTime(currentHour + temp, currentMinute)
                timePickerDialog.setButton(TimePickerDialog.BUTTON_POSITIVE, "Done") { _, _ -> }
                timePickerDialog.show()

            }, currentYear, currentMonth, currentDay)
//        datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "Done") { _, _ -> }
        datePickerDialog.show()
    }

    private fun isValid(selectedCalendar: Calendar): Boolean {
        val calendar: Calendar = Calendar.getInstance()
        if(selectedCalendar>calendar) {
            return true
        }
        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setIcon(R.drawable.bell)
            .setTitle("Warning")
            .setMessage("Selected Date and Time is invalid.")
            .setNeutralButton("OK") { dialog, which ->

            }
            .show()
        return false
    }

    private fun cancelAlarm() {
        binding.notificationButton.setBackgroundResource(R.drawable.alarm_disable)
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            note!!.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent);
    }

    private fun setAlarm(cal: Calendar) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), NotificationReceiver::class.java)
        val title = binding.noteTitleEditText.text.toString()
        val content = binding.noteContentEditText.text.toString()
        intent.putExtra("title", title)
        intent.putExtra("content", content)
        intent.putExtra("note_id", note!!.id)

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            note!!.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)

        val temp: List<String> = cal.time.toString().split(" ")
        alarmDate = temp[0] + " " + temp[1] + " " + temp[2] + " " + temp[3] + " " + temp[5]

        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setIcon(R.drawable.bell)
            .setTitle("Successful")
            .setMessage("Alarm successfully set on $alarmDate .")
            .setNeutralButton("OK") { dialog, which ->
            }
            .show()

        binding.notificationButton.setBackgroundResource(R.drawable.alarm_enable)
        note!!.alarm_set = true
        note!!.alarm_date = alarmDate
        viewModel.updateAlarmState(note!!.id, true, alarmDate)
    }

    private fun handleActionButtons() {
        binding.toolsFloatingActButtonLayout.apply {
            toolsFab.animate().rotationBy(180f)

            toolsFab.setOnClickListener {
                if (toolsFab.rotation == 180f) showActionButtons()
                else hideActionButtons()
            }

            saveNoteFab.setOnClickListener {
                if (binding.noteTitleEditText.text.toString()
                        .isNotEmpty() && binding.noteContentEditText.text.toString()
                        .isNotEmpty()
                ) {
                    hideActionButtons()
                    saveNote()
                } else {
                    // Your note is empty
                    MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                        .setIcon(R.drawable.bell)
                        .setTitle("Warning")
                        .setMessage("Can't add note with empty content or title.")
                        .setNeutralButton("OK") { dialog, which ->
                        }
                        .show()
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
                    MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                        .setIcon(R.drawable.bell)
                        .setTitle("Warning")
                        .setMessage("You should save the note first.")
                        .setNeutralButton("OK") { dialog, which ->
                        }
                        .show()
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
                if (binding.noteTitleEditText.text.toString()
                        .isNotEmpty() && binding.noteContentEditText.text.toString().isNotEmpty()
                ) {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    val text =
                        binding.noteTitleEditText.text.toString() + "\n" + binding.noteContentEditText.text.toString()
                    intent.putExtra(Intent.EXTRA_TEXT, text)
                    startActivity(Intent.createChooser(intent, "Send A Copy To..."))
                } else {
                    MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                        .setIcon(R.drawable.bell)
                        .setTitle("Warning")
                        .setMessage("Can't share a note with empty content or title.")
                        .setNeutralButton("OK") { dialog, which ->
                        }
                        .show()
                }
            }
        }
    }

    private fun setUpNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "note_app_reminder_channel"
            val desc = "channel for note app"
            val priority = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("note_app", name, priority)
            channel.description = desc
            val notificationManager =
                requireContext().getSystemService(NotificationManager::class.java)

            notificationManager!!.createNotificationChannel(channel)
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
            if (note.alarm_set)
                binding.notificationButton.setBackgroundResource(R.drawable.alarm_enable)
            else binding.notificationButton.setBackgroundResource(R.drawable.alarm_disable)

            if (note.is_favorite)
                binding.favoriteButton.setBackgroundResource(R.drawable.favorite_enable)
            else binding.favoriteButton.setBackgroundResource(R.drawable.favorite_disable)
            title.setText(note.title)

            // set font
            val typeface = ResourcesCompat.getFont(requireView().context, note.fontId)
            title.typeface = typeface
            content.typeface = typeface
            content.renderMD(note.content)

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
            binding.notificationButton.setBackgroundResource(R.drawable.alarm_disable)
            binding.favoriteButton.setBackgroundResource(R.drawable.favorite_disable)
            binding.noteEditedOnDate.text = getString(R.string.edited_on, currentDate)
        }
    }

    private fun saveNote() {
        note = args.note
        when (note) {
            null -> {
                viewModel.insertNote(
                    Note(
                        0,
                        binding.noteTitleEditText.text.toString(),
                        binding.noteContentEditText.text.toString(),
                        currentDate,
                        color,
                        selectedFontId,
                        alarm_set = false,
                        alarm_date = "",
                        is_locked = false,
                        is_favorite = false
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
                    note!!.fontId,
                    note!!.alarm_set,
                    note!!.alarm_date,
                    note!!.is_locked,
                    note!!.is_favorite
                )
            )
        }
    }
}