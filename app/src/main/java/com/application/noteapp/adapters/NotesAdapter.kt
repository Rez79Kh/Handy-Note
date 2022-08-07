package com.application.noteapp.adapters

import android.graphics.Color
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
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
import com.application.noteapp.util.DiffUtilCallback
import com.application.noteapp.util.hideKeyboard
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import org.commonmark.node.SoftLineBreak
import kotlin.collections.ArrayList

class NotesAdapter(
    private val countNotesText: MutableLiveData<String>,
    private val lifecycleOwner: LifecycleOwner,
    private var deleteButtonListener: EventListener? = null
) :
    ListAdapter<Note, NotesAdapter.NotesViewHolder>(DiffUtilCallback()) {
    val selectedNotes: ArrayList<Note> = ArrayList()
    var is_menu_visible: Boolean = false
    var is_all_selected: Boolean = false

    inner class NotesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: NoteListItemBinding = NoteListItemBinding.bind(itemView)
        val title: MaterialTextView = binding.noteItemTitle
        val content: MaterialTextView = binding.noteItemContent
        val noteCheck: ImageView = binding.checkNote
        val date: MaterialTextView = binding.noteItemDate
        val parent: MaterialCardView = binding.noteItemCard
        val markDown = Markwon.builder(itemView.context).usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(itemView.context))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                    super.configureVisitor(builder)
                    builder.on(SoftLineBreak::class.java) { visitor, _ ->
                        visitor.forceNewLine()

                    }
                }
            }).build()

    }

    interface EventListener {
        fun onEvent(notes: ArrayList<Note>, all_selected: Boolean)
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
                markDown.setMarkdown(content, note.content)
                date.text = note.date
                parent.setCardBackgroundColor(note.color)

                val typeface = ResourcesCompat.getFont(parent.context, note.fontId)
                title.typeface = typeface
                content.typeface = typeface

                parent.setOnClickListener {
                    if (is_menu_visible) {
                        clickItem(holder)
                    } else {
                        val action =
                            NoteHomeFragmentDirections.actionNoteHomeFragmentToAddOrUpdateNoteFragment()
                                .setNote(note)
                        val extras = FragmentNavigatorExtras(parent to "recyclerView_${note.id}")
                        it.hideKeyboard()
                        Navigation.findNavController(it).navigate(action, extras)
                    }
                }

                content.setOnClickListener {
                    if (is_menu_visible) {
                        clickItem(holder)
                    } else {
                        val action =
                            NoteHomeFragmentDirections.actionNoteHomeFragmentToAddOrUpdateNoteFragment()
                                .setNote(note)
                        val extras = FragmentNavigatorExtras(parent to "recyclerView_${note.id}")
                        it.hideKeyboard()
                        Navigation.findNavController(it).navigate(action, extras)
                    }
                }

                parent.setOnLongClickListener(object : View.OnLongClickListener {
                    override fun onLongClick(view: View?): Boolean {
                        if (!is_menu_visible) {
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
                                    is_menu_visible = true
                                    clickItem(holder)

                                    countNotesText.observe(binding.lifecycleOwner!!) { value ->
                                        actionMode!!.title = "Selected $value"
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
                                            is_menu_visible = false
                                            if (selectedNotes.size > 0) {
                                                deleteButtonListener?.onEvent(selectedNotes, is_all_selected)
                                                selectedNotes.clear()
                                            }
                                            actionMode?.finish()
                                        } // delete selected notes , if size == 0 show no note available
                                        R.id.menuSelectAllNote -> {
                                            if (selectedNotes.size == currentList.size) {
                                                is_all_selected = false
                                                selectedNotes.clear()
                                            } else {
                                                is_all_selected = true
                                                selectedNotes.clear()
                                                // add all
                                                for (el in currentList)
                                                    selectedNotes.add(el)
                                            }
                                            countNotesText.value = selectedNotes.size.toString()
                                            notifyDataSetChanged()
                                        }// select all note
                                    }
                                    return true
                                }

                                override fun onDestroyActionMode(actionMode: ActionMode?) {
                                    is_menu_visible = false
                                    is_all_selected = false
                                    selectedNotes.clear()
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

                })

                if (is_all_selected) {
                    noteCheck.visibility = View.VISIBLE
                    parent.setBackgroundColor(Color.YELLOW)
                } else {
                    noteCheck.visibility = View.GONE
                    parent.setBackgroundColor(Color.TRANSPARENT)
                }
            }

        }

    }

    private fun clickItem(holder: NotesAdapter.NotesViewHolder) {
        if (holder.noteCheck.visibility == View.GONE) {
            holder.noteCheck.visibility = View.VISIBLE
            holder.parent.setBackgroundColor(Color.YELLOW)
            selectedNotes.add(getItem(holder.position))
        } else {
            holder.noteCheck.visibility = View.GONE
            holder.parent.setBackgroundColor(Color.TRANSPARENT)
            selectedNotes.remove(getItem(holder.position))
        }

        countNotesText.value = selectedNotes.size.toString()
    }
}