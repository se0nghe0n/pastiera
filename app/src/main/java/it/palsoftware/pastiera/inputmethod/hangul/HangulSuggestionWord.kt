package it.palsoftware.pastiera.inputmethod.hangul

import it.palsoftware.pastiera.core.Punctuation

/**
 * Current-word string used for Korean suggestions.
 *
 * Committed text before the cursor is the word so far. A composing Hangul
 * syllable has to be part of that word, but editors disagree about whether
 * [android.view.inputmethod.InputConnection.getTextBeforeCursor] already
 * includes the composing span. Standard editors include it, so appending
 * again turns "한국" into "한국국". Editors that omit the span still need
 * the syllable appended. A bare choseong or jungseong is left off either
 * way: "한" + "ㄱ" is not a prefix of "한국".
 */
internal object HangulSuggestionWord {
    fun compose(
        textBeforeCursor: String,
        composing: String,
        justCommitted: String = ""
    ): String {
        var committed = wordTail(textBeforeCursor)
        // Take the composing span out when the editor already returned it,
        // then add a finished syllable back once below.
        if (composing.isNotEmpty() && committed.endsWith(composing)) {
            committed = committed.dropLast(composing.length)
        }
        // Remote editors can lag behind finishComposingText, so the syllable
        // just committed may still be missing from text before the cursor.
        if (justCommitted.isNotEmpty() &&
            justCommitted.all(::isHangulSyllable) &&
            !committed.endsWith(justCommitted)
        ) {
            committed += justCommitted
        }
        if (composing.isEmpty() || composing.any { !isHangulSyllable(it) }) {
            return committed
        }
        return committed + composing
    }

    fun isHangulSyllable(ch: Char): Boolean = ch in '\uAC00'..'\uD7A3'

    private fun wordTail(before: String): String {
        var start = before.length
        while (start > 0) {
            val ch = before[start - 1]
            val prev = before.getOrNull(start - 2)
            val next = before.getOrNull(start)
            if (!Punctuation.isWordBoundary(ch, prev, next)) {
                start--
                continue
            }
            break
        }
        return before.substring(start)
    }
}
