package com.application.noteapp.fragments

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.text.*
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.application.noteapp.R
import com.application.noteapp.activities.MainActivity
import com.application.noteapp.databinding.FragmentAddOrUpdateNoteBinding
import com.application.noteapp.model.Note
import com.application.noteapp.receivers.NotificationReceiver
import com.application.noteapp.util.*
import com.application.noteapp.viewmodel.NoteViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import io.github.mthli.knife.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class AddOrUpdateNoteFragment : Fragment(R.layout.fragment_add_or_update_note) {
    lateinit var binding: FragmentAddOrUpdateNoteBinding
    lateinit var navigator: NavController
    private val viewModel: NoteViewModel by activityViewModels()
    private val currentDate: String = SimpleDateFormat.getInstance().format(Date())
    var note: Note? = null
    var color: Int = Color.parseColor("#eaf4f4")
    lateinit var result: String
    private val job = CoroutineScope(Dispatchers.Main)
    private val args: AddOrUpdateNoteFragmentArgs by navArgs()
    private var isColorPickerShowing: Boolean = false

    lateinit var alarmDate: String

    private val currentLang: String = getCurrentPhoneLanguage()

    val mapToIranianDays = mapOf(
        "Sun" to "یکشنبه",
        "Mon" to "دوشنبه",
        "Tue" to "سه شنبه",
        "Wed" to "چهارشنبه",
        "Thu" to "پنجشنبه",
        "Fri" to "جمعه",
        "Sat" to "شنبه"
    )

    val mapToIranianMonths = mapOf(
        "Jan" to "ژانویه",
        "Feb" to "فوریه",
        "Mar" to "مارس",
        "Apr" to "آوریل",
        "May" to "مه",
        "Jun" to "ژوئن",
        "Jul" to "ژوئیه",
        "Aug" to "اوت",
        "Sep" to "سپتامبر",
        "Oct" to "اکتبر",
        "Nov" to "نوامبر",
        "Dec" to "دسامبر",
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val anim = MaterialContainerTransform().apply {
            drawingViewId = R.id.mainFragment
            duration = 300
            scrimColor = Color.TRANSPARENT
        }

        sharedElementEnterTransition = anim
        sharedElementReturnTransition = anim

        requireActivity().onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    checkChanges()
                }
            })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddOrUpdateNoteBinding.bind(view)
        note = args.note
        activity?.window!!.statusBarColor = resources.getColor(R.color.app_background)

        if (currentLang == "fa") {
            binding.colorPickerLayout.translationX = -200f
            binding.backButton.rotation = 180f
        } else binding.colorPickerLayout.translationX = 200f

        setUpNotificationChannel()

        val activity = activity as MainActivity

        ViewCompat.setTransitionName(
            binding.addOrUpdateNoteFragmentParent,
            "recyclerView_${args.note?.id}"
        )

        navigator = Navigation.findNavController(view)

        binding.backButton.setOnClickListener {
            checkChanges()
        }

        try {
            binding.noteContentEditText.setOnFocusChangeListener { _, focused ->
                if (focused) {
                    binding.styleBar.visibility = View.VISIBLE
                } else {
                    binding.styleBar.visibility = View.GONE
                }
            }
        } catch (ex: Exception) {
            Log.d("ExceptionContent", ex.toString())
        }

        initNote()

        handleActionButtons()

        binding.closeColorPickerButton.setOnClickListener {
            if (currentLang == "fa") binding.colorPickerLayout.animate()
                .translationX(-200f).duration = 350
            else binding.colorPickerLayout.animate().translationX(200f).duration = 350
            isColorPickerShowing = false
        }

        binding.colorPicker.apply {
            setSelectedColor(color)
            setOnColorSelectedListener { selectedColor ->
                color = selectedColor
                binding.apply {
                    addOrUpdateNoteFragmentParent.setBackgroundColor(color)
                    FragmentAddOrUpdateToolbar.setBackgroundColor(color)
                    styleBar.setBackgroundColor(color)
                    activity.window.statusBarColor = color
                }
            }
        }

        binding.notificationButton.setOnClickListener {
            if (note != null) {
                if (!note!!.alarm_set) showDatePicker()
                else {
                    var convertedDate: String = note!!.alarm_date
                    if (currentLang == "fa") {
                        val temp = note!!.alarm_date.split(" ")
                        Log.e("temp", temp.toString())
                        convertedDate =
                            mapToIranianDays[temp[0]] + " " + FormatNumber.convertToPersian(
                                temp[2]
                            ) + " " + mapToIranianMonths[temp[1]] + " " + FormatNumber.convertToPersian(
                                temp[3]
                            ) + " " + "ساعت" + " " + FormatNumber.convertToPersian(
                                temp[4]
                            )
                    }
                    MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                        .setIcon(R.drawable.ic_warning)
                        .setTitle(R.string.warning)
                        .setMessage(getString(R.string.cancel_alarm, convertedDate))
                        .setPositiveButton(R.string.yes) { dialog, which ->
                            // cancel alarm
                            cancelAlarm()
                        }
                        .setNegativeButton(R.string.no) { dialog, which ->

                        }
                        .show()
                }
            } else {
                MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                    .setIcon(R.drawable.ic_warning)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.create_note_first)
                    .setPositiveButton(R.string.ok) { dialog, which ->

                    }
                    .show()
            }

        }

        binding.favoriteButton.setOnClickListener {
            if (note != null) {
                if (!note!!.is_favorite) {
                    binding.favoriteButton.setBackgroundResource(R.drawable.ic_favorite_enable)
                    binding.favoriteButton.backgroundTintList =
                        ColorStateList.valueOf(resources.getColor(R.color.app_yellow))
                    note!!.is_favorite = true
                    viewModel.updateNoteFavoriteState(note!!.id, true)
                } else {
                    binding.favoriteButton.setBackgroundResource(R.drawable.ic_favorite_disable)
                    binding.favoriteButton.backgroundTintList =
                        ColorStateList.valueOf(resources.getColor(R.color.app_yellow))
                    note!!.is_favorite = false
                    viewModel.updateNoteFavoriteState(note!!.id, false)
                }
            } else {
                MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                    .setIcon(R.drawable.ic_warning)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.create_note_first)
                    .setNeutralButton(R.string.ok) { dialog, which ->

                    }
                    .show()
            }
        }

        binding.noteContentEditText.setSelection(binding.noteContentEditText.editableText.length)
        initStyleBar()
        handleStyleBarWithSoftKeyBoard(view)

        // handle bullet selection
        binding.bullet.setOnClickListener {
            val editTextCursor = binding.noteContentEditText.selectionStart
            val currentText = binding.noteContentEditText.editableText
            if (currentText.contains('\n')) {
                val index = currentText.substring(0, editTextCursor).indexOfLast { char ->
                    char == '\n'
                }
                if (index == -1) {
                    if (binding.noteContentEditText.editableText[0] != '\u2022') {
                        binding.noteContentEditText.editableText.insert(0, "\u2022 ")
                    } else {
                        binding.noteContentEditText.setText(
                            binding.noteContentEditText.text.delete(
                                0,
                                2
                            ) as Spanned
                        )
                        setSelectionToEndOfLine(0)
                    }
                } else {
                    if (binding.noteContentEditText.editableText.length == index + 1) {
                        binding.noteContentEditText.editableText.append("\u2022 ")
                    } else {
                        if (binding.noteContentEditText.editableText[index + 1] != '\u2022') {
                            binding.noteContentEditText.editableText.insert(index + 1, "\u2022 ")
                        } else {
                            binding.noteContentEditText.setText(
                                binding.noteContentEditText.text.delete(
                                    index + 1,
                                    index + 3
                                ) as Spanned
                            )
                            setSelectionToEndOfLine(index + 1)
                        }
                    }

                }
            } else {
                if (binding.noteContentEditText.editableText.isEmpty()) {
                    binding.noteContentEditText.editableText.append("\u2022 ")
                } else {
                    if (binding.noteContentEditText.editableText[0] != '\u2022') {
                        binding.noteContentEditText.editableText.insert(0, "\u2022 ")
                    } else {
                        binding.noteContentEditText.setText(
                            binding.noteContentEditText.text.delete(
                                0,
                                2
                            ) as Spanned
                        )
                        setSelectionToEndOfLine(0)
                    }
                }
            }
        }

        binding.saveNoteButton.setOnClickListener {
            if (binding.noteTitleEditText.text.toString()
                    .isNotEmpty() && binding.noteContentEditText.text.toString()
                    .isNotEmpty()
            ) {
                if (binding.toolsFloatingActButtonLayout.toolsFab.rotation != 0f) hideActionButtons()
                saveNote()
            } else {
                // Your note is empty
                MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                    .setIcon(R.drawable.ic_warning)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.cant_add_empty_note)
                    .setNeutralButton(R.string.ok) { dialog, which ->
                    }
                    .show()
            }
        }
    }

    private fun setSelectionToEndOfLine(index: Int) {
        val end = binding.noteContentEditText.editableText.substring(
            index,
            binding.noteContentEditText.editableText.length
        ).indexOfFirst { char ->
            char == '\n'
        }
        if (end != 0 && end != -1) {
            binding.noteContentEditText.setSelection(end + index)
        } else {
            binding.noteContentEditText.setSelection(binding.noteContentEditText.text.length)
        }
    }

    private fun handleStyleBarWithSoftKeyBoard(view: View) {
        var isVisible = false
        fun onKeyboardVisibilityChanged(opened: Boolean) {
            Log.e("Keyboard", "keyboard $opened")
            if (opened) {
                if (binding.toolsFloatingActButtonLayout.toolsFab.rotation != 0f)
                    hideActionButtons()
            }
            if (opened && binding.noteContentEditText.hasFocus()) {
                binding.styleBar.visibility = View.VISIBLE
            } else {
                binding.styleBar.visibility = View.GONE
            }
        }

        view.viewTreeObserver?.addOnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            // 0.15 ratio is perhaps enough to determine keypad height
            if (keypadHeight > screenHeight * 0.15) {
                // keyboard is opened
                if (!isVisible) {
                    isVisible = true
                    onKeyboardVisibilityChanged(true)
                }
            } else {
                // keyboard is closed
                if (isVisible) {
                    isVisible = false
                    onKeyboardVisibilityChanged(false)
                }
            }
        }
    }

    private fun initStyleBar() {
        // Bold
        binding.bold.setOnClickListener {
            binding.noteContentEditText.bold(
                !binding.noteContentEditText.contains(
                    KnifeText.FORMAT_BOLD
                )
            )
        }
        binding.bold.setOnLongClickListener {
            true
        }

        // Italic
        binding.italic.setOnClickListener {
            binding.noteContentEditText.italic(
                !binding.noteContentEditText.contains(
                    KnifeText.FORMAT_ITALIC
                )
            )
        }
        binding.italic.setOnLongClickListener {
            true
        }

        // StrikeThrough
        binding.strikethrough.setOnClickListener {
            binding.noteContentEditText.strikethrough(
                !binding.noteContentEditText.contains(
                    KnifeText.FORMAT_STRIKETHROUGH
                )
            )
        }
        binding.strikethrough.setOnLongClickListener {
            true
        }
    }


    private fun checkChanges() {
        requireView().hideKeyboard()
        val newTitle = binding.noteTitleEditText.text.toString()
        val newContent = binding.noteContentEditText.text.toString()
        val newColor = color
        if (note != null) {
            val lastContent = SpannableStringBuilder()
            lastContent.append(
                HtmlCompat.fromHtml(
                    note!!.content,
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            )
            setUpBulletStyle(lastContent, lastContent.length)
            if (note!!.title != newTitle || lastContent.toString() != newContent || note!!.color.toString() != newColor.toString()) {
                MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                    .setIcon(R.drawable.ic_warning)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.unsaved_changes)
                    .setPositiveButton(R.string.yes) { dialog, which ->
                        navigator.popBackStack()
                    }
                    .setNegativeButton(R.string.no) { dialog, which ->

                    }
                    .show()
            } else {
                navigator.popBackStack()
            }
        } else {
            if (newTitle.isNotEmpty() || newContent.isNotEmpty()) {
                MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                    .setIcon(R.drawable.ic_warning)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.unsaved_changes)
                    .setPositiveButton(R.string.yes) { dialog, which ->
                        navigator.popBackStack()
                    }
                    .setNegativeButton(R.string.no) { dialog, which ->

                    }
                    .show()
            } else {
                navigator.popBackStack()
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
            DatePickerDialog(requireContext(), R.style.datePicker, { view, year, month, day ->
                selectedDay = day
                selectedYear = year
                selectedMonth = month
                currentHour = calendar.get(Calendar.HOUR)
                currentMinute = calendar.get(Calendar.MINUTE)
                val timePickerDialog = TimePickerDialog(
                    view.context, R.style.datePicker, { _, hour, minute ->
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
                val temp = if (currentTime == "PM" || currentTime == "بعدازظهر") 12 else 0
                timePickerDialog.updateTime(currentHour + temp, currentMinute)
                timePickerDialog.setButton(
                    TimePickerDialog.BUTTON_POSITIVE,
                    getString(R.string.done)
                ) { _, _ -> }
                timePickerDialog.show()

            }, currentYear, currentMonth, currentDay)
        datePickerDialog.show()
    }

    private fun isValid(selectedCalendar: Calendar): Boolean {
        val calendar: Calendar = Calendar.getInstance()
        if (selectedCalendar > calendar) {
            return true
        }
        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setIcon(R.drawable.ic_warning)
            .setTitle(R.string.warning)
            .setMessage(R.string.invalid_date)
            .setNeutralButton(R.string.ok) { dialog, which ->

            }
            .show()
        return false
    }

    private fun cancelAlarm() {
        note!!.alarm_set = false
        note!!.alarm_date = ""
        viewModel.updateAlarmState(note!!.id, false, "")
        binding.notificationButton.setBackgroundResource(R.drawable.ic_alarm_off)
        binding.notificationButton.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.app_red))
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            note!!.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)

        val temp: List<String> = cal.time.toString().split(" ")
        alarmDate =
            temp[0] + " " + temp[1] + " " + temp[2] + " " + temp[5] + " " + temp[3].substring(
                0,
                temp[3].lastIndexOf(":")
            )

        Log.e("alaram date", alarmDate)

        var convertedDate: String = alarmDate
        if (currentLang == "fa") {
            convertedDate = mapToIranianDays[temp[0]] + " " + FormatNumber.convertToPersian(
                temp[2]
            ) + " " + mapToIranianMonths[temp[1]] + " " + FormatNumber.convertToPersian(temp[5]) + " " + "ساعت" + " " + FormatNumber.convertToPersian(
                temp[3].substring(
                    0,
                    temp[3].lastIndexOf(":")
                )
            )
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setIcon(R.drawable.ic_alarm_on)
            .setTitle(R.string.successful)
            .setMessage(getString(R.string.set_alarm, convertedDate))
            .setNeutralButton(R.string.ok) { dialog, which ->
            }
            .show()

        binding.notificationButton.setBackgroundResource(R.drawable.ic_alarm_on)
        binding.notificationButton.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.app_red))
        note!!.alarm_set = true
        note!!.alarm_date = alarmDate
        viewModel.updateAlarmState(note!!.id, true, alarmDate)
    }

    private fun handleActionButtons() {
        binding.toolsFloatingActButtonLayout.apply {

            toolsFab.setOnClickListener {
                requireView().hideKeyboard()
                if (toolsFab.rotation == 0f) showActionButtons()
                else hideActionButtons()
            }

            deleteNoteFab.setOnClickListener {
                hideActionButtons()
                val note2 = args.note
                if (note2 != null) {
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.AlertDialogTheme
                    )
                        .setIcon(R.drawable.ic_warning)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.want_delete_note)
                        .setPositiveButton(R.string.yes) { dialog, which ->
                            viewModel.deleteNote(note2)
                            navigator.popBackStack()
                        }
                        .setNegativeButton(R.string.no) { dialog, which ->
                        }
                        .show()

                } else {
                    // Your note is empty
                    MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                        .setIcon(R.drawable.ic_warning)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.save_note_first)
                        .setNeutralButton(R.string.ok) { dialog, which ->
                        }
                        .show()
                }
            }

            colorPickerFab.setOnClickListener {
                // Show color picker layout
                hideActionButtons()
                if (!isColorPickerShowing) {
                    binding.colorPickerLayout.animate().translationX(0f).duration = 350
                    isColorPickerShowing = true
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
                    startActivity(Intent.createChooser(intent, getString(R.string.send_copy_to)))
                } else {
                    MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
                        .setIcon(R.drawable.ic_warning)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.cant_share_empty_note)
                        .setNeutralButton(R.string.ok) { dialog, which ->
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
            toolsFab.animate().rotationBy(-90f)
            deleteNoteFab.animate().translationY(-180f).translationX(-80f)
            colorPickerFab.animate().translationY(-180f).translationX(80f)
            sendCopyOfNoteFab.animate().translationY(-330f).translationX(0f)
        }
    }

    private fun hideActionButtons() {
        binding.toolsFloatingActButtonLayout.apply {
            toolsFab.animate().rotationBy(90f)
            colorPickerFab.animate().translationY(0f).translationX(0f)
            deleteNoteFab.animate().translationY(0f).translationX(0f)
            sendCopyOfNoteFab.animate().translationY(0f).translationX(0f)
        }
    }

    private fun initNote() {
        val note = args.note
        val title = binding.noteTitleEditText
        val content = binding.noteContentEditText
        val date = binding.noteEditedOnDate

        if (note != null) {
            if (note.alarm_set) {
                binding.notificationButton.setBackgroundResource(R.drawable.ic_alarm_on)
                binding.notificationButton.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.app_red))
            } else {
                binding.notificationButton.setBackgroundResource(R.drawable.ic_alarm_off)
                binding.notificationButton.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.app_red))
            }

            if (note.is_favorite) {
                binding.favoriteButton.setBackgroundResource(R.drawable.ic_favorite_enable)
                binding.favoriteButton.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.app_yellow))
            } else {
                binding.favoriteButton.setBackgroundResource(R.drawable.ic_favorite_disable)
                binding.favoriteButton.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.app_yellow))
            }
            title.setText(note.title)

            val builder = SpannableStringBuilder()
            builder.append(HtmlCompat.fromHtml(note.content, HtmlCompat.FROM_HTML_MODE_COMPACT))
            setUpBulletStyle(builder, builder.length)
            content.text = builder

            if (currentLang == "fa") date.text = FormatNumber.convertToPersian(note.date)
            else date.text = note.date

            color = note.color
            binding.apply {
                job.launch {
                    delay(10)
                    addOrUpdateNoteFragmentParent.setBackgroundColor(color)
                }
                FragmentAddOrUpdateToolbar.setBackgroundColor(color)
                styleBar.setBackgroundColor(color)
            }
            activity?.window?.statusBarColor = note.color
        } else {
            binding.notificationButton.setBackgroundResource(R.drawable.ic_alarm_off)
            binding.favoriteButton.setBackgroundResource(R.drawable.ic_favorite_disable)
            binding.noteEditedOnDate.text = currentDate
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
                        binding.noteContentEditText.getHtml(),
                        currentDate,
                        color,
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
                    binding.noteContentEditText.getHtml(),
                    currentDate,
                    color,
                    alarm_set = note!!.alarm_set,
                    alarm_date = note!!.alarm_date,
                    is_locked = note!!.is_locked,
                    is_favorite = note!!.is_favorite
                )
            )
        }
    }

    private fun KnifeText.getHtml(): String {
        return Parser.toHtml(editableText)
    }
}