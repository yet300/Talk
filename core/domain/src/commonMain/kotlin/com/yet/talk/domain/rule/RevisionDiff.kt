package com.yet.talk.domain.rule

/** Lines added and removed between two revisions, as the revision list shows them (`+212 −38`). */
data class LineChanges(val added: Int, val removed: Int)

object RevisionDiff {
    /** Counts by the longest common subsequence of lines, so moved-but-unchanged lines do not count. */
    fun lineChanges(old: String, new: String): LineChanges {
        val a = old.lines()
        val b = new.lines()
        // Two rows of the LCS table are enough; posts stay within 10,000 characters.
        var previous = IntArray(b.size + 1)
        var current = IntArray(b.size + 1)
        for (i in 1..a.size) {
            for (j in 1..b.size) {
                current[j] = if (a[i - 1] == b[j - 1]) previous[j - 1] + 1 else maxOf(previous[j], current[j - 1])
            }
            previous = current.also { current = previous }
        }
        val common = previous[b.size]
        return LineChanges(added = b.size - common, removed = a.size - common)
    }
}
