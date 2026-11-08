package com.galaxyoffline.todo

import com.galaxyoffline.todo.data.Task
import com.galaxyoffline.todo.data.TaskFilter
import com.galaxyoffline.todo.data.TaskLogic
import com.galaxyoffline.todo.data.TaskSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskLogicTest {
    private val today = LocalDate.of(2026, 5, 1)
    private fun t(id: Long, due: Long?, done: Boolean = false, prio: Int = 1, list: Long = 1) =
        Task(id = id, listId = list, title = "g$id", due = due?.let { today.toEpochDay() + it }, priority = prio, done = done, createdAt = id)

    private val tasks = listOf(t(1, -2), t(2, 0, prio = 2), t(3, 3), t(4, 10), t(5, null), t(6, 0, done = true), t(7, 1, list = 2))

    @Test fun filters() {
        assertEquals(listOf(1L, 2L), TaskLogic.filter(tasks, TaskFilter.TODAY, null, today, TaskSort.DUE).map { it.id })
        assertEquals(listOf(7L, 3L), TaskLogic.filter(tasks, TaskFilter.UPCOMING, null, today, TaskSort.DUE).map { it.id })
        assertEquals(6, TaskLogic.filter(tasks, TaskFilter.ALL, null, today, TaskSort.DUE).size)
        assertEquals(listOf(6L), TaskLogic.filter(tasks, TaskFilter.DONE, null, today, TaskSort.DUE).map { it.id })
        assertEquals(listOf(7L), TaskLogic.filter(tasks, TaskFilter.ALL, 2, today, TaskSort.DUE).map { it.id })
    }

    @Test fun sortingAndOverdue() {
        assertEquals(2L, TaskLogic.filter(tasks, TaskFilter.ALL, null, today, TaskSort.PRIORITY).first().id)
        assertEquals(5L, TaskLogic.filter(tasks, TaskFilter.ALL, null, today, TaskSort.DUE).last().id)
        assertTrue(TaskLogic.isOverdue(tasks[0], today))
    }
}
