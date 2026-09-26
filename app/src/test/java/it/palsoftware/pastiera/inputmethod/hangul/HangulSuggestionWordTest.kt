package it.palsoftware.pastiera.inputmethod.hangul

import org.junit.Assert.assertEquals
import org.junit.Test

class HangulSuggestionWordTest {

    @Test
    fun `composing syllable extends the committed word`() {
        assertEquals("안녕하", HangulSuggestionWord.compose("안녕", "하"))
    }

    @Test
    fun `composing syllable already in the editor is not repeated`() {
        assertEquals("한국", HangulSuggestionWord.compose("한국", "국"))
    }

    @Test
    fun `repeated syllable already in the editor is not collapsed`() {
        assertEquals("가가", HangulSuggestionWord.compose("가가", "가"))
    }

    @Test
    fun `bare choseong already in the editor is left off the word`() {
        assertEquals("한", HangulSuggestionWord.compose("한ㄱ", "ㄱ"))
    }

    @Test
    fun `bare choseong does not break the syllable prefix`() {
        assertEquals("안녕", HangulSuggestionWord.compose("안녕", "ㅎ"))
    }

    @Test
    fun `first syllable is the whole word`() {
        assertEquals("한", HangulSuggestionWord.compose("", "한"))
    }

    @Test
    fun `jamo alone does not start a suggestion word`() {
        assertEquals("", HangulSuggestionWord.compose("", "ㄱ"))
    }

    @Test
    fun `word boundary drops the previous eojeol`() {
        assertEquals("에", HangulSuggestionWord.compose("학교, ", "에"))
    }

    @Test
    fun `finished word is the text before the cursor`() {
        assertEquals("한국", HangulSuggestionWord.compose("한국", ""))
    }

    @Test
    fun `just committed syllable is kept when the editor has not caught up`() {
        assertEquals("가나", HangulSuggestionWord.compose("", "나", justCommitted = "가"))
    }

    @Test
    fun `just committed syllable is not duplicated once the editor has it`() {
        assertEquals("가나", HangulSuggestionWord.compose("가", "나", justCommitted = "가"))
    }
}
