package it.palsoftware.pastiera.inputmethod.hangul

/**
 * Dubeolsik (두벌식) syllable composer.
 *
 * Jamo indexes follow the Unicode Hangul syllable order. A composed syllable is
 * `0xAC00 + ((cho * 21) + jung) * 28 + jong`. Incomplete choseong or jungseong
 * stays on the compatibility jamo block so the editor can show it while composing.
 *
 * A batchim followed by a vowel uses 연음: a complex batchim keeps its first
 * consonant and the second becomes the next syllable's choseong; a simple
 * batchim moves entirely.
 */
internal class HangulComposer {
    data class Result(
        /** Replaces the current composing region and is committed. Empty means no commit. */
        val commit: String = "",
        /** New composing text. Empty means composition is clear. */
        val composing: String = "",
        /** False when the caller should still insert [jamo] through the normal key path. */
        val consumed: Boolean = true
    )

    private var cho: Int = -1
    private var jung: Int = -1
    private var jong: Int = 0
    private var choCombined: Boolean = false
    private var jungCombined: Boolean = false
    private var jongCombined: Boolean = false

    fun process(jamoOrNullFromKey: Char?): Result {
        if (jamoOrNullFromKey == null) return backspace()
        val consonant = consonantIndex(jamoOrNullFromKey)
        if (consonant >= 0) return processConsonant(consonant)
        val vowel = vowelIndex(jamoOrNullFromKey)
        if (vowel >= 0) return processVowel(vowel)
        return passThrough()
    }

    fun flush(): Result {
        if (!hasComposition()) return Result(consumed = false)
        val text = currentText()
        clear()
        return Result(commit = text, composing = "", consumed = true)
    }

    fun reset() {
        clear()
    }

    fun hasComposition(): Boolean = cho >= 0 || jung >= 0

    private fun passThrough(): Result {
        if (!hasComposition()) return Result(consumed = false)
        val text = currentText()
        clear()
        return Result(commit = text, composing = "", consumed = false)
    }

    private fun backspace(): Result {
        if (!hasComposition()) return Result(consumed = false)
        when {
            jong != 0 -> {
                if (jongCombined) {
                    jong = JONG_DECOMPOSE[jong] ?: 0
                    jongCombined = false
                } else {
                    jong = 0
                }
            }
            jung >= 0 -> {
                if (jungCombined) {
                    jung = JUNG_DECOMPOSE[jung] ?: -1
                    jungCombined = false
                } else {
                    jung = -1
                }
            }
            cho >= 0 -> {
                if (choCombined) {
                    cho = CHO_DECOMPOSE[cho] ?: -1
                    choCombined = false
                    if (cho < 0) clear()
                } else {
                    clear()
                }
            }
        }
        return Result(composing = currentText(), consumed = true)
    }

    private fun processConsonant(incoming: Int): Result {
        if (cho < 0 && jung < 0) {
            cho = incoming
            choCombined = false
            return composingOnly()
        }
        if (jung < 0 && cho >= 0) {
            val combined = if (cho == incoming) CHO_DOUBLES[cho] ?: -1 else -1
            if (combined >= 0) {
                cho = combined
                choCombined = true
                return composingOnly()
            }
            return commitAndStartConsonant(incoming)
        }
        if (cho < 0) {
            return commitAndStartConsonant(incoming)
        }
        if (jong == 0) {
            val asJong = CHO_TO_JONG[incoming] ?: -1
            if (asJong >= 0) {
                jong = asJong
                jongCombined = false
                return composingOnly()
            }
            return commitAndStartConsonant(incoming)
        }
        val combinedJong = JONG_COMBINE[jong to incoming] ?: -1
        if (combinedJong >= 0) {
            jong = combinedJong
            jongCombined = true
            return composingOnly()
        }
        // Shift (or layout uppercase) may deliver ㄲ/ㅆ directly while jong is already
        // the simple consonant. Treat that as the doubled batchim.
        val simpleCho = JONG_TO_CHO[jong]
        if (simpleCho != null && CHO_DOUBLES[simpleCho] == incoming) {
            val doubledJong = CHO_TO_JONG[incoming] ?: -1
            if (doubledJong >= 0) {
                jong = doubledJong
                jongCombined = true
                return composingOnly()
            }
        }
        return commitAndStartConsonant(incoming)
    }

    private fun processVowel(incoming: Int): Result {
        if (cho < 0 && jung < 0) {
            jung = incoming
            jungCombined = false
            return composingOnly()
        }
        if (jong != 0 && cho >= 0 && jung >= 0) {
            val split = splitJong(jong)
            jong = split.keepJong
            val committed = currentText()
            cho = split.moveCho
            choCombined = false
            jung = incoming
            jungCombined = false
            jong = 0
            jongCombined = false
            return Result(commit = committed, composing = currentText(), consumed = true)
        }
        if (jung < 0 && cho >= 0) {
            jung = incoming
            jungCombined = false
            return composingOnly()
        }
        val combined = JUNG_COMBINE[jung to incoming] ?: -1
        if (combined >= 0) {
            jung = combined
            jungCombined = true
            return composingOnly()
        }
        val committed = currentText()
        clear()
        jung = incoming
        jungCombined = false
        return Result(commit = committed, composing = currentText(), consumed = true)
    }

    private fun commitAndStartConsonant(incoming: Int): Result {
        val committed = currentText()
        clear()
        cho = incoming
        choCombined = false
        return Result(commit = committed, composing = currentText(), consumed = true)
    }

    private fun composingOnly(): Result = Result(composing = currentText(), consumed = true)

    private fun splitJong(current: Int): JongSplit {
        JONG_SPLIT[current]?.let { return it }
        return JongSplit(keepJong = 0, moveCho = JONG_TO_CHO.getValue(current))
    }

    private fun currentText(): String {
        if (cho < 0 && jung < 0) return ""
        if (cho >= 0 && jung >= 0) {
            val code = HANGUL_BASE + ((cho * JUNG_COUNT) + jung) * JONG_COUNT + jong
            return code.toChar().toString()
        }
        if (cho >= 0) return CHOSEONG[cho].toString()
        return JUNGSEONG[jung].toString()
    }

    private fun clear() {
        cho = -1
        jung = -1
        jong = 0
        choCombined = false
        jungCombined = false
        jongCombined = false
    }

    private data class JongSplit(val keepJong: Int, val moveCho: Int)

    companion object {
        const val KOREAN_DUBEOLSIK_LAYOUT_ID = "korean_dubeolsik_qwerty"

        private const val HANGUL_BASE = 0xAC00
        private const val JUNG_COUNT = 21
        private const val JONG_COUNT = 28

        private val CHOSEONG = charArrayOf(
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
        )
        private val JUNGSEONG = charArrayOf(
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
            'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
        )

        /** Choseong index → jongseong index. Doubles that cannot be batchim are absent. */
        private val CHO_TO_JONG = mapOf(
            0 to 1, 1 to 2, 2 to 4, 3 to 7, 5 to 8, 6 to 16, 7 to 17,
            9 to 19, 10 to 20, 11 to 21, 12 to 22, 14 to 23, 15 to 24,
            16 to 25, 17 to 26, 18 to 27
        )
        private val JONG_TO_CHO = mapOf(
            1 to 0, 2 to 1, 4 to 2, 7 to 3, 8 to 5, 16 to 6, 17 to 7,
            19 to 9, 20 to 10, 21 to 11, 22 to 12, 23 to 14, 24 to 15,
            25 to 16, 26 to 17, 27 to 18
        )
        private val CHO_DOUBLES = mapOf(
            0 to 1, 3 to 4, 7 to 8, 9 to 10, 12 to 13
        )
        private val CHO_DECOMPOSE = mapOf(
            1 to 0, 4 to 3, 8 to 7, 10 to 9, 13 to 12
        )
        private val JUNG_COMBINE = mapOf(
            (8 to 0) to 9,   // ㅗ + ㅏ = ㅘ
            (8 to 1) to 10,  // ㅗ + ㅐ = ㅙ
            (8 to 20) to 11, // ㅗ + ㅣ = ㅚ
            (13 to 4) to 14, // ㅜ + ㅓ = ㅝ
            (13 to 5) to 15, // ㅜ + ㅔ = ㅞ
            (13 to 20) to 16, // ㅜ + ㅣ = ㅟ
            (18 to 20) to 19 // ㅡ + ㅣ = ㅢ
        )
        private val JUNG_DECOMPOSE = mapOf(
            9 to 8, 10 to 8, 11 to 8,
            14 to 13, 15 to 13, 16 to 13,
            19 to 18
        )
        private val JONG_COMBINE = mapOf(
            (1 to 0) to 2,   // ㄱ + ㄱ = ㄲ
            (1 to 9) to 3,   // ㄱ + ㅅ = ㄳ
            (4 to 12) to 5,  // ㄴ + ㅈ = ㄵ
            (4 to 18) to 6,  // ㄴ + ㅎ = ㄶ
            (8 to 0) to 9,   // ㄹ + ㄱ = ㄺ
            (8 to 6) to 10,  // ㄹ + ㅁ = ㄻ
            (8 to 7) to 11,  // ㄹ + ㅂ = ㄼ
            (8 to 9) to 12,  // ㄹ + ㅅ = ㄽ
            (8 to 16) to 13, // ㄹ + ㅌ = ㄾ
            (8 to 17) to 14, // ㄹ + ㅍ = ㄿ
            (8 to 18) to 15, // ㄹ + ㅎ = ㅀ
            (17 to 9) to 18, // ㅂ + ㅅ = ㅄ
            (19 to 9) to 20  // ㅅ + ㅅ = ㅆ
        )
        private val JONG_DECOMPOSE = mapOf(
            2 to 1, 3 to 1, 5 to 4, 6 to 4,
            9 to 8, 10 to 8, 11 to 8, 12 to 8, 13 to 8, 14 to 8, 15 to 8,
            18 to 17, 20 to 19
        )
        private val JONG_SPLIT = mapOf(
            3 to JongSplit(1, 9),
            5 to JongSplit(4, 12),
            6 to JongSplit(4, 18),
            9 to JongSplit(8, 0),
            10 to JongSplit(8, 6),
            11 to JongSplit(8, 7),
            12 to JongSplit(8, 9),
            13 to JongSplit(8, 16),
            14 to JongSplit(8, 17),
            15 to JongSplit(8, 18),
            18 to JongSplit(17, 9)
        )

        private val consonantIndexes: Map<Char, Int> = CHOSEONG.withIndex().associate { it.value to it.index }
        private val vowelIndexes: Map<Char, Int> = JUNGSEONG.withIndex().associate { it.value to it.index }

        fun isActiveForLayout(layoutName: String?): Boolean = layoutName == KOREAN_DUBEOLSIK_LAYOUT_ID

        private fun consonantIndex(ch: Char): Int = consonantIndexes[ch] ?: -1

        private fun vowelIndex(ch: Char): Int = vowelIndexes[ch] ?: -1
    }
}
