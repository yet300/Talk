package com.yet.talk.database.dao.sql

/** A LIKE pattern matching [text] anywhere, with backslash, `%` and `_` escaped (queries use a backslash ESCAPE). */
internal fun likePattern(text: String): String =
    "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
