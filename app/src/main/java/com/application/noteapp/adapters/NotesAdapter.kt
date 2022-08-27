package com.application.noteapp.adapters

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.htmlEncode
import androidx.core.text.parseAsHtml
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.application.noteapp.R
import com.application.noteapp.databinding.NoteListItemBinding
import com.application.noteapp.fragments.NoteHomeFragmentDirections
import com.application.noteapp.model.Note
import com.application.noteapp.util.*
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import io.github.mthli.knife.KnifeBulletSpan
import kotlin.collections.ArrayList

class NotesAdapter(
    private val countNotesText: MutableLiveData<String>,
    private val lifecycleOwner: LifecycleOwner,
    private var adapterListener: EventListener? = null,
    private val context: Context
) :
    ListAdapter<Note, NotesAdapter.NotesViewHolder>(DiffUtilCallback()) {
    val selectedNotes: ArrayList<Note> = ArrayList()
    val selectedNotePositions: ArrayList<Int> = ArrayList()
    var isMenuVisible: Boolean = false
    var isAllSelected: Boolean = false
    val currentLang = getCurrentPhoneLanguage()

    inner class NotesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: NoteListItemBinding = NoteListItemBinding.bind(itemView)
        val title: MaterialTextView = binding.noteItemTitle
        val content: MaterialTextView = binding.noteItemContent
        val noteCheck: ImageView = binding.checkNote
        val isFavorite: ImageView = binding.isFavorite
        val hasAlarm: ImageView = binding.hasAlarm
        val date: MaterialTextView = binding.noteItemDate
        val parent: MaterialCardView = binding.noteItemCard
        val lock: ImageView = binding.lockNoteIcon

    }

    interface EventListener {
        // request == 1 -> delete , request == 2 -> lock , request ==3 -> unlock
        fun menuOnClick(
            notes: ArrayList<Note>,
            notesPositions: ArrayList<Int>,
            all_selected: Boolean,
            request: Int
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        return NotesViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.note_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
        holder.binding.lifecycleOwner = lifecycleOwner
        getItem(position).let { note ->
            holder.apply {
                parent.transitionName = "recyclerView_${note.id}"
                title.text = note.title
                if (currentLang == "fa") date.text =
                    FormatNumber.convertToPersian(note.date)
                else date.text = note.date
                parent.setCardBackgroundColor(note.color)
                val typeface = ResourcesCompat.getFont(parent.context, note.fontId)
                title.typeface = typeface

                if (note.is_favorite) {
                    isFavorite.visibility = View.VISIBLE
                } else {
                    isFavorite.visibility = View.GONE
                }

                if (note.alarm_set) {
                    hasAlarm.visibility = View.VISIBLE
                } else {
                    hasAlarm.visibility = View.GONE
                }

                if (note.is_locked) {
                    content.visibility = View.INVISIBLE

                    val builder = SpannableStringBuilder()
                    builder.append(HtmlCompat.fromHtml(note.content, HtmlCompat.FROM_HTML_MODE_COMPACT))
                    setUpBulletStyle(builder, builder.length)
                    content.text = builder

                    content.typeface = typeface
                    lock.visibility = View.VISIBLE
                    parent.setOnClickListener {
                        if (isMenuVisible) {
                            clickItem(holder)
                        } else {
                            // clicks on a locked note -> unlock
                            MaterialAlertDialogBuilder(
                                itemView.context,
                                R.style.AlertDialogTheme
                            )
                                .setIcon(R.drawable.warning)
                                .setTitle(R.string.warning)
                                .setMessage(R.string.want_unlock_note)
                                .setPositiveButton(R.string.yes) { dialog, which ->
                                    isAllSelected = false
                                    selectedNotes.clear()
                                    selectedNotePositions.clear()
                                    selectedNotes.add(getItem(position))
                                    selectedNotePositions.add(position)
                                    adapterListener?.menuOnClick(
                                        selectedNotes,
                                        selectedNotePositions,
                                        isAllSelected,
                                        3
                                    )
                                    selectedNotes.clear()
                                    selectedNotePositions.clear()
                                }
                                .setNegativeButton(R.string.no) { dialog, which ->

                                }
                                .show()
                        }
                    }
                    content.setOnClickListener {
                        if (isMenuVisible) {
                            clickItem(holder)
                        } else {
                            // clicks on a locked note -> unlock
                            MaterialAlertDialogBuilder(
                                itemView.context,
                                R.style.AlertDialogTheme
                            )
                                .setIcon(R.drawable.warning)
                                .setTitle(R.string.warning)
                                .setMessage(R.string.want_unlock_note)
                                .setPositiveButton(R.string.yes) { dialog, which ->
                                    isAllSelected = false
                                    selectedNotes.clear()
                                    selectedNotePositions.clear()
                                    selectedNotes.add(getItem(position))
                                    selectedNotePositions.add(position)
                                    adapterListener?.menuOnClick(
                                        selectedNotes,
                                        selectedNotePositions,
                                        isAllSelected,
                                        3
                                    )
                                    selectedNotes.clear()
                                    selectedNotePositions.clear()
                                }
                                .setNegativeButton(R.string.no) { dialog, which ->

                                }
                                .show()
                        }
                    }
                } else {
                    // just if note is not locked -> show the content
                    content.visibility = View.VISIBLE
                    lock.visibility = View.GONE

                    val builder = SpannableStringBuilder()
                    builder.append(HtmlCompat.fromHtml(note.content, HtmlCompat.FROM_HTML_MODE_COMPACT))
                    setUpBulletStyle(builder, builder.length)
                    content.text = builder

                    content.typeface = typeface

                    parent.setOnClickListener {
                        if (isMenuVisible) {
                            clickItem(holder)
                        } else {
                            openNote(note, holder, it)
                        }
                    }

                    content.setOnClickListener {
                        if (isMenuVisible) {
                            clickItem(holder)
                        } else {
                            openNote(note, holder, it)
                        }
                    }
                }

                content.setOnLongClickListener(holdNoteHandler(holder))
                parent.setOnLongClickListener(holdNoteHandler(holder))

                if (isAllSelected) {
                    noteCheck.visibility = View.VISIBLE
                    parent.setCardBackgroundColor(context.resources.getColor(R.color.app_yellow))
                } else {
                    noteCheck.visibility = View.GONE
                    parent.setCardBackgroundColor(note.color)
                }
            }
        }

    }

    private fun isAllNotesLocked(selectedNotes: ArrayList<Note>): Boolean {
        for (note in selectedNotes) {
            if (!note.is_locked)
                return false
        }
        return true
    }

    private fun isAllNotesUnlocked(selectedNotes: ArrayList<Note>): Boolean {
        for (note in selectedNotes) {
            if (note.is_locked)
                return false
        }
        return true
    }

    private fun openNote(note: Note, holder: NotesAdapter.NotesViewHolder, view: View?) {
        val action =
            NoteHomeFragmentDirections.actionNoteHomeFragmentToAddOrUpdateNoteFragment()
                .setNote(note)
        val extras = FragmentNavigatorExtras(holder.parent to "recyclerView_${note.id}")
        view?.hideKeyboard()
        Navigation.findNavController(view!!).navigate(action, extras)
    }

    private fun clickItem(holder: NotesAdapter.NotesViewHolder) {
        if (holder.noteCheck.visibility == View.GONE) {
            holder.noteCheck.visibility = View.VISIBLE
            holder.parent.setCardBackgroundColor(context.resources.getColor(R.color.app_yellow))
            selectedNotes.add(getItem(holder.position))
            selectedNotePositions.add(holder.position)
        } else {
            holder.noteCheck.visibility = View.GONE
            holder.parent.setCardBackgroundColor(getItem(holder.position).color)
            selectedNotes.remove(getItem(holder.position))
            selectedNotePositions.remove(holder.position)
        }

        countNotesText.value = selectedNotes.size.toString()
    }

    private fun holdNoteHandler(holder: NotesAdapter.NotesViewHolder) =
        object : View.OnLongClickListener {
            override fun onLongClick(view: View?): Boolean {
                if (!isMenuVisible) {
                    val callback = object : ActionMode.Callback {
                        override fun onCreateActionMode(
                            actionMode: ActionMode?,
                            menu: Menu?
                        ): Boolean {
                            val menuInflater = actionMode?.menuInflater
                            menuInflater?.inflate(R.menu.select_menu, menu)
                            return true
                        }

                        override fun onPrepareActionMode(
                            actionMode: ActionMode?,
                            menu: Menu?
                        ): Boolean {
                            if (!isMenuVisible) {
                                isMenuVisible = true
                                clickItem(holder)
                                countNotesText.observe(holder.binding.lifecycleOwner!!) { value ->
                                    var newVal = value
                                    if(currentLang=="fa"){
                                        newVal = FormatNumber.convertToPersian(value)
                                    }
                                    actionMode!!.title = context.resources.getString(
                                        R.string.selected,
                                        newVal
                                    )
                                }
                            }
                            return true

                        }

                        override fun onActionItemClicked(
                            actionMode: ActionMode?,
                            menuItem: MenuItem?
                        ): Boolean {
                            val id: Int = menuItem!!.itemId
                            when (id) {
                                R.id.menuDeleteNote -> {
                                    if (selectedNotes.size > 0) {
                                        MaterialAlertDialogBuilder(
                                            view!!.context,
                                            R.style.AlertDialogTheme
                                        )
                                            .setIcon(R.drawable.warning)
                                            .setTitle(R.string.warning)
                                            .setMessage(R.string.want_delete_all_notes)
                                            .setPositiveButton(R.string.yes) { dialog, which ->
                                                isMenuVisible = false
                                                adapterListener?.menuOnClick(
                                                    selectedNotes,
                                                    selectedNotePositions,
                                                    isAllSelected,
                                                    1
                                                )
                                                selectedNotes.clear()
                                                selectedNotePositions.clear()
                                                actionMode?.finish()
                                            }
                                            .setNegativeButton(R.string.no) { dialog, which ->
                                                isMenuVisible = true
                                            }
                                            .show()
                                    }
                                } // delete selected notes , if size == 0 show no note available
                                R.id.menuSelectAllNote -> {
                                    if (selectedNotes.size == currentList.size) {
                                        isAllSelected = false
                                        selectedNotes.clear()
                                        selectedNotePositions.clear()
                                    } else {
                                        isAllSelected = true
                                        selectedNotes.clear()
                                        selectedNotePositions.clear()
                                        // add all
                                        for (el in currentList) {
                                            selectedNotes.add(el)
                                        }
                                        for (index in 0 until currentList.size) {
                                            selectedNotePositions.add(index)
                                        }
                                    }
                                    countNotesText.value = selectedNotes.size.toString()
                                    notifyDataSetChanged()
                                }// select all note

                                R.id.menuLockNote -> {
                                    if (deviceHasSecurity(context)) {
                                        if (selectedNotes.size > 0) {
                                            if (isAllNotesLocked(selectedNotes)) {
                                                MaterialAlertDialogBuilder(
                                                    view!!.context,
                                                    R.style.AlertDialogTheme
                                                )
                                                    .setIcon(R.drawable.warning)
                                                    .setTitle(R.string.warning)
                                                    .setMessage(R.string.all_locked)
                                                    .setNeutralButton(R.string.ok) { dialog, which ->
                                                        isMenuVisible = true

                                                    }
                                                    .show()
                                            } else {
                                                MaterialAlertDialogBuilder(
                                                    view!!.context,
                                                    R.style.AlertDialogTheme
                                                )
                                                    .setIcon(R.drawable.warning)
                                                    .setTitle(R.string.warning)
                                                    .setMessage(R.string.want_lock_notes)
                                                    .setPositiveButton(R.string.yes) { dialog, which ->
                                                        isMenuVisible = false
                                                        adapterListener?.menuOnClick(
                                                            selectedNotes,
                                                            selectedNotePositions,
                                                            isAllSelected,
                                                            2
                                                        )
                                                        selectedNotes.clear()
                                                        selectedNotePositions.clear()
                                                        actionMode?.finish()
                                                    }
                                                    .setNegativeButton(R.string.no) { dialog, which ->
                                                        isMenuVisible = true
                                                    }
                                                    .show()
                                            }

                                        }
                                    }
                                    else{
                                        MaterialAlertDialogBuilder(
                                            view!!.context,
                                            R.style.AlertDialogTheme
                                        )
                                            .setIcon(R.drawable.warning)
                                            .setTitle(R.string.warning)
                                            .setMessage(R.string.no_password_already_set)
                                            .setNeutralButton(R.string.ok) { dialog, which ->

                                            }
                                            .show()
                                    }

                                }// lock selected notes

                                R.id.menuUnlockNote -> {
                                    if (selectedNotes.size > 0) {
                                        if (isAllNotesUnlocked(selectedNotes)) {
                                            MaterialAlertDialogBuilder(
                                                view!!.context,
                                                R.style.AlertDialogTheme
                                            )
                                                .setIcon(R.drawable.warning)
                                                .setTitle(R.string.warning)
                                                .setMessage(R.string.all_unlocked)
                                                .setNeutralButton(R.string.ok) { dialog, which ->
                                                    isMenuVisible = true
                                                }
                                                .show()
                                        } else {
                                            MaterialAlertDialogBuilder(
                                                view!!.context,
                                                R.style.AlertDialogTheme
                                            )
                                                .setIcon(R.drawable.warning)
                                                .setTitle(R.string.warning)
                                                .setMessage(R.string.want_unlock_selected_notes)
                                                .setPositiveButton(R.string.yes) { dialog, which ->
                                                    isMenuVisible = false
                                                    adapterListener?.menuOnClick(
                                                        selectedNotes,
                                                        selectedNotePositions,
                                                        isAllSelected,
                                                        3
                                                    )
                                                    selectedNotes.clear()
                                                    selectedNotePositions.clear()
                                                    actionMode?.finish()
                                                }
                                                .setNegativeButton(R.string.no) { dialog, which ->
                                                    isMenuVisible = true
                                                }
                                                .show()
                                        }

                                    }
                                }// unlock selected notes
                            }
                            return true
                        }

                        override fun onDestroyActionMode(actionMode: ActionMode?) {
                            isMenuVisible = false
                            isAllSelected = false
                            selectedNotes.clear()
                            selectedNotePositions.clear()
                            notifyDataSetChanged()
                        }

                    }
                    val c = view?.context as AppCompatActivity
                    c.startActionMode(callback)
                } else {
                    clickItem(holder)
                }
                return true
            }

        }
}