package com.application.noteapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.application.noteapp.R
import com.application.noteapp.activities.MainActivity
import com.application.noteapp.databinding.FragmentNoteHomeBinding
import com.application.noteapp.util.hideKeyboard
import com.google.android.material.transition.MaterialElevationScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteHomeFragment : Fragment(R.layout.fragment_note_home) {
    lateinit var binding: FragmentNoteHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialElevationScale(false).apply { duration = 300 }
        enterTransition = MaterialElevationScale(true).apply { duration = 300 }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNoteHomeBinding.bind(view)
        val navigator = Navigation.findNavController(view)
        var activity = activity as MainActivity
        requireView().hideKeyboard()

        CoroutineScope(Dispatchers.Main).launch {
            activity.window.statusBarColor = Color.WHITE
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            activity.window.statusBarColor = Color.parseColor("#9E9D9D")
        }

        binding.addNoteButton.setOnClickListener {
            binding.appBar.visibility = View.INVISIBLE
            navigator.navigate(NoteHomeFragmentDirections.actionNoteHomeFragmentToAddOrUpdateNoteFragment())
        }

        binding.addNoteFloatingActButton.setOnClickListener {
            binding.appBar.visibility = View.INVISIBLE
            navigator.navigate(NoteHomeFragmentDirections.actionNoteHomeFragmentToAddOrUpdateNoteFragment())
        }

    }
}