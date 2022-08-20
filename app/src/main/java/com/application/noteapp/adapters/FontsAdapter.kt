package com.application.noteapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.application.noteapp.R
import com.application.noteapp.databinding.FontListItemBinding
import com.application.noteapp.model.Font
import com.google.android.material.textview.MaterialTextView

class FontsAdapter(private val fontsList: List<Font>) :RecyclerView.Adapter<FontsAdapter.FontsViewHolder>(){
    var onItemClick: ((Font) -> Unit)? = null
    inner class FontsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding:FontListItemBinding = FontListItemBinding.bind(itemView)
        val name:MaterialTextView = binding.fontName
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FontsViewHolder {
        return FontsViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.font_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: FontsViewHolder, position: Int) {
        val font = fontsList[position]
        holder.apply {
            name.text = font.name
            name.typeface = ResourcesCompat.getFont(itemView.context, font.id)
            itemView.setOnClickListener {
                onItemClick?.invoke(fontsList[adapterPosition])
            }
        }

    }

    override fun getItemCount(): Int {
        return fontsList.size
    }

}