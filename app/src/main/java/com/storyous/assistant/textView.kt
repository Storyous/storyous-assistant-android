package com.storyous.assistant

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import android.widget.TextView

fun SpannableStringBuilder.appendOrderedList(
    texts: Array<String>,
    firstHeader: Boolean = false
): SpannableStringBuilder {

    if (firstHeader && texts.isNotEmpty()) {
        var contentStart = length
        appendln(texts[0])
        setSpan(
            StyleSpan(Typeface.BOLD),
            contentStart,
            length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )

        contentStart = length
        appendln()
        setSpan(
            AbsoluteSizeSpan(10, true),
            contentStart,
            length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
    }

    texts.filterIndexed { index, _ -> !firstHeader || index > 0 }
        .forEachIndexed { index, s ->
            val contentStart = length
            appendln("${index + 1}. $s")
            setSpan(
                LeadingMarginSpan.Standard(0, 5),
                contentStart,
                length,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

    return this
}

fun TextView.setOrderedListText(
    texts: Array<String>,
    firstHeader: Boolean = false
) {
    text = SpannableStringBuilder().appendOrderedList(texts, firstHeader)
}
