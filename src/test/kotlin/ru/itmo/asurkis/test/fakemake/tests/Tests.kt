package ru.itmo.asurkis.test.fakemake.tests

import org.junit.jupiter.api.assertThrows
import ru.itmo.asurkis.test.fakemake.Section
import ru.itmo.asurkis.test.fakemake.SectionLoopException
import java.io.FileNotFoundException

import kotlin.test.Test
import kotlin.test.assertEquals

class DoubleRunException(val name: String) : Exception() {
    override val message: String
        get() = "Section $name had been run twice"
}

class SingleRunSection(
    val name: String,
    val allowedRuns: Int,
    dependencies: List<String> = listOf(),
    targetFileName: String? = null,
    runCommand: String? = null
) : Section(dependencies, targetFileName, runCommand) {
    var nRuns = 0

    override fun makeRun() {
        if (++nRuns > allowedRuns) {
            throw DoubleRunException(name)
        }
    }
}

class Tests {
    @Test
    fun singleRun() {
        val one = SingleRunSection("one", 1)
        val map = mapOf(Pair("one", one))
        one.satisfy(map)
        assertEquals(1, one.nRuns)
    }

    @Test
    fun reuseRuns() {
        val one = SingleRunSection("one", 1)
        val map = mapOf(Pair("one", one))
        one.satisfy(map)
        one.satisfy(map)
        assertEquals(1, one.nRuns)
    }

    @Test
    fun satisfyDependencies() {
        val one = SingleRunSection("one", 1)
        val two = SingleRunSection("two", 2, listOf("one"))
        val map = mapOf(Pair("one", one), Pair("two", two))
        two.satisfy(map)
        assertEquals(1, one.nRuns)
        assertEquals(1, two.nRuns)
    }

    @Test
    fun reuseDependencies() {
        val one = SingleRunSection("one", 1)
        val two = SingleRunSection("two", 1, listOf("one"))
        val map = mapOf(Pair("one", one), Pair("two", two))
        one.satisfy(map)
        two.satisfy(map)
        assertEquals(1, one.nRuns)
        assertEquals(1, two.nRuns)
    }

    @Test
    fun reuseDependencies2() {
        val one = SingleRunSection("one", 1, listOf("three"))
        val two = SingleRunSection("two", 1, listOf("three"))
        val three = SingleRunSection("three", 1)
        val map = mapOf(Pair("one", one), Pair("two", two), Pair("three", three))
        one.satisfy(map)
        two.satisfy(map)
        assertEquals(1, one.nRuns)
        assertEquals(1, two.nRuns)
        assertEquals(1, three.nRuns)
    }

    @Test
    fun leaveIndependent() {
        val one = SingleRunSection("one", 1)
        val two = SingleRunSection("two", 0, listOf("one"))
        val map = mapOf(Pair("one", one), Pair("two", two))
        one.satisfy(map)
        assertEquals(1, one.nRuns)
    }

    @Test
    fun leaveIndependent2() {
        val one = SingleRunSection("one", 1)
        val two = SingleRunSection("two", 1, listOf("one"))
        val three = SingleRunSection("three", 0, listOf("one"))
        val map = mapOf(Pair("one", one), Pair("two", two), Pair("three", three))
        two.satisfy(map)
        assertEquals(1, one.nRuns)
        assertEquals(1, two.nRuns)
    }

    @Test
    fun throwNotFound() {
        /* File that would not be present */
        val one = SingleRunSection("one", 0, listOf("qwerasdfzxcv"))
        val map = mapOf(Pair("one", one))
        assertThrows<FileNotFoundException> { one.satisfy(map) }
        assertEquals(0, one.nRuns)
    }

    @Test
    fun throwLoop() {
        val one = SingleRunSection("one", 0, listOf("two"))
        val two = SingleRunSection("two", 0, listOf("one"))
        val map = mapOf(Pair("one", one), Pair("two", two))
        assertThrows<SectionLoopException> { one.satisfy(map) }
    }

    @Test
    fun loopExceptionMessage() {
        val one = SingleRunSection("one", 0, listOf("two"))
        val two = SingleRunSection("two", 0, listOf("one"))
        val three = SingleRunSection("three", 0, listOf("one"))
        val map = mapOf(Pair("one", one), Pair("two", two), Pair("three", three))
        assertThrows<SectionLoopException>("Loop of sections detected: one -> two -> one") {
            three.satisfy(map)
        }
    }
}
