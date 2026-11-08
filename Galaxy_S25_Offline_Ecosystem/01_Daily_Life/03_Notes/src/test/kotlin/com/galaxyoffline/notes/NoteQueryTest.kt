package com.galaxyoffline.notes

import com.galaxyoffline.notes.data.Note
import com.galaxyoffline.notes.data.NoteFilter
import com.galaxyoffline.notes.data.NoteQuery
import com.galaxyoffline.notes.data.NoteSort
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteQueryTest {
    private fun n(id: Long, title: String, pinned: Boolean = false, archived: Boolean = false, deleted: Long? = null, t: Long = id) =
        Note(id = id, title = title, content = "", pinned = pinned, archived = archived, deletedAt = deleted, createdAt = t, updatedAt = t)

    private val all = listOf(n(1, "Işık"), n(2, "alışveriş", pinned = true), n(3, "Arşiv", archived = true), n(4, "Çöp", deleted = 5))

    @Test fun filtersAndPinnedFirst() {
        assertEquals(listOf(2L, 1L), NoteQuery.apply(all, NoteFilter.NOTES, "", NoteSort.UPDATED).map { it.id })
        assertEquals(listOf(3L), NoteQuery.apply(all, NoteFilter.ARCHIVE, "", NoteSort.UPDATED).map { it.id })
        assertEquals(listOf(4L), NoteQuery.apply(all, NoteFilter.TRASH, "", NoteSort.UPDATED).map { it.id })
    }

    @Test fun turkishCaseInsensitiveSearch() {
        assertEquals(listOf(1L), NoteQuery.apply(all, NoteFilter.NOTES, "ışı", NoteSort.UPDATED).map { it.id })
        assertEquals(listOf(2L), NoteQuery.apply(all, NoteFilter.NOTES, "ALIŞ", NoteSort.TITLE).map { it.id })
    }
}
