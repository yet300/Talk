package com.yet.talk.domain.rule

import kotlin.test.Test
import kotlin.test.assertEquals

class RevisionDiffTest {
    @Test
    fun countsAddedAndRemovedLines() {
        assertEquals(LineChanges(added = 1, removed = 1), RevisionDiff.lineChanges("# A\nold\nsame", "# A\nnew\nsame"))
        assertEquals(LineChanges(added = 2, removed = 0), RevisionDiff.lineChanges("one", "one\ntwo\nthree"))
        assertEquals(LineChanges(added = 0, removed = 0), RevisionDiff.lineChanges("x\ny", "x\ny"))
    }
}
