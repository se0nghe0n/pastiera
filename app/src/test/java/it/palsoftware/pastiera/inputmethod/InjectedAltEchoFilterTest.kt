package it.palsoftware.pastiera.inputmethod

import android.view.KeyCharacterMap
import android.view.KeyEvent
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InjectedAltEchoFilterTest {
    private val filter = InjectedAltEchoFilter()
    private val alt = KeyEvent.KEYCODE_ALT_LEFT

    @Test
    fun injectedAltRightAfterPhysicalRelease_isConsumedDownAndUp() {
        // Sequence captured on Q25 with Key Mapper: physical Alt up, then a virtual Alt tap 2ms later.
        assertNull(filter.shouldConsumeKeyDown(alt, event(KeyEvent.ACTION_DOWN, 1_000, PHYSICAL)))
        assertNull(filter.shouldConsumeKeyUp(alt, event(KeyEvent.ACTION_UP, 2_000, PHYSICAL)))
        assertNotNull(filter.shouldConsumeKeyDown(alt, event(KeyEvent.ACTION_DOWN, 2_002, VIRTUAL)))
        assertNotNull(filter.shouldConsumeKeyUp(alt, event(KeyEvent.ACTION_UP, 2_002, VIRTUAL)))
    }

    @Test
    fun injectedAltLongAfterPhysicalRelease_passesThrough() {
        filter.shouldConsumeKeyUp(alt, event(KeyEvent.ACTION_UP, 2_000, PHYSICAL))
        assertNull(filter.shouldConsumeKeyDown(alt, event(KeyEvent.ACTION_DOWN, 3_000, VIRTUAL)))
        assertNull(filter.shouldConsumeKeyUp(alt, event(KeyEvent.ACTION_UP, 3_050, VIRTUAL)))
    }

    @Test
    fun physicalAltTapAfterRelease_passesThrough() {
        filter.shouldConsumeKeyUp(alt, event(KeyEvent.ACTION_UP, 2_000, PHYSICAL))
        assertNull(filter.shouldConsumeKeyDown(alt, event(KeyEvent.ACTION_DOWN, 2_050, PHYSICAL)))
    }

    @Test
    fun injectedAltWithoutPriorPhysicalRelease_passesThrough() {
        assertNull(filter.shouldConsumeKeyDown(alt, event(KeyEvent.ACTION_DOWN, 10, VIRTUAL)))
        assertNull(filter.shouldConsumeKeyUp(alt, event(KeyEvent.ACTION_UP, 20, VIRTUAL)))
    }

    private fun event(action: Int, time: Long, deviceId: Int) =
        KeyEvent(time, time, action, alt, 0, 0, deviceId, 56)

    private companion object {
        const val PHYSICAL = 0
        const val VIRTUAL = KeyCharacterMap.VIRTUAL_KEYBOARD
    }
}
