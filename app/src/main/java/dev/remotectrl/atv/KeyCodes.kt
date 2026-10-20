package dev.remotectrl.atv

/** Android KeyEvent codes understood by the TV. */
object KeyCodes {
    const val HOME = 3
    const val BACK = 4
    const val UP = 19
    const val DOWN = 20
    const val LEFT = 21
    const val RIGHT = 22
    const val CENTER = 23
    const val VOLUME_UP = 24
    const val VOLUME_DOWN = 25
    const val POWER = 26
    const val MUTE = 164
    const val PLAY_PAUSE = 85
    const val ENTER = 66
    const val DELETE = 67
    const val MENU = 82
    const val TV = 170
    const val CHANNEL_UP = 166
    const val CHANNEL_DOWN = 167
    const val BOOKMARK = 174
    const val SETTINGS = 176
    const val TV_INPUT = 178
    const val VOICE_ASSIST = 231

    private const val KEY_0 = 7
    private const val KEY_A = 29
    private val punctuation = mapOf(' ' to 62, ',' to 55, '.' to 56, '-' to 69, '@' to 77, '/' to 76, '\n' to ENTER)

    /** Maps one typed character to a key code; letters are sent lowercase. Null if unsupported. */
    fun forChar(c: Char): Int? = when (val l = c.lowercaseChar()) {
        in 'a'..'z' -> KEY_A + (l - 'a')
        in '0'..'9' -> KEY_0 + (l - '0')
        else -> punctuation[l]
    }
}
