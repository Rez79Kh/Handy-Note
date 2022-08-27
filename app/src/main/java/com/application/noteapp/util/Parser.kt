package com.application.noteapp.util

import android.graphics.Typeface
import android.text.Spanned
import android.text.TextUtils
import android.text.style.*


object Parser {
    fun toHtml(text: Spanned): String {
        val out = StringBuilder()
        withHtml(out, text)
        return out.toString().replace("</ul>(<br>)?".toRegex(), "</ul>")
            .replace("</blockquote>(<br>)?".toRegex(), "</blockquote>")
    }

    private fun withHtml(out: StringBuilder, text: Spanned) {
        var next: Int
        var i = 0
        while (i < text.length) {
            next = text.nextSpanTransition(i, text.length, ParagraphStyle::class.java)
            withContent(out, text, i, next)
            i = next
        }
    }

    private fun withContent(out: StringBuilder, text: Spanned, start: Int, end: Int) {
        var next: Int
        var i = start
        while (i < end) {
            next = TextUtils.indexOf(text, '\n', i, end)
            if (next < 0) {
                next = end
            }
            var nl = 0
            while (next < end && text[next] == '\n') {
                next++
                nl++
            }
            withParagraph(out, text, i, next - nl, nl)
            i = next
        }
    }

    private fun withParagraph(out: StringBuilder, text: Spanned, start: Int, end: Int, nl: Int) {
        var next: Int
        run {
            var i = start
            while (i < end) {
                next = text.nextSpanTransition(i, end, CharacterStyle::class.java)
                val spans = text.getSpans(
                    i, next,
                    CharacterStyle::class.java
                )
                for (j in spans.indices) {
                    if (spans[j] is StyleSpan) {
                        val style = (spans[j] as StyleSpan).style
                        if (style and Typeface.BOLD != 0) {
                            out.append("<b>")
                        }
                        if (style and Typeface.ITALIC != 0) {
                            out.append("<i>")
                        }
                    }
                    if (spans[j] is StrikethroughSpan) {
                        out.append("<del>")
                    }
                }
                convertStyle(out, text, i, next)
                for (j in spans.indices.reversed()) {
                    if (spans[j] is StrikethroughSpan) {
                        out.append("</del>")
                    }
                    if (spans[j] is StyleSpan) {
                        val style = (spans[j] as StyleSpan).style
                        if (style and Typeface.BOLD != 0) {
                            out.append("</b>")
                        }
                        if (style and Typeface.ITALIC != 0) {
                            out.append("</i>")
                        }
                    }
                }
                i = next
            }
        }
        for (i in 0 until nl) {
            out.append("<br>")
        }
    }

    private fun convertStyle(out: StringBuilder, text: CharSequence, start: Int, end: Int) {
        var i = start
        while (i < end) {
            val c = text[i]
            if (c == '<') {
                out.append("&lt;")
            } else if (c == '>') {
                out.append("&gt;")
            } else if (c == '&') {
                out.append("&amp;")
            } else if (c.code in 0xD800..0xDFFF) {
                if (c.code < 0xDC00 && i + 1 < end) {
                    val d = text[i + 1]
                    if (d.code in 0xDC00..0xDFFF) {
                        i++
                        val codepoint = 0x010000 or (c.code - 0xD800 shl 10) or d.code - 0xDC00
                        out.append("&#").append(codepoint).append(";")
                    }
                }
            } else if (c.code > 0x7E || c < ' ') {
                out.append("&#").append(c.code).append(";")
            } else if (c == ' ') {
                while (i + 1 < end && text[i + 1] == ' ') {
                    out.append("&nbsp;")
                    i++
                }
                out.append(' ')
            } else {
                out.append(c)
            }
            i++
        }
    }
}