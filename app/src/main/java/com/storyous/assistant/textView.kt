package com.storyous.assistant

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import android.widget.TextView

private const val HEADER_OFFSET = 10
private const val LIST_ITEM_OFFSET = 5

fun SpannableStringBuilder.appendOrderedList(
    texts: Array<String>,
    firstItemIsHeader: Boolean = false
): SpannableStringBuilder {

    if (firstItemIsHeader && texts.isNotEmpty()) {
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
            AbsoluteSizeSpan(HEADER_OFFSET, true),
            contentStart,
            length,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
    }

    texts.filterIndexed { index, _ -> !firstItemIsHeader || index > 0 }
        .forEachIndexed { index, s ->
            val contentStart = length
            appendln("${index + 1}. $s")
            setSpan(
                LeadingMarginSpan.Standard(0, LIST_ITEM_OFFSET),
                contentStart,
                length,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

    return this
}

fun TextView.setOrderedListText(
    texts: Array<String>,
    firstItemIsHeader: Boolean = false
) {
    text = SpannableStringBuilder().appendOrderedList(texts, firstItemIsHeader)
}
