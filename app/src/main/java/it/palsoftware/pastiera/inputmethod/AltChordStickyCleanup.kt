package it.palsoftware.pastiera.inputmethod

/**
 * Decides which sticky Alt flags to drop after an Alt+key chord.
 * Latch/one-shot that were already active before the hold are preserved.
 */
internal object AltChordStickyCleanup {
    data class StickyFlags(
        val oneShot: Boolean,
        val latchActive: Boolean,
        val modifierLayerLatched: Boolean = false
    )

    fun afterChord(
        shortcutUsedDuringHold: Boolean,
        current: StickyFlags,
        beforeHoldOneShot: Boolean?,
        beforeHoldLatch: Boolean?
    ): StickyFlags {
        if (!shortcutUsedDuringHold) return current
        return StickyFlags(
            oneShot = if (beforeHoldOneShot == true) current.oneShot else false,
            latchActive = if (beforeHoldLatch == true) current.latchActive else false,
            modifierLayerLatched = false
        )
    }
}
