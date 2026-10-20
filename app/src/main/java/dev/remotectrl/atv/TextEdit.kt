package dev.remotectrl.atv

/** Backspaces then new text: what the TV must receive to go from one text to another. */
data class TextEdit(val deletes: Int, val insert: String)

/** Assumes the TV's cursor sits at the end of its text, as it does when typing letter by letter. */
fun diffText(old: String, new: String): TextEdit {
    val shared = old.commonPrefixWith(new).length
    return TextEdit(deletes = old.length - shared, insert = new.substring(shared))
}
