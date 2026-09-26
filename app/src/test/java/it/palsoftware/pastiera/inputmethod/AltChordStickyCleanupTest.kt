package it.palsoftware.pastiera.inputmethod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AltChordStickyCleanupTest {

    @Test
    fun chordDropsOneShotArmedOnThisHold() {
        val next = AltChordStickyCleanup.afterChord(
            shortcutUsedDuringHold = true,
            current = AltChordStickyCleanup.StickyFlags(oneShot = true, latchActive = false),
            beforeHoldOneShot = false,
            beforeHoldLatch = false
        )
        assertFalse(next.oneShot)
        assertFalse(next.latchActive)
        assertFalse(next.modifierLayerLatched)
    }

    @Test
    fun chordDropsLatchArmedBySingleTapLatches() {
        val next = AltChordStickyCleanup.afterChord(
            shortcutUsedDuringHold = true,
            current = AltChordStickyCleanup.StickyFlags(oneShot = false, latchActive = true),
            beforeHoldOneShot = false,
            beforeHoldLatch = false
        )
        assertFalse(next.latchActive)
    }

    @Test
    fun chordKeepsPreexistingLatch() {
        val next = AltChordStickyCleanup.afterChord(
            shortcutUsedDuringHold = true,
            current = AltChordStickyCleanup.StickyFlags(oneShot = false, latchActive = true),
            beforeHoldOneShot = false,
            beforeHoldLatch = true
        )
        assertTrue(next.latchActive)
    }

    @Test
    fun noChordLeavesStickyUntouched() {
        val next = AltChordStickyCleanup.afterChord(
            shortcutUsedDuringHold = false,
            current = AltChordStickyCleanup.StickyFlags(
                oneShot = true,
                latchActive = true,
                modifierLayerLatched = true
            ),
            beforeHoldOneShot = false,
            beforeHoldLatch = false
        )
        assertEquals(
            AltChordStickyCleanup.StickyFlags(
                oneShot = true,
                latchActive = true,
                modifierLayerLatched = true
            ),
            next
        )
    }
}
