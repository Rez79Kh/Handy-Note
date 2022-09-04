package com.application.noteapp.util

class FormatNumber {
    companion object {
        private val persianNumbers = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
        fun convertToPersian(text: String): String {
            if (text.isEmpty()) return ""
            var res = ""
            for (char in text) {
                when (char) {
                    in '0'..'9' -> {
                        val number = Integer.parseInt(char.toString());
                        res += persianNumbers[number]
                    }
                    '٫' -> res += '،';
                    else -> res += char;
                }
            }
            return res
        }
    }
}