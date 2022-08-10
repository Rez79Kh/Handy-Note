package com.application.noteapp.util

import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.application.noteapp.R
import com.application.noteapp.model.Font

/*
Extension function to hide the keyboard
 */
fun View.hideKeyboard() =
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
        windowToken,
        InputMethodManager.HIDE_NOT_ALWAYS
    )

fun getAvailableFonts(): ArrayList<Font> {
    val fontFields = R.font::class.java.fields
    val fonts: ArrayList<Font> = ArrayList()
    for (font in fontFields) {
        if (!font.toString().contains("bold") && !font.toString().contains("italic") && !font.toString().contains("regular")) {
            fonts.add(
                Font(
                    font.toString().substring(font.toString().lastIndexOf(".") + 1),
                    font.getInt(null)
                )
            )
        }
    }
    return fonts
}

fun getCurrentPhoneLanguage() = ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0).toString()
    .substringBefore("_")