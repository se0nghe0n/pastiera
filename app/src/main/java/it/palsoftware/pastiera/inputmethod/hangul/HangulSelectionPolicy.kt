package it.palsoftware.pastiera.inputmethod.hangul

/**
 * Decides when an [android.inputmethodservice.InputMethodService.onUpdateSelection]
 * callback should commit an in-progress Hangul syllable.
 *
 * Ordinary composing updates (setComposingText) must NOT finish composition — that
 * prematurely commits 이 before Shift+T ㅆ can attach as batchim (device: 이ㅆ…).
 */
internal object HangulSelectionPolicy {
    fun shouldFinishComposition(
        hasComposition: Boolean,
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ): Boolean {
        if (!hasComposition) return false

        val collapsed = newSelStart == newSelEnd
        // User highlighted text → commit.
        if (!collapsed) return true

        val cursorChanged = oldSelStart != newSelStart || oldSelEnd != newSelEnd
        if (!cursorChanged) return false

        // Prefer the composing span when the editor reports it.
        if (candidatesStart >= 0 && candidatesEnd >= candidatesStart) {
            // Cursor still inside (or at the exclusive end of) the composing region.
            val insideOrAtComposingEnd =
                newSelStart >= candidatesStart && newSelStart <= candidatesEnd
            return !insideOrAtComposingEnd
        }

        // No composing region reported: ignore ordinary forward-by-one typing;
        // finish on backward moves or jumps larger than one.
        val forwardByOne = oldSelStart == oldSelEnd &&
            newSelEnd == newSelStart &&
            newSelStart == oldSelStart + 1
        if (forwardByOne) return false
        if (newSelStart < oldSelStart) return true
        val delta = kotlin.math.abs(newSelStart - oldSelStart)
        return delta > 1
    }

    fun finishReason(
        hasComposition: Boolean,
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ): String? {
        if (!shouldFinishComposition(
                hasComposition,
                oldSelStart,
                oldSelEnd,
                newSelStart,
                newSelEnd,
                candidatesStart,
                candidatesEnd
            )
        ) {
            return null
        }
        val collapsed = newSelStart == newSelEnd
        return when {
            !collapsed -> "non-collapsed selection"
            candidatesStart >= 0 && candidatesEnd >= candidatesStart ->
                "cursor outside composing [$candidatesStart,$candidatesEnd] -> $newSelStart"
            newSelStart < oldSelStart -> "backward move $oldSelStart -> $newSelStart"
            else -> "cursor jump $oldSelStart -> $newSelStart (no candidates)"
        }
    }
}
