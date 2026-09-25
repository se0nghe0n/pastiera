package it.palsoftware.pastiera.inputmethod

import android.view.KeyCharacterMap
import android.view.KeyEvent

/**
 * Drops a software-injected Alt tap that echoes a physical Alt release.
 *
 * Key remappers (e.g. Key Mapper with an Alt trigger) consume the physical Alt and
 * re-inject a virtual Alt down/up right after it is released. That echo re-arms
 * Alt one-shot, or latches Alt when it lands inside the double-tap window.
 */
class InjectedAltEchoFilter {
    private var lastPhysicalAltUpTime = Long.MIN_VALUE
    private var swallowingEchoUp = false

    /** Returns a debug description when the event should be consumed, else null. */
    fun shouldConsumeKeyDown(keyCode: Int, event: KeyEvent?): String? {
        if (event == null || !isAlt(keyCode) || !isInjected(event)) return null
        val delta = event.eventTime - lastPhysicalAltUpTime
        if (delta !in 0..ECHO_WINDOW_MS) return null
        swallowingEchoUp = true
        return "injected_alt_echo:ignored:delta=${delta}ms"
    }

    fun shouldConsumeKeyUp(keyCode: Int, event: KeyEvent?): String? {
        if (event == null || !isAlt(keyCode)) return null
        if (!isInjected(event)) {
            lastPhysicalAltUpTime = event.eventTime
            return null
        }
        if (!swallowingEchoUp) return null
        swallowingEchoUp = false
        return "injected_alt_echo:ignored"
    }

    private fun isAlt(keyCode: Int) =
        keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT

    private fun isInjected(event: KeyEvent) = event.deviceId == KeyCharacterMap.VIRTUAL_KEYBOARD

    private companion object {
        // Observed echo on Q25 + Key Mapper: 2ms after the physical release.
        const val ECHO_WINDOW_MS = 150L
    }
}
