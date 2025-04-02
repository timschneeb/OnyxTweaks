package me.timschneeberger.onyxtweaks.mods.utils

import android.annotation.SuppressLint
import android.text.SpannableStringBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.regex.Pattern

object StringFormatter {
    fun formatString(input: String): CharSequence {
        val result = SpannableStringBuilder(input)
        val pattern =
            Pattern.compile("\\$((T[a-zA-Z][0-9]*)|([A-Z][A-Za-z]+))")

        val matcher = pattern.matcher(input)
        while (matcher.find()) {
            val match = matcher.group(1)

            val start = result.toString().indexOf("$$match")
            result.replace(start, start + match!!.length + 1, valueOf(match))
        }
        return result
    }

    private fun valueOf(match: String): CharSequence {
        return when (match.substring(0, 1)) {
            "G" -> georgianDateOf(match.substring(1))
            else -> "$$match"
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun georgianDateOf(format: String) = try {
        SimpleDateFormat(format).format(Calendar.getInstance().getTime())
    } catch (_: Exception) {
        "\$G$format"
    }
}