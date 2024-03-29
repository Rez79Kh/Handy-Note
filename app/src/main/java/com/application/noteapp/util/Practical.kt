package com.application.noteapp.util

import android.app.KeyguardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.text.Editable
import android.text.Spanned
import android.text.style.BulletSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.os.ConfigurationCompat
import io.github.mthli.knife.KnifeBulletSpan

/*
Practical functions to use in whole app
 */
fun View.hideKeyboard() =
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
        windowToken,
        InputMethodManager.HIDE_NOT_ALWAYS
    )

fun getCurrentPhoneLanguage() =
    ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0).toString()
        .substringBefore("_")

fun deviceHasSecurity(context: Context): Boolean {
    return isPassSet(context)
}

fun isPassSet(context: Context): Boolean {
    val keyguardManager: KeyguardManager =
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return keyguardManager.isKeyguardSecure
}

fun setUpBulletStyle(editable: Editable, end: Int) {
    val bulletSpans = editable.getSpans(
        0, end,
        BulletSpan::class.java
    )
    for (span in bulletSpans) {
        val spanStart = editable.getSpanStart(span)
        var spanEnd = editable.getSpanEnd(span)
        spanEnd =
            if (0 < spanEnd && spanEnd < editable.length && editable[spanEnd] == '\n') spanEnd - 1 else spanEnd
        editable.removeSpan(span)
        editable.setSpan(
            KnifeBulletSpan(Color.BLACK, 6, 17),
            spanStart,
            spanEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
