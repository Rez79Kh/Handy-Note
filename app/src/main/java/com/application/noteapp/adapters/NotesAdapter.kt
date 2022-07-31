package com.application.noteapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
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

class NotesAdapter : ListAdapter<Note, NotesAdapter.NotesViewHolder>(DiffUtilCallback()) {

    inner class NotesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: NoteListItemBinding = NoteListItemBinding.bind(itemView)
        val title: MaterialTextView = binding.noteItemTitle
        val content: MaterialTextView = binding.noteItemContent
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        return NotesViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.note_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
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

                parent.setOnClickListener{
                    val action = NoteHomeFragmentDirections.actionNoteHomeFragmentToAddOrUpdateNoteFragment().setNote(note)
                    val extras = FragmentNavigatorExtras(parent to "recyclerView_${note.id}")
                    it.hideKeyboard()
                    Navigation.findNavController(it).navigate(action,extras)
                }

                content.setOnClickListener {
                    val action = NoteHomeFragmentDirections.actionNoteHomeFragmentToAddOrUpdateNoteFragment().setNote(note)
                    val extras = FragmentNavigatorExtras(parent to "recyclerView_${note.id}")
                    it.hideKeyboard()
                    Navigation.findNavController(it).navigate(action,extras)
                }

            }

        }
    }

}