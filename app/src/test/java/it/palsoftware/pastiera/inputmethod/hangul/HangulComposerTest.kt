package it.palsoftware.pastiera.inputmethod.hangul

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HangulComposerTest {

    @Test
    fun `ㄱ plus ㅏ composes 가`() {
        val composer = HangulComposer()
        composer.process('ㄱ')
        val result = composer.process('ㅏ')
        assertEquals("", result.commit)
        assertEquals("가", result.composing)
        assertEquals(0xAC00, result.composing[0].code)
    }

    @Test
    fun `가 plus ㄴ composes 간`() {
        val composer = HangulComposer()
        composer.process('ㄱ')
        composer.process('ㅏ')
        val result = composer.process('ㄴ')
        assertEquals("", result.commit)
        assertEquals("간", result.composing)
    }

    @Test
    fun `간 plus ㅏ uses yeoneum to 가나`() {
        val composer = HangulComposer()
        composer.process('ㄱ')
        composer.process('ㅏ')
        composer.process('ㄴ')
        val result = composer.process('ㅏ')
        assertEquals("가", result.commit)
        assertEquals("나", result.composing)
        assertTrue(result.consumed)
        assertEquals("가나", textAfter(result, committedAlready = ""))
    }

    @Test
    fun `각 plus ㅏ uses yeoneum to 가가`() {
        val composer = HangulComposer()
        composer.process('ㄱ')
        composer.process('ㅏ')
        assertEquals("각", composer.process('ㄱ').composing)
        val result = composer.process('ㅏ')
        assertEquals("가", result.commit)
        assertEquals("가", result.composing)
    }

    @Test
    fun `ㅂ plus ㅂ combines to ㅃ and shift ㅃ is atomic`() {
        val combined = HangulComposer()
        combined.allowDoublePressTenseConsonants = true
        combined.process('ㅂ')
        val doubled = combined.process('ㅂ')
        assertEquals("", doubled.commit)
        assertEquals("ㅃ", doubled.composing)
        assertEquals("ㅂ", combined.process(null).composing)

        val syllable = HangulComposer()
        syllable.allowDoublePressTenseConsonants = true
        syllable.process('ㅂ')
        syllable.process('ㅂ')
        assertEquals("빠", syllable.process('ㅏ').composing)
        assertEquals("ㅃ", syllable.process(null).composing)
        assertEquals("ㅂ", syllable.process(null).composing)

        val shifted = HangulComposer()
        assertEquals("ㅃ", shifted.process('ㅃ').composing)
        assertEquals("", shifted.process(null).composing)
        assertFalse(shifted.process(null).consumed)
    }

    @Test
    fun `ㅗ plus ㅏ composes ㅘ inside and outside a syllable`() {
        val bare = HangulComposer()
        bare.process('ㅗ')
        assertEquals("ㅘ", bare.process('ㅏ').composing)

        val syllable = HangulComposer()
        syllable.process('ㄱ')
        syllable.process('ㅗ')
        assertEquals("과", syllable.process('ㅏ').composing)
    }

    @Test
    fun `backspace decomposes jong then jung then cho`() {
        val composer = HangulComposer()
        composer.process('ㄱ')
        composer.process('ㅏ')
        composer.process('ㄴ')
        val dropJong = composer.process(null)
        assertTrue(dropJong.consumed)
        assertEquals("", dropJong.commit)
        assertEquals("가", dropJong.composing)
        assertEquals("ㄱ", composer.process(null).composing)
        val cleared = composer.process(null)
        assertEquals("", cleared.composing)
        assertTrue(cleared.consumed)
        assertFalse(composer.process(null).consumed)
    }

    @Test
    fun `backspace decomposes compound jong and compound jung`() {
        val batchim = HangulComposer()
        batchim.process('ㄱ')
        batchim.process('ㅏ')
        batchim.process('ㅂ')
        assertEquals("값", batchim.process('ㅅ').composing)
        assertEquals("갑", batchim.process(null).composing)
        assertEquals("가", batchim.process(null).composing)

        val vowel = HangulComposer()
        vowel.process('ㅗ')
        vowel.process('ㅏ')
        assertEquals("ㅗ", vowel.process(null).composing)
    }

    @Test
    fun `compound vowels and batchim follow dubeolsik tables`() {
        assertEquals("왜", type("ㅇㅗㅐ"))
        assertEquals("외", type("ㅇㅗㅣ"))
        assertEquals("워", type("ㅇㅜㅓ"))
        assertEquals("웨", type("ㅇㅜㅔ"))
        assertEquals("위", type("ㅇㅜㅣ"))
        assertEquals("의", type("ㅇㅡㅣ"))
        assertEquals("앉", type("ㅇㅏㄴㅈ"))
        assertEquals("않", type("ㅇㅏㄴㅎ"))
        assertEquals("닭", type("ㄷㅏㄹㄱ"))
        assertEquals("값", type("ㄱㅏㅂㅅ"))
        assertEquals("없", type("ㅇㅓㅂㅅ"))
        assertEquals("갔", type("ㄱㅏㅅㅅ", allowDoublePressTenseConsonants = true))
        assertEquals("까", type("ㄱㄱㅏ", allowDoublePressTenseConsonants = true))
        assertEquals("따", type("ㄷㄷㅏ", allowDoublePressTenseConsonants = true))
        assertEquals("싸", type("ㅅㅅㅏ", allowDoublePressTenseConsonants = true))
        assertEquals("짜", type("ㅈㅈㅏ", allowDoublePressTenseConsonants = true))
    }

    @Test
    fun `complex batchim splits on the next vowel`() {
        assertEquals("안자", type("ㅇㅏㄴㅈㅏ"))
        assertEquals("안하", type("ㅇㅏㄴㅎㅏ"))
        assertEquals("달가", type("ㄷㅏㄹㄱㅏ"))
        assertEquals("갑사", type("ㄱㅏㅂㅅㅏ"))
        assertEquals("업서", type("ㅇㅓㅂㅅㅓ"))
        assertEquals("가싸", type("ㄱㅏㅅㅅㅏ", allowDoublePressTenseConsonants = true))
        assertEquals("가까", type("ㄱㅏㄱㄱㅏ", allowDoublePressTenseConsonants = true))
    }

    @Test
    fun `consecutive syllables commit when the next jamo cannot join`() {
        assertEquals("한국어", type("ㅎㅏㄴㄱㅜㄱㅇㅓ"))
        assertEquals("안녕", type("ㅇㅏㄴㄴㅕㅇ"))
        assertEquals("값진", type("ㄱㅏㅂㅅㅈㅣㄴ"))
    }

    @Test
    fun `doubles that cannot be batchim start a new syllable`() {
        val composer = HangulComposer()
        composer.process('ㄱ')
        composer.process('ㅏ')
        val result = composer.process('ㄸ')
        assertEquals("가", result.commit)
        assertEquals("ㄸ", result.composing)
    }

    @Test
    fun `non jamo commits the syllable and is not consumed`() {
        val composer = HangulComposer()
        composer.process('ㄱ')
        composer.process('ㅏ')
        val result = composer.process('!')
        assertFalse(result.consumed)
        assertEquals("가", result.commit)
        assertEquals("", result.composing)
        assertFalse(composer.hasComposition())
    }

    @Test
    fun `flush commits the current syllable`() {
        val composer = HangulComposer()
        composer.process('ㄱ')
        composer.process('ㅏ')
        val flushed = composer.flush()
        assertEquals("가", flushed.commit)
        assertEquals("", flushed.composing)
        assertFalse(composer.hasComposition())
    }

    @Test
    fun `simple batchim doubles with plain and shift jamo`() {
        assertEquals("갔", type("ㄱㅏㅅㅅ", allowDoublePressTenseConsonants = true))
        assertEquals("갂", type("ㄱㅏㄱㄱ", allowDoublePressTenseConsonants = true)) // 가 + ㄱ + ㄱ
        assertEquals("깍", type("ㄲㅏㄱ")) // ㄲ + ㅏ + ㄱ
        assertEquals("깎", type("ㄲㅏㄱㄱ", allowDoublePressTenseConsonants = true)) // ㄲ + ㅏ + ㄱ + ㄱ → ㄲㅏㄲ
        assertEquals("깎", type("ㄲㅏㄲ")) // 까 + Shiftㄱ (ㄲ as atomic batchim)
        assertEquals("샀", type("ㅅㅏㅆ")) // 사 + Shiftㅌ→ㅆ
        val shiftDouble = HangulComposer()
        shiftDouble.process('ㄱ')
        shiftDouble.process('ㅏ')
        shiftDouble.process('ㄱ')
        val result = shiftDouble.process('ㄲ') // Shift delivers ㄲ while jong is ㄱ
        assertEquals("", result.commit)
        assertEquals("갂", result.composing)
        assertTrue(result.consumed)
        val shiftFromEmptyJong = HangulComposer()
        shiftFromEmptyJong.process('ㄱ')
        shiftFromEmptyJong.process('ㅏ')
        assertEquals("갂", shiftFromEmptyJong.process('ㄲ').composing) // 가 + Shiftㄱ
        // Modern jamo (U+1100 block) must compose the same way as compatibility jamo.
        val modern = HangulComposer()
        modern.process('ᄀ') // U+1100
        modern.process('ᅡ') // U+1161
        assertEquals("갂", modern.process('ᄁ').composing) // U+1101
    }

    @Test
    fun `flush and non-jamo leave a committed syllable for IME punctuation path`() {
        // IME must flush then self-commit the symbol (not CallSuper KeyEvent),
        // otherwise some editors wipe the finished syllable.
        val composer = HangulComposer()
        composer.process('ㄱ')
        composer.process('ㅏ')
        assertTrue(composer.hasComposition())
        val flushed = composer.flush()
        assertEquals("가", flushed.commit)
        assertEquals("", flushed.composing)
        assertTrue(flushed.consumed)
        assertFalse(composer.hasComposition())

        val again = HangulComposer()
        again.process('ㄱ')
        again.process('ㅏ')
        val punct = again.process('.')
        assertEquals("가", punct.commit)
        assertEquals("", punct.composing)
        assertFalse(punct.consumed)
        assertFalse(again.hasComposition())

        val space = HangulComposer()
        space.process('ㄲ')
        space.process('ㅏ')
        space.process('ㄲ')
        assertEquals("깎", space.flush().commit)
    }

    @Test
    fun `이 plus ㅆ attaches batchim as 있`() {
        // Device regression: premature finish left 이ㅆ instead of 있.
        assertEquals("있", type("ㅇㅣㅆ"))
        val shift = HangulComposer()
        shift.process('ㅇ')
        shift.process('ㅣ')
        val result = shift.process('ㅆ') // Shift+T in Dubeolsik
        assertEquals("", result.commit)
        assertEquals("있", result.composing)
        assertTrue(result.consumed)
        assertTrue(shift.hasComposition())
        // Period must flush the full syllable, not leave bare ㅆ.
        val flushed = shift.flush()
        assertEquals("있", flushed.commit)
        assertEquals("", flushed.composing)
    }

    @Test
    fun `있다 아님 버그다 syllable breaks use commit plus composing without punctuation`() {
        // Device regression: spurious "." appeared between syllables (있.다 / 아.님 / 버그.다)
        // at commitAndStartConsonant / 연음. Composer itself must only emit Hangul.
        fun steps(sequence: String): List<HangulComposer.Result> {
            val composer = HangulComposer()
            return sequence.map { composer.process(it) }
        }

        val itda = steps("ㅇㅣㅆㄷㅏ")
        assertEquals("", itda[2].commit) // 있 still composing after ㅆ
        assertEquals("있", itda[2].composing)
        assertEquals("있", itda[3].commit) // ㄷ commits 있 and starts ㄷ
        assertEquals("ㄷ", itda[3].composing)
        assertFalse(itda[3].commit.contains('.'))
        assertFalse(itda[3].composing.contains('.'))
        assertEquals("", itda[4].commit)
        assertEquals("다", itda[4].composing)
        assertEquals("있다", type("ㅇㅣㅆㄷㅏ"))

        val anim = steps("ㅇㅏㄴㅣㅁ")
        assertEquals("아", anim[3].commit) // 연음 안+ㅣ
        assertEquals("니", anim[3].composing)
        assertFalse(anim[3].commit.contains('.'))
        assertEquals("아님", type("ㅇㅏㄴㅣㅁ"))

        val beogeuda = steps("ㅂㅓㄱㅡㄷㅏ")
        assertEquals("버", beogeuda[3].commit) // 연음 벅+ㅡ
        assertEquals("그", beogeuda[3].composing)
        assertEquals("", beogeuda[4].commit) // ㄷ attaches as batchim → 귿
        assertEquals("귿", beogeuda[4].composing)
        assertEquals("그", beogeuda[5].commit) // ㅏ splits 귿 → 그 + 다
        assertEquals("다", beogeuda[5].composing)
        assertFalse(beogeuda.any { '.' in it.commit || '.' in it.composing })
        assertEquals("버그다", type("ㅂㅓㄱㅡㄷㅏ"))
    }

    @Test
    fun `layout activation is tied to korean dubeolsik id`() {
        assertEquals("korean_dubeolsik_qwerty", HangulComposer.KOREAN_DUBEOLSIK_LAYOUT_ID)
        assertTrue(HangulComposer.isActiveForLayout("korean_dubeolsik_qwerty"))
        assertFalse(HangulComposer.isActiveForLayout("qwerty"))
        assertFalse(HangulComposer.isActiveForLayout("vietnamese_telex_qwerty"))
        assertFalse(HangulComposer.isActiveForLayout(null))
    }

    @Test
    fun `double press does not create tense consonants when disabled`() {
        val composer = HangulComposer()
        assertFalse(composer.allowDoublePressTenseConsonants)

        assertEquals("ㄱ", composer.process('ㄱ').composing)
        val secondGiyeok = composer.process('ㄱ')
        assertEquals("ㄱ", secondGiyeok.commit)
        assertEquals("ㄱ", secondGiyeok.composing)
        val vowel = composer.process('ㅏ')
        assertEquals("", vowel.commit)
        assertEquals("가", vowel.composing)
        assertEquals("ㄱ가", type("ㄱㄱㅏ"))
        assertEquals("ㄷ다", type("ㄷㄷㅏ"))
        assertEquals("ㅂ바", type("ㅂㅂㅏ"))
        assertEquals("ㅅ사", type("ㅅㅅㅏ"))
        assertEquals("ㅈ자", type("ㅈㅈㅏ"))
    }

    @Test
    fun `shift tense consonants and compound batchim still combine when double press is disabled`() {
        assertEquals("까", type("ㄲㅏ"))
        assertEquals("따", type("ㄸㅏ"))
        assertEquals("빠", type("ㅃㅏ"))
        assertEquals("싸", type("ㅆㅏ"))
        assertEquals("짜", type("ㅉㅏ"))
        assertEquals("갔", type("ㄱㅏㅆ"))
        assertEquals("갂", type("ㄱㅏㄲ"))
        assertEquals("깎", type("ㄲㅏㄲ"))
        assertEquals("샀", type("ㅅㅏㅆ"))
        assertEquals("값", type("ㄱㅏㅂㅅ"))
        assertEquals("갃", type("ㄱㅏㄱㅅ"))
        assertEquals("앉", type("ㅇㅏㄴㅈ"))
        assertEquals("닭", type("ㄷㅏㄹㄱ"))

        val shiftOnGiyeokBatchim = HangulComposer()
        shiftOnGiyeokBatchim.process('ㄱ')
        shiftOnGiyeokBatchim.process('ㅏ')
        shiftOnGiyeokBatchim.process('ㄱ')
        val giyeokShift = shiftOnGiyeokBatchim.process('ㄲ')
        assertEquals("", giyeokShift.commit)
        assertEquals("갂", giyeokShift.composing)
        assertTrue(giyeokShift.consumed)

        val shiftOnSiotBatchim = HangulComposer()
        shiftOnSiotBatchim.process('ㅅ')
        shiftOnSiotBatchim.process('ㅏ')
        shiftOnSiotBatchim.process('ㅅ')
        assertEquals("샀", shiftOnSiotBatchim.process('ㅆ').composing)
    }

    @Test
    fun `가 plus plain ㄱ ㄱ commits instead of doubling the batchim`() {
        assertEquals("각ㄱ", type("ㄱㅏㄱㄱ"))
        assertEquals("갓ㅅ", type("ㄱㅏㅅㅅ"))
        assertEquals("각가", type("ㄱㅏㄱㄱㅏ"))
        assertEquals("갓사", type("ㄱㅏㅅㅅㅏ"))
        assertEquals("깍ㄱ", type("ㄲㅏㄱㄱ"))

        val composer = HangulComposer()
        composer.process('ㄱ')
        composer.process('ㅏ')
        assertEquals("각", composer.process('ㄱ').composing)
        val second = composer.process('ㄱ')
        assertEquals("각", second.commit)
        assertEquals("ㄱ", second.composing)
        assertTrue(second.consumed)
    }

    private fun type(
        sequence: String,
        allowDoublePressTenseConsonants: Boolean = false
    ): String {
        val composer = HangulComposer()
        composer.allowDoublePressTenseConsonants = allowDoublePressTenseConsonants
        val committed = StringBuilder()
        var composing = ""
        for (ch in sequence) {
            val result = composer.process(ch)
            if (result.commit.isNotEmpty()) committed.append(result.commit)
            composing = result.composing
        }
        return committed.toString() + composing
    }

    private fun textAfter(result: HangulComposer.Result, committedAlready: String): String {
        return committedAlready + result.commit + result.composing
    }
}
