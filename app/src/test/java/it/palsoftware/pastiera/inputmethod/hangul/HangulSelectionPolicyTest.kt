package it.palsoftware.pastiera.inputmethod.hangul

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HangulSelectionPolicyTest {

    @Test
    fun `setComposingText style updates inside composing region do not finish`() {
        // After ㅇ→이, editor keeps cursor at end of composing span [5,6].
        assertFalse(
            HangulSelectionPolicy.shouldFinishComposition(
                hasComposition = true,
                oldSelStart = 6,
                oldSelEnd = 6,
                newSelStart = 6,
                newSelEnd = 6,
                candidatesStart = 5,
                candidatesEnd = 6
            )
        )
        // Cursor reports at exclusive end while composing grows in place.
        assertFalse(
            HangulSelectionPolicy.shouldFinishComposition(
                hasComposition = true,
                oldSelStart = 5,
                oldSelEnd = 5,
                newSelStart = 6,
                newSelEnd = 6,
                candidatesStart = 5,
                candidatesEnd = 6
            )
        )
    }

    @Test
    fun `cursor leaving composing region finishes`() {
        assertTrue(
            HangulSelectionPolicy.shouldFinishComposition(
                hasComposition = true,
                oldSelStart = 6,
                oldSelEnd = 6,
                newSelStart = 3,
                newSelEnd = 3,
                candidatesStart = 5,
                candidatesEnd = 6
            )
        )
        assertTrue(
            HangulSelectionPolicy.shouldFinishComposition(
                hasComposition = true,
                oldSelStart = 6,
                oldSelEnd = 6,
                newSelStart = 10,
                newSelEnd = 10,
                candidatesStart = 5,
                candidatesEnd = 6
            )
        )
    }

    @Test
    fun `non-collapsed selection finishes`() {
        assertTrue(
            HangulSelectionPolicy.shouldFinishComposition(
                hasComposition = true,
                oldSelStart = 6,
                oldSelEnd = 6,
                newSelStart = 4,
                newSelEnd = 6,
                candidatesStart = 5,
                candidatesEnd = 6
            )
        )
    }

    @Test
    fun `without candidates forward-by-one does not finish but jump does`() {
        assertFalse(
            HangulSelectionPolicy.shouldFinishComposition(
                hasComposition = true,
                oldSelStart = 5,
                oldSelEnd = 5,
                newSelStart = 6,
                newSelEnd = 6,
                candidatesStart = -1,
                candidatesEnd = -1
            )
        )
        assertTrue(
            HangulSelectionPolicy.shouldFinishComposition(
                hasComposition = true,
                oldSelStart = 6,
                oldSelEnd = 6,
                newSelStart = 4,
                newSelEnd = 4,
                candidatesStart = -1,
                candidatesEnd = -1
            )
        )
        assertTrue(
            HangulSelectionPolicy.shouldFinishComposition(
                hasComposition = true,
                oldSelStart = 5,
                oldSelEnd = 5,
                newSelStart = 9,
                newSelEnd = 9,
                candidatesStart = -1,
                candidatesEnd = -1
            )
        )
    }

    @Test
    fun `no composition never finishes`() {
        assertFalse(
            HangulSelectionPolicy.shouldFinishComposition(
                hasComposition = false,
                oldSelStart = 0,
                oldSelEnd = 0,
                newSelStart = 10,
                newSelEnd = 10,
                candidatesStart = -1,
                candidatesEnd = -1
            )
        )
    }
}
