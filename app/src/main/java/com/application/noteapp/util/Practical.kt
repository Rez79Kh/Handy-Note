package com.application.noteapp.util

import android.app.KeyguardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.provider.Settings
import android.text.Editable
import android.text.Spanned
import android.text.style.BulletSpan
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.application.noteapp.R
import com.application.noteapp.fragments.AddOrUpdateNoteFragment
import com.application.noteapp.model.Font
import io.github.mthli.knife.KnifeBulletSpan

/*
Practical functions to use in whole app
 */
fun View.hideKeyboard() =
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
        windowToken,
        InputMethodManager.HIDE_NOT_ALWAYS
    )

fun getAvailableFonts(context: Context): ArrayList<Font> {
    val fontFields = R.font::class.java.fields
    val fonts: ArrayList<Font> = ArrayList()
    var index = 0
    for (font in fontFields) {
        if (!font.toString().contains("bold") && !font.toString()
                .contains("italic") && !font.toString().contains("regular")
        ) {
            Log.e(
                font.toString().substring(font.toString().lastIndexOf(".") + 1),
                font.toString().substring(font.toString().lastIndexOf(".") + 1)
            )
            index++
            val num: String =
                if (getCurrentPhoneLanguage() == "fa") FormatNumber.convertToPersian(index.toString())
                else index.toString()
            fonts.add(
                Font(
                    context.resources.getString(R.string.font, num),
                    font.getInt(null)
                )
            )
        }
    }
    return fonts
}

fun getCurrentPhoneLanguage() =
    ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0).toString()
        .substringBefore("_")

fun deviceHasSecurity(context: Context): Boolean {
    return hasPassOrPin(context) || hasPattern(context)
}

fun hasPassOrPin(context: Context): Boolean {
    val keyguardManager: KeyguardManager =
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return keyguardManager.isKeyguardSecure
}

fun hasPattern(context: Context): Boolean {
    val contentResolver = context.contentResolver

    return try {
        val lockPatternEnable =
            Settings.Secure.getInt(contentResolver, Settings.Secure.LOCK_PATTERN_ENABLED)
        lockPatternEnable == 1;
    } catch (ex: Settings.SettingNotFoundException) {
        Log.e("Settings.SettingNotFoundException", ex.message.toString())
        false
    }
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
