package com.application.noteapp.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/*
Extension function to hide the keyboard
 */
fun View.hideKeyboard() =
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
        windowToken,
        InputMethodManager.HIDE_NOT_ALWAYS
    );